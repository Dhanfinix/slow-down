package com.slowdown.throttle.core

/** Non-blocking view of the real-world connection a [TcpSession] relays to. */
interface RemoteConnection {
    /** Returns bytes read, 0 if nothing is available, -1 at end of stream. May throw IOException. */
    fun read(dst: ByteArray, off: Int, len: Int): Int

    /** Returns bytes written (0 if the socket buffer is full). May throw IOException. */
    fun write(src: ByteArray, off: Int, len: Int): Int
    fun shutdownOutput()
    fun close()
}

/** Where packets for the client app go (the TUN device). */
fun interface PacketSink {
    fun send(packet: ByteArray)
}

/**
 * One TCP connection between an app on the phone (the "client", seen through the TUN) and a real server.
 *
 * The session plays the server to the client. The TUN is lossless and in order, so there is no retransmit queue:
 * data read from [remote] is sent once and bounded by the client's receive window.
 *
 * Throttling: download bytes are only read from [remote] when [down] has tokens, so the real server is slowed by
 * ordinary TCP flow control. Upload bytes sit in a bounded buffer that drains into [remote] at the [up] rate. The
 * advertised window shrinks while the buffer is full, which slows the client.
 *
 * Not thread safe. Call from one thread only.
 */
class TcpSession(
    val clientIp: Int, val clientPort: Int, val serverIp: Int, val serverPort: Int,
    private val remote: RemoteConnection,
    private val sink: PacketSink,
    private val down: TokenBucket,
    private val up: TokenBucket,
    private val nowMs: () -> Long,
    private val iss: Int,
    private val onDownBytes: (Int) -> Unit = {},
    private val onUpBytes: (Int) -> Unit = {},
) {
    enum class State { WAIT_REMOTE, SYN_ACK_SENT, ESTABLISHED, CLOSED }

    var state = State.WAIT_REMOTE; private set
    val isClosed get() = state == State.CLOSED
    val isWaitingForRemote get() = state == State.WAIT_REMOTE

    /** True when the engine should watch the remote socket for readability. */
    var wantsRead = false; private set
    /** True when the engine should watch the remote socket for writability. */
    var wantsWrite = false; private set
    /** True when the session is blocked on tokens and must be pumped again soon. */
    var needsTimer = false; private set
    var lastActivityMs = nowMs(); private set
    val createdMs = lastActivityMs

    private var rcvNxt = 0
    private var sndNxt = iss
    private var sndUna = iss
    private var sndWnd = 65535
    private var segSize = DEFAULT_MSS
    private var lastAdvWin = 0

    private val pending = ByteArray(RECV_CAP)
    private var pStart = 0
    private var pLen = 0

    private var clientFin = false
    private var remoteEof = false
    private var finSent = false
    private var finAcked = false
    private var outShutdown = false
    private var readBuf: ByteArray? = null

    private fun free() = RECV_CAP - pLen
    private fun touch() { lastActivityMs = nowMs() }

    /** First packet of the connection. */
    fun onSyn(seg: TcpSegment) {
        rcvNxt = seg.seq + 1
        sndWnd = seg.window
        segSize = if (seg.mss in 1..DEFAULT_MSS) seg.mss else if (seg.mss > DEFAULT_MSS) DEFAULT_MSS else 536
        touch()
    }

    fun onRemoteConnected() {
        if (state != State.WAIT_REMOTE) return
        sndUna = iss
        sndNxt = iss + 1
        state = State.SYN_ACK_SENT
        emit(TcpFlags.SYN or TcpFlags.ACK, iss, null, 0, 0, DEFAULT_MSS)
        update()
    }

    fun onRemoteConnectFailed() {
        if (state == State.CLOSED) return
        emitRaw(TcpFlags.RST or TcpFlags.ACK, 0, rcvNxt)
        closeNow()
    }

    fun onClientPacket(seg: TcpSegment) {
        if (state == State.CLOSED) return
        touch()
        if (seg.rst) { closeNow(); return }
        when (state) {
            State.WAIT_REMOTE -> return
            State.SYN_ACK_SENT -> {
                if (seg.syn) { emit(TcpFlags.SYN or TcpFlags.ACK, iss, null, 0, 0, DEFAULT_MSS); return }
                if (!seg.hasAck) return
                if (seg.ack == iss + 1) { state = State.ESTABLISHED; sndUna = seg.ack } else { abort(); return }
            }
            else -> {}
        }
        if (seg.syn) return
        if (seg.hasAck) {
            val d = seg.ack - sndUna
            if (d > 0 && (seg.ack - sndNxt) <= 0) {
                sndUna = seg.ack
                if (finSent && sndUna == sndNxt) finAcked = true
            }
            sndWnd = seg.window
        }
        processData(seg)
        pump()
    }

    private fun processData(seg: TcpSegment) {
        if (seg.payloadLen == 0 && !seg.fin) return
        var off = seg.payloadOff
        var len = seg.payloadLen
        var seq = seg.seq
        val overlap = rcvNxt - seq
        if (overlap > 0) {
            val trim = minOf(overlap, len)
            off += trim; len -= trim; seq += trim
        }
        if (seq == rcvNxt) {
            val accept = minOf(len, free())
            if (accept > 0) {
                if (pStart + pLen + accept > RECV_CAP) {
                    System.arraycopy(pending, pStart, pending, 0, pLen)
                    pStart = 0
                }
                System.arraycopy(seg.buf, off, pending, pStart + pLen, accept)
                pLen += accept
                rcvNxt += accept
            }
            if (accept == len && seg.fin && !clientFin) {
                clientFin = true
                rcvNxt += 1
            }
        }
        emit(TcpFlags.ACK, sndNxt, null, 0, 0)
    }

    /** Moves data in both directions as far as tokens, windows and socket buffers allow. */
    fun pump() {
        if (state == State.CLOSED) return
        if (state == State.ESTABLISHED) {
            try {
                pumpToRemote()
                pumpFromRemote()
            } catch (e: java.io.IOException) {
                abort()
                return
            }
            if (finAcked && clientFin) closeNow()
        }
        update()
    }

    private fun pumpToRemote() {
        val before = free()
        while (pLen > 0) {
            val want = minOf(pLen, 16384)
            val n = up.takeUpTo(want.toLong()).toInt()
            if (n == 0) break
            val wrote = remote.write(pending, pStart, n)
            if (wrote < n) up.give((n - wrote).toLong())
            if (wrote <= 0) break
            pStart += wrote; pLen -= wrote
            onUpBytes(wrote)
            touch()
        }
        if (pLen == 0) pStart = 0
        if (pLen == 0 && clientFin && !outShutdown) {
            outShutdown = true
            remote.shutdownOutput()
        }
        val now = free()
        if (now > before && (lastAdvWin < segSize || now - lastAdvWin >= RECV_CAP / 2)) {
            emit(TcpFlags.ACK, sndNxt, null, 0, 0)
        }
    }

    private fun pumpFromRemote() {
        if (remoteEof) return
        val buf = readBuf ?: ByteArray(DEFAULT_MSS).also { readBuf = it }
        while (true) {
            val inflight = sndNxt - sndUna
            val room = minOf(sndWnd, 65535) - inflight
            if (room <= 0) break
            val chunk = minOf(segSize, room)
            if (chunk < segSize && inflight > 0) break
            if (!down.tryTake(chunk.toLong())) break
            val n = remote.read(buf, 0, chunk)
            if (n < chunk) down.give((chunk - maxOf(n, 0)).toLong())
            if (n == 0) break
            if (n < 0) {
                remoteEof = true
                finSent = true
                emit(TcpFlags.FIN or TcpFlags.ACK, sndNxt, null, 0, 0)
                sndNxt += 1
                break
            }
            emit(TcpFlags.ACK or TcpFlags.PSH, sndNxt, buf, 0, n)
            sndNxt += n
            onDownBytes(n)
            touch()
        }
    }

    /** Recomputes what the engine should wait for. */
    private fun update() {
        wantsRead = false; wantsWrite = false; needsTimer = false
        if (state != State.ESTABLISHED) return
        if (!remoteEof) {
            val room = minOf(sndWnd, 65535) - (sndNxt - sndUna)
            val inflight = sndNxt - sndUna
            val windowOk = room > 0 && (room >= segSize || inflight == 0)
            if (windowOk) {
                if (down.available() >= minOf(segSize, room)) wantsRead = true else needsTimer = true
            }
        }
        if (pLen > 0) {
            if (up.available() > 0) wantsWrite = true else needsTimer = true
        }
    }

    private fun emit(flags: Int, seq: Int, payload: ByteArray?, off: Int, len: Int, mss: Int = 0) {
        val win = free()
        lastAdvWin = win
        sink.send(PacketBuilder.tcp(serverIp, clientIp, serverPort, clientPort, seq, rcvNxt, flags, win, payload, off, len, mss))
    }

    private fun emitRaw(flags: Int, seq: Int, ack: Int) {
        sink.send(PacketBuilder.tcp(serverIp, clientIp, serverPort, clientPort, seq, ack, flags, 0))
    }

    /** Resets the client and tears down. */
    fun abort() {
        if (state == State.CLOSED) return
        if (state != State.WAIT_REMOTE) emitRaw(TcpFlags.RST or TcpFlags.ACK, sndNxt, rcvNxt)
        else emitRaw(TcpFlags.RST or TcpFlags.ACK, 0, rcvNxt)
        closeNow()
    }

    fun closeNow() {
        if (state == State.CLOSED) return
        state = State.CLOSED
        wantsRead = false; wantsWrite = false; needsTimer = false
        readBuf = null
        try { remote.close() } catch (_: Exception) {}
    }

    companion object {
        const val DEFAULT_MSS = 1360
        const val RECV_CAP = 65535
    }
}

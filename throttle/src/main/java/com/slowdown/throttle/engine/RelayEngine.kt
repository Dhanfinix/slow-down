package com.slowdown.throttle.engine

import android.util.Log
import com.slowdown.throttle.core.Ipv4
import com.slowdown.throttle.core.PacketBuilder
import com.slowdown.throttle.core.Proto
import com.slowdown.throttle.core.TcpFlags
import com.slowdown.throttle.core.TcpSegment
import com.slowdown.throttle.core.TcpSession
import com.slowdown.throttle.core.TokenBucket
import com.slowdown.throttle.core.UdpDatagram
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.PriorityQueue
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicLong

/** Lets the engine exempt its own sockets from the VPN (VpnService.protect). */
interface SocketProtector {
    fun protect(socket: Socket): Boolean
    fun protect(socket: DatagramSocket): Boolean
}

/**
 * Userspace packet relay. One TUN reader thread feeds a queue, one engine thread owns the selector and all
 * sessions. Every TCP and UDP flow gets its own protected socket.
 */
class RelayEngine(
    private val tunIn: InputStream,
    private val tunOut: OutputStream,
    private val protector: SocketProtector,
    config: ThrottleConfigLite,
    private val onStats: (bytesDown: Long, bytesUp: Long) -> Unit,
    private val onStopped: (Throwable?) -> Unit,
) {
    data class ThrottleConfigLite(val downKbps: Int, val upKbps: Int, val extraLatencyMs: Int)

    private val down = TokenBucket(TokenBucket.bytesPerSec(config.downKbps))
    private val up = TokenBucket(TokenBucket.bytesPerSec(config.upKbps))
    @Volatile private var latencyNanos = config.extraLatencyMs * 1_000_000L
    val bytesDown = AtomicLong()
    val bytesUp = AtomicLong()

    private val selector: Selector = Selector.open()
    private val inbound = ArrayBlockingQueue<ByteArray>(4096)
    @Volatile private var running = false
    private var reader: Thread? = null
    private var worker: Thread? = null

    private class TcpEntry(val session: TcpSession, val channel: SocketChannel) { var key: SelectionKey? = null }
    private class UdpFlow(
        val channel: DatagramChannel, val clientIp: Int, val clientPort: Int,
        val serverIp: Int, val serverPort: Int, var lastMs: Long,
    ) { var key: SelectionKey? = null }

    private val tcp = HashMap<Long, TcpEntry>()
    private val udp = HashMap<Long, UdpFlow>()
    private val pumpSet = LinkedHashSet<TcpEntry>()
    private val timerSet = LinkedHashSet<TcpEntry>()
    private class Delayed(val due: Long, val seq: Long, val task: () -> Unit)
    private val delayQ = PriorityQueue<Delayed>(compareBy<Delayed>({ it.due }, { it.seq }))
    private var delaySeq = 0L
    private var issCounter = (System.nanoTime() and 0x7fffffff).toInt()

    fun updateSpeed(downKbps: Int, upKbps: Int) {
        down.setRate(TokenBucket.bytesPerSec(downKbps))
        up.setRate(TokenBucket.bytesPerSec(upKbps))
    }

    fun start() {
        running = true
        reader = Thread({ readTun() }, "slowdown-tun-reader").also { it.isDaemon = true; it.start() }
        worker = Thread({ loop() }, "slowdown-engine").also { it.isDaemon = true; it.start() }
    }

    fun stop() {
        running = false
        try { selector.wakeup() } catch (_: Exception) {}
        reader?.interrupt()
    }

    private fun readTun() {
        val buf = ByteArray(32768)
        try {
            while (running) {
                val n = tunIn.read(buf)
                if (n < 0) break
                if (n == 0) continue
                inbound.put(buf.copyOf(n))
                selector.wakeup()
            }
        } catch (e: Exception) {
            if (running) Log.w(TAG, "tun read ended: $e")
        }
        if (running) { running = false; selector.wakeup() }
    }

    private fun loop() {
        var error: Throwable? = null
        var nextStats = now() + 1000
        var nextSweep = now() + 1000
        try {
            while (running) {
                var timeout = 1000L
                if (timerSet.isNotEmpty()) timeout = 5
                delayQ.peek()?.let { timeout = minOf(timeout, maxOf(1, (it.due - System.nanoTime()) / 1_000_000)) }
                selector.select(timeout)
                drainInbound()
                val keys = selector.selectedKeys()
                for (key in keys) if (key.isValid) onKey(key)
                keys.clear()
                if (timerSet.isNotEmpty()) pumpSet.addAll(timerSet)
                pumpAll()
                runDelayed()
                val t = now()
                if (t >= nextSweep) { sweep(t); nextSweep = t + 1000 }
                if (t >= nextStats) { onStats(bytesDown.get(), bytesUp.get()); nextStats = t + 1000 }
            }
        } catch (e: Throwable) {
            error = e
            Log.e(TAG, "engine crashed", e)
        } finally {
            running = false
            tcp.values.forEach { it.session.closeNow() }
            udp.values.forEach { try { it.channel.close() } catch (_: Exception) {} }
            tcp.clear(); udp.clear()
            try { selector.close() } catch (_: Exception) {}
            onStopped(error)
        }
    }

    private fun now() = System.currentTimeMillis()

    private fun schedule(delayNanos: Long, task: () -> Unit) {
        if (delayNanos <= 0) task() else delayQ.add(Delayed(System.nanoTime() + delayNanos, delaySeq++, task))
    }

    private fun runDelayed() {
        val t = System.nanoTime()
        while (true) {
            val d = delayQ.peek() ?: break
            if (d.due > t) break
            delayQ.poll()
            d.task()
        }
    }

    private fun drainInbound() {
        val half = latencyNanos / 2
        while (true) {
            val p = inbound.poll() ?: break
            if (half <= 0) handlePacket(p) else schedule(half) { handlePacket(p) }
        }
    }

    private fun toTun(p: ByteArray) {
        val half = latencyNanos / 2
        schedule(half) {
            try { tunOut.write(p) } catch (e: IOException) { running = false }
        }
    }

    private fun handlePacket(p: ByteArray) {
        when (p[0].toInt() ushr 4) {
            4 -> {
                val ip = Ipv4.parse(p, p.size) ?: return
                if (ip.isFragment) return
                when (ip.protocol) {
                    Proto.TCP -> handleTcp(ip)
                    Proto.UDP -> handleUdp(ip)
                }
            }
            6 -> PacketBuilder.icmpv6Unreachable(p, p.size)?.let { toTun(it) }
        }
    }

    private fun key(clientPort: Int, serverIp: Int, serverPort: Int): Long =
        (clientPort.toLong() shl 48) or ((serverIp.toLong() and 0xffffffffL) shl 16) or serverPort.toLong()

    // ---- TCP ----

    private fun handleTcp(ip: Ipv4) {
        val seg = TcpSegment.parse(ip) ?: return
        val k = key(seg.srcPort, ip.dstIp, seg.dstPort)
        val entry = tcp[k]
        if (entry != null) {
            entry.session.onClientPacket(seg)
            pumpSet.add(entry)
            return
        }
        if (seg.syn && !seg.hasAck) { newTcp(ip, seg, k); return }
        if (seg.rst) return
        // Unknown connection: reset it.
        val rst = if (seg.hasAck) PacketBuilder.tcp(ip.dstIp, ip.srcIp, seg.dstPort, seg.srcPort, seg.ack, 0, TcpFlags.RST, 0)
        else PacketBuilder.tcp(
            ip.dstIp, ip.srcIp, seg.dstPort, seg.srcPort, 0,
            seg.seq + seg.payloadLen + (if (seg.fin) 1 else 0), TcpFlags.RST or TcpFlags.ACK, 0,
        )
        toTun(rst)
    }

    private fun newTcp(ip: Ipv4, seg: TcpSegment, k: Long) {
        fun refuse() = toTun(PacketBuilder.tcp(ip.dstIp, ip.srcIp, seg.dstPort, seg.srcPort, 0, seg.seq + 1, TcpFlags.RST or TcpFlags.ACK, 0))
        if (tcp.size >= MAX_TCP) { refuse(); return }
        val ch = try {
            SocketChannel.open().also {
                it.configureBlocking(false)
                if (!protector.protect(it.socket())) { it.close(); throw IOException("protect failed") }
            }
        } catch (e: IOException) { refuse(); return }
        val session = TcpSession(
            ip.srcIp, seg.srcPort, ip.dstIp, seg.dstPort, NioRemote(ch),
            { toTun(it) }, down, up, ::now, issCounter.also { issCounter += 64000 },
            { bytesDown.addAndGet(it.toLong()) }, { bytesUp.addAndGet(it.toLong()) },
        )
        session.onSyn(seg)
        val entry = TcpEntry(session, ch)
        tcp[k] = entry
        try {
            val addr = InetSocketAddress(InetAddress.getByAddress(ip.dstBytes()), seg.dstPort)
            if (ch.connect(addr)) {
                session.onRemoteConnected()
                pumpSet.add(entry)
            } else {
                entry.key = ch.register(selector, SelectionKey.OP_CONNECT, entry)
            }
        } catch (e: Exception) {
            session.onRemoteConnectFailed()
            finish(k, entry)
        }
    }

    private fun entryKey(e: TcpEntry) = key(e.session.clientPort, e.session.serverIp, e.session.serverPort)

    private fun finish(k: Long, e: TcpEntry) {
        tcp.remove(k)
        pumpSet.remove(e); timerSet.remove(e)
        e.key?.cancel()
    }

    private fun onKey(key: SelectionKey) {
        when (val att = key.attachment()) {
            is TcpEntry -> {
                if (key.isConnectable) {
                    try {
                        if (att.channel.finishConnect()) att.session.onRemoteConnected()
                    } catch (e: IOException) {
                        att.session.onRemoteConnectFailed()
                    }
                }
                pumpSet.add(att)
            }
            is UdpFlow -> if (key.isReadable) readUdp(att)
        }
    }

    private fun pumpAll() {
        if (pumpSet.isEmpty()) return
        val batch = pumpSet.toList()
        pumpSet.clear()
        for (e in batch) {
            e.session.pump()
            if (e.session.isClosed) { finish(entryKey(e), e); continue }
            if (e.session.isWaitingForRemote) continue
            if (e.session.needsTimer) timerSet.add(e) else timerSet.remove(e)
            var ops = 0
            if (e.session.wantsRead) ops = ops or SelectionKey.OP_READ
            if (e.session.wantsWrite) ops = ops or SelectionKey.OP_WRITE
            try {
                val k = e.key
                if (k == null) e.key = e.channel.register(selector, ops, e) else if (k.isValid) k.interestOps(ops)
            } catch (ex: Exception) {
                e.session.abort(); finish(entryKey(e), e)
            }
        }
    }

    // ---- UDP ----

    private fun handleUdp(ip: Ipv4) {
        val d = UdpDatagram.parse(ip) ?: return
        val k = key(d.srcPort, ip.dstIp, d.dstPort)
        var flow = udp[k]
        if (flow == null) {
            if (udp.size >= MAX_UDP) return
            try {
                val ch = DatagramChannel.open()
                ch.configureBlocking(false)
                if (!protector.protect(ch.socket())) { ch.close(); return }
                ch.connect(InetSocketAddress(InetAddress.getByAddress(ip.dstBytes()), d.dstPort))
                flow = UdpFlow(ch, ip.srcIp, d.srcPort, ip.dstIp, d.dstPort, now())
                flow.key = ch.register(selector, SelectionKey.OP_READ, flow)
                udp[k] = flow
            } catch (e: Exception) { return }
        }
        flow.lastMs = now()
        if (d.dstPort != DNS_PORT && !up.tryTake(d.payloadLen.toLong())) return // over the limit: drop, like a full queue
        try {
            flow.channel.write(ByteBuffer.wrap(d.buf, d.payloadOff, d.payloadLen))
            bytesUp.addAndGet(d.payloadLen.toLong())
        } catch (e: IOException) { closeUdp(k, flow) }
    }

    private val udpBuf = ByteBuffer.allocate(65535)

    private fun readUdp(f: UdpFlow) {
        val k = key(f.clientPort, f.serverIp, f.serverPort)
        try {
            while (true) {
                udpBuf.clear()
                val n = f.channel.read(udpBuf)
                if (n <= 0) break
                f.lastMs = now()
                if (f.serverPort != DNS_PORT && !down.tryTake(n.toLong())) continue
                bytesDown.addAndGet(n.toLong())
                toTun(PacketBuilder.udp(f.serverIp, f.clientIp, f.serverPort, f.clientPort, udpBuf.array(), 0, n))
            }
        } catch (e: IOException) { closeUdp(k, f) }
    }

    private fun closeUdp(k: Long, f: UdpFlow) {
        udp.remove(k)
        f.key?.cancel()
        try { f.channel.close() } catch (_: Exception) {}
    }

    private fun sweep(t: Long) {
        for ((k, e) in tcp.toList()) {
            val s = e.session
            val limit = if (s.isWaitingForRemote) CONNECT_TIMEOUT_MS else TCP_IDLE_MS
            if (t - s.lastActivityMs > limit) { s.abort(); finish(k, e) }
        }
        for ((k, f) in udp.toList()) {
            val limit = if (f.serverPort == DNS_PORT) 15_000 else UDP_IDLE_MS
            if (t - f.lastMs > limit) closeUdp(k, f)
        }
    }

    companion object {
        private const val TAG = "SlowDownEngine"
        private const val DNS_PORT = 53
        private const val MAX_TCP = 1024
        private const val MAX_UDP = 512
        private const val CONNECT_TIMEOUT_MS = 20_000L
        private const val TCP_IDLE_MS = 300_000L
        private const val UDP_IDLE_MS = 60_000L
    }
}

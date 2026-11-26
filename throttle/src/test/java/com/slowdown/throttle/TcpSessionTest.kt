package com.slowdown.throttle

import com.slowdown.throttle.core.Ipv4
import com.slowdown.throttle.core.PacketBuilder
import com.slowdown.throttle.core.PacketSink
import com.slowdown.throttle.core.RemoteConnection
import com.slowdown.throttle.core.TcpFlags
import com.slowdown.throttle.core.TcpSegment
import com.slowdown.throttle.core.TcpSession
import com.slowdown.throttle.core.TokenBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Drives a TcpSession like a client TCP stack would, against a fake network. */
class TcpSessionTest {
    private val cIp = 0x0a6fde01
    private val sIp = 0x5db8d822
    private val cPort = 40000
    private val sPort = 443

    private class FakeRemote : RemoteConnection {
        val toClient = ArrayDeque<Byte>()
        var eof = false
        val received = ByteArrayOutputStream()
        var outShut = false
        var closed = false
        var failRead = false
        override fun read(dst: ByteArray, off: Int, len: Int): Int {
            if (failRead) throw IOException("boom")
            if (toClient.isEmpty()) return if (eof) -1 else 0
            val n = minOf(len, toClient.size)
            for (i in 0 until n) dst[off + i] = toClient.removeFirst()
            return n
        }
        override fun write(src: ByteArray, off: Int, len: Int): Int { received.write(src, off, len); return len }
        override fun shutdownOutput() { outShut = true }
        override fun close() { closed = true }
    }

    private class Env(rateKbps: Int = 100_000) {
        var nowNs = 0L
        val remote = FakeRemote()
        val out = ArrayList<TcpSegment>()
        val outPayload = ByteArrayOutputStream()
        val down = TokenBucket(TokenBucket.bytesPerSec(rateKbps), nanoTime = { nowNs })
        val up = TokenBucket(TokenBucket.bytesPerSec(rateKbps), nanoTime = { nowNs })
        val sink = PacketSink { p ->
            val seg = TcpSegment.parse(Ipv4.parse(p, p.size)!!)!!
            out.add(seg)
            outPayload.write(p, seg.payloadOff, seg.payloadLen)
        }
        var downBytes = 0L
        val s = TcpSession(0x0a6fde01, 40000, 0x5db8d822, 443, remote, sink, down, up, { nowNs / 1_000_000 }, 1000, { downBytes += it })
        var cSeq = 5000
        var cAck = 0 // next server seq we expect

        fun client(flags: Int, payload: ByteArray = ByteArray(0), window: Int = 65535, mss: Int = 0) {
            val p = PacketBuilder.tcp(0x0a6fde01, 0x5db8d822, 40000, 443, cSeq, cAck, flags, window, payload, 0, payload.size, mss)
            val seg = TcpSegment.parse(Ipv4.parse(p, p.size)!!)!!
            if (flags and TcpFlags.SYN != 0) s.onSyn(seg)
            s.onClientPacket(seg)
            cSeq += payload.size + (if (flags and TcpFlags.SYN != 0) 1 else 0) + (if (flags and TcpFlags.FIN != 0) 1 else 0)
        }
    }

    @Test
    fun handshakeSendsSynAckWithMssAndAckNumber() {
        val e = Env()
        e.client(TcpFlags.SYN, mss = 1460)
        assertTrue(e.out.isEmpty()) // nothing until the real connection is up
        e.s.onRemoteConnected()
        val sa = e.out.single()
        assertEquals(TcpFlags.SYN or TcpFlags.ACK, sa.flags)
        assertEquals(1000, sa.seq)
        assertEquals(5001, sa.ack)
        assertEquals(1360, sa.mss)
        e.cAck = 1001
        e.client(TcpFlags.ACK)
        assertEquals(TcpSession.State.ESTABLISHED, e.s.state)
    }

    @Test
    fun uploadDataGoesToRemoteAndIsAcked() {
        val e = Env()
        e.client(TcpFlags.SYN, mss = 1460); e.s.onRemoteConnected(); e.cAck = 1001; e.client(TcpFlags.ACK)
        val data = ByteArray(3000) { (it % 251).toByte() }
        e.client(TcpFlags.ACK or TcpFlags.PSH, data.copyOfRange(0, 1500))
        e.client(TcpFlags.ACK or TcpFlags.PSH, data.copyOfRange(1500, 3000))
        assertEquals(data.toList(), e.remote.received.toByteArray().toList())
        assertEquals(5001 + 3000, e.out.last().ack)
    }

    @Test
    fun duplicateAndOverlappingDataIsNotWrittenTwice() {
        val e = Env()
        e.client(TcpFlags.SYN, mss = 1460); e.s.onRemoteConnected(); e.cAck = 1001; e.client(TcpFlags.ACK)
        val d = ByteArray(100) { it.toByte() }
        e.client(TcpFlags.ACK, d)
        e.cSeq -= 100
        e.client(TcpFlags.ACK, d) // retransmission
        e.cSeq -= 50
        e.client(TcpFlags.ACK, ByteArray(100) { (it + 50).toByte() }) // 50 old + 50 new
        assertEquals(150, e.remote.received.size())
        assertEquals(5001 + 150, e.out.last().ack)
    }

    @Test
    fun downloadDataIsSegmentedAndRespectsClientWindow() {
        val e = Env()
        e.client(TcpFlags.SYN, mss = 1460); e.s.onRemoteConnected(); e.cAck = 1001; e.client(TcpFlags.ACK, window = 3000)
        repeat(10000) { e.remote.toClient.addLast((it % 200).toByte()) }
        e.out.clear()
        e.s.pump()
        // Window of 3000 allows two full segments, a third would exceed it.
        assertEquals(2, e.out.count { it.payloadLen > 0 })
        assertTrue(e.out.all { it.payloadLen <= 1360 })
        // Client acks, opening the window, more flows.
        e.cAck = 1001 + 2720
        e.client(TcpFlags.ACK, window = 65535)
        val total = e.outPayload.size()
        assertEquals(10000, total)
        assertEquals((0 until 10000).map { (it % 200).toByte() }, e.outPayload.toByteArray().toList())
        // sequence numbers are contiguous
        val data = e.out.filter { it.payloadLen > 0 }
        var next = 1001
        for (seg in data) { assertEquals(next, seg.seq); next += seg.payloadLen }
    }

    @Test
    fun downloadRateIsLimitedToTarget() {
        val targetKbps = 800 // 100 kB/s
        val e = Env(targetKbps)
        e.client(TcpFlags.SYN, mss = 1460); e.s.onRemoteConnected(); e.cAck = 1001; e.client(TcpFlags.ACK)
        // endless server data; client acks everything instantly
        var served = 0
        val chunk = ByteArray(65536)
        var sent = 0L
        e.downBytes = 0
        while (e.nowNs < 12_000_000_000L) {
            if (e.remote.toClient.size < 100_000) { for (b in chunk) e.remote.toClient.addLast(b); served += chunk.size }
            e.s.pump()
            sent = e.out.filter { it.payloadLen > 0 }.sumOf { it.payloadLen.toLong() }
            e.cAck = 1001 + sent.toInt()
            e.client(TcpFlags.ACK)
            e.nowNs += 1_000_000
        }
        val seconds = 12.0
        val avg = e.downBytes / seconds
        val target = targetKbps * 1000 / 8.0
        assertTrue("avg=$avg target=$target", avg in target * 0.8..target * 1.2)
        // the whole stream stayed in order and unmodified
        assertEquals(e.downBytes, e.outPayload.size().toLong())
    }

    @Test
    fun uploadRateIsLimitedAndWindowShrinks() {
        val e = Env(80) // 10 kB/s, burst 3000
        e.client(TcpFlags.SYN, mss = 1460); e.s.onRemoteConnected(); e.cAck = 1001; e.client(TcpFlags.ACK)
        val seg = ByteArray(1360)
        // Client blasts 60 kB in one go: the buffer accepts it all (64 KB), but the remote only gets the burst.
        repeat(44) { e.client(TcpFlags.ACK, seg) }
        assertTrue(e.remote.received.size() <= 3000)
        assertTrue(e.out.last().window < 65535)
        // After 5 s about 50 kB more... limited by rate: 10 kB/s * 5 s = 50000
        repeat(5000) { e.nowNs += 1_000_000; e.s.pump() }
        val got = e.remote.received.size()
        assertTrue("got=$got", got in 45_000..60_000)
    }

    @Test
    fun serverClosesFirstThenClient() {
        val e = Env()
        e.client(TcpFlags.SYN, mss = 1460); e.s.onRemoteConnected(); e.cAck = 1001; e.client(TcpFlags.ACK)
        e.remote.toClient.addAll("bye".toByteArray().toList())
        e.remote.eof = true
        e.out.clear()
        e.s.pump()
        assertEquals("bye", String(e.outPayload.toByteArray()))
        val fin = e.out.last()
        assertTrue(fin.fin)
        assertEquals(1001 + 3, fin.seq)
        e.cAck = fin.seq + 1
        e.client(TcpFlags.ACK) // acks data + FIN
        assertFalse(e.s.isClosed)
        e.client(TcpFlags.FIN or TcpFlags.ACK)
        assertEquals(5001 + 1, e.out.last().ack) // FIN is acked
        assertTrue(e.s.isClosed)
        assertTrue(e.remote.closed)
    }

    @Test
    fun clientClosesFirstHalfClosesRemote() {
        val e = Env()
        e.client(TcpFlags.SYN, mss = 1460); e.s.onRemoteConnected(); e.cAck = 1001; e.client(TcpFlags.ACK)
        e.client(TcpFlags.FIN or TcpFlags.ACK, "req".toByteArray())
        assertEquals("req", String(e.remote.received.toByteArray()))
        assertTrue(e.remote.outShut)
        assertEquals(5001 + 3 + 1, e.out.last().ack)
        // server replies and closes
        e.remote.toClient.addAll("resp".toByteArray().toList()); e.remote.eof = true
        e.s.pump()
        assertEquals("resp", String(e.outPayload.toByteArray()))
        val fin = e.out.last()
        assertTrue(fin.fin)
        e.cAck = fin.seq + 1
        e.client(TcpFlags.ACK)
        assertTrue(e.s.isClosed)
    }

    @Test
    fun connectFailureSendsRst() {
        val e = Env()
        e.client(TcpFlags.SYN, mss = 1460)
        e.s.onRemoteConnectFailed()
        val r = e.out.single()
        assertTrue(r.rst)
        assertEquals(5001, r.ack)
        assertTrue(e.s.isClosed)
    }

    @Test
    fun clientRstClosesRemote() {
        val e = Env()
        e.client(TcpFlags.SYN, mss = 1460); e.s.onRemoteConnected(); e.cAck = 1001; e.client(TcpFlags.ACK)
        e.client(TcpFlags.RST)
        assertTrue(e.s.isClosed); assertTrue(e.remote.closed)
    }

    @Test
    fun remoteErrorResetsClient() {
        val e = Env()
        e.client(TcpFlags.SYN, mss = 1460); e.s.onRemoteConnected(); e.cAck = 1001; e.client(TcpFlags.ACK)
        e.remote.failRead = true
        e.s.pump()
        assertTrue(e.out.last().rst)
        assertTrue(e.s.isClosed)
    }
}

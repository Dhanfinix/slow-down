package com.slowdown.throttle

import com.slowdown.throttle.core.Ipv4
import com.slowdown.throttle.core.PacketBuilder
import com.slowdown.throttle.core.TcpFlags
import com.slowdown.throttle.core.TcpSegment
import com.slowdown.throttle.engine.RelayEngine
import com.slowdown.throttle.engine.SocketProtector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Real NIO sockets on 127.0.0.1, fake TUN. A mini client TCP stack in the test downloads from a local server
 * through the RelayEngine and measures the speed.
 */
class RelayEngineLoopbackTest {
    private val cIp = 0x0a6fde01
    private val lo = 0x7f000001

    private class FakeTun {
        val toEngine = LinkedBlockingQueue<ByteArray>()
        val fromEngine = LinkedBlockingQueue<ByteArray>()
        val input = object : InputStream() {
            override fun read(): Int = throw UnsupportedOperationException()
            override fun read(b: ByteArray): Int {
                val p = try { toEngine.take() } catch (e: InterruptedException) { return -1 }
                System.arraycopy(p, 0, b, 0, p.size)
                return p.size
            }
        }
        val output = object : OutputStream() {
            override fun write(b: Int) = throw UnsupportedOperationException()
            override fun write(b: ByteArray) { fromEngine.put(b.copyOf()) }
        }
    }

    private val protector = object : SocketProtector {
        override fun protect(socket: Socket) = true
        override fun protect(socket: DatagramSocket) = true
    }

    private fun seg(p: ByteArray) = TcpSegment.parse(Ipv4.parse(p, p.size)!!)!!

    @Test
    fun downloadThroughEngineIsThrottledAndCompletes() {
        val kbps = 400
        val seconds = 10
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val total = kbps * 1000 / 8 * seconds * 3 // more than 10 s worth, server never runs dry
        thread(isDaemon = true) {
            server.accept().use { s ->
                val buf = ByteArray(16384) { 7 }
                var sent = 0
                while (sent < total) { val n = minOf(buf.size, total - sent); s.getOutputStream().write(buf, 0, n); sent += n }
            }
        }
        val tun = FakeTun()
        val engine = RelayEngine(tun.input, tun.output, protector, RelayEngine.ThrottleConfigLite(kbps, kbps, 0), { _, _ -> }, { })
        engine.start()
        try {
            val port = server.localPort
            var cSeq = 100
            tun.toEngine.put(PacketBuilder.tcp(cIp, lo, 50000, port, cSeq, 0, TcpFlags.SYN, 65535, mss = 1360)); cSeq++
            val sa = seg(tun.fromEngine.poll(5, TimeUnit.SECONDS)!!)
            assertEquals(TcpFlags.SYN or TcpFlags.ACK, sa.flags)
            var rcvNxt = sa.seq + 1
            tun.toEngine.put(PacketBuilder.tcp(cIp, lo, 50000, port, cSeq, rcvNxt, TcpFlags.ACK, 65535))

            var got = 0L
            val start = System.nanoTime()
            val end = start + seconds * 1_000_000_000L
            while (System.nanoTime() < end) {
                val p = tun.fromEngine.poll(200, TimeUnit.MILLISECONDS) ?: continue
                val s = seg(p)
                if (s.payloadLen > 0 && s.seq == rcvNxt) {
                    rcvNxt += s.payloadLen; got += s.payloadLen
                    tun.toEngine.put(PacketBuilder.tcp(cIp, lo, 50000, port, cSeq, rcvNxt, TcpFlags.ACK, 65535))
                }
            }
            val elapsed = (System.nanoTime() - start) / 1e9
            val avgKbps = got * 8 / 1000 / elapsed
            println("loopback avg=$avgKbps kbps target=$kbps bytes=$got")
            assertTrue("avg=$avgKbps", avgKbps in kbps * 0.8..kbps * 1.2)
        } finally {
            engine.stop()
            server.close()
        }
    }

    @Test
    fun udpEchoRoundTripThroughEngine() {
        val echo = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
        thread(isDaemon = true) {
            val b = ByteArray(1500)
            val pk = java.net.DatagramPacket(b, b.size)
            echo.receive(pk)
            echo.send(java.net.DatagramPacket(pk.data, pk.length, pk.socketAddress))
        }
        val tun = FakeTun()
        val engine = RelayEngine(tun.input, tun.output, protector, RelayEngine.ThrottleConfigLite(100, 100, 0), { _, _ -> }, { })
        engine.start()
        try {
            val msg = "ping".toByteArray()
            tun.toEngine.put(PacketBuilder.udp(cIp, lo, 6000, echo.localPort, msg, 0, msg.size))
            val p = tun.fromEngine.poll(5, TimeUnit.SECONDS)!!
            val u = com.slowdown.throttle.core.UdpDatagram.parse(Ipv4.parse(p, p.size)!!)!!
            assertEquals(echo.localPort, u.srcPort)
            assertEquals(6000, u.dstPort)
            assertEquals("ping", String(p, u.payloadOff, u.payloadLen))
        } finally {
            engine.stop(); echo.close()
        }
    }
}

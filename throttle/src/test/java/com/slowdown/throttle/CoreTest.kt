package com.slowdown.throttle

import com.slowdown.throttle.core.Checksum
import com.slowdown.throttle.core.Ipv4
import com.slowdown.throttle.core.PacketBuilder
import com.slowdown.throttle.core.Proto
import com.slowdown.throttle.core.TcpFlags
import com.slowdown.throttle.core.TcpSegment
import com.slowdown.throttle.core.TokenBucket
import com.slowdown.throttle.core.UdpDatagram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreTest {
    private val src = 0x0a6fde01
    private val dst = 0x5db8d822

    @Test
    fun checksumKnownVector() {
        // RFC 1071 example: words 0001 f203 f4f5 f6f7 -> sum ddf2 -> checksum 220d
        val d = byteArrayOf(0x00, 0x01, 0xf2.toByte(), 0x03, 0xf4.toByte(), 0xf5.toByte(), 0xf6.toByte(), 0xf7.toByte())
        assertEquals(0x220d, Checksum.compute(d, 0, d.size))
    }

    @Test
    fun checksumOddLength() {
        val d = byteArrayOf(0x12, 0x34, 0x56)
        assertEquals(Checksum.fold(0x1234L + 0x5600L), Checksum.compute(d, 0, 3))
    }

    @Test
    fun tcpRoundTripAndChecksums() {
        val payload = ByteArray(101) { it.toByte() } // odd length on purpose
        val pkt = PacketBuilder.tcp(src, dst, 40000, 443, 0x7ffffff0, 12345, TcpFlags.ACK or TcpFlags.PSH, 4096, payload, 0, payload.size)
        // A valid header/segment sums to zero when the stored checksum is included.
        assertEquals(0, Checksum.compute(pkt, 0, 20))
        assertEquals(0, Checksum.transportV4(pkt, 20, pkt.size - 20, src, dst, Proto.TCP))
        val ip = Ipv4.parse(pkt, pkt.size)!!
        assertEquals(src, ip.srcIp); assertEquals(dst, ip.dstIp); assertEquals(Proto.TCP, ip.protocol)
        val t = TcpSegment.parse(ip)!!
        assertEquals(40000, t.srcPort); assertEquals(443, t.dstPort)
        assertEquals(0x7ffffff0, t.seq); assertEquals(12345, t.ack)
        assertEquals(4096, t.window); assertEquals(101, t.payloadLen)
        assertTrue(t.hasAck && !t.syn && !t.fin)
        assertEquals(payload.toList(), pkt.copyOfRange(t.payloadOff, t.payloadOff + t.payloadLen).toList())
    }

    @Test
    fun tcpSynParsesMss() {
        val pkt = PacketBuilder.tcp(src, dst, 1000, 80, 1, 0, TcpFlags.SYN, 65535, mss = 1360)
        assertEquals(0, Checksum.transportV4(pkt, 20, pkt.size - 20, src, dst, Proto.TCP))
        val t = TcpSegment.parse(Ipv4.parse(pkt, pkt.size)!!)!!
        assertEquals(1360, t.mss); assertTrue(t.syn); assertEquals(0, t.payloadLen)
    }

    @Test
    fun udpRoundTrip() {
        val payload = "hello".toByteArray()
        val pkt = PacketBuilder.udp(src, dst, 5353, 53, payload, 0, payload.size)
        assertEquals(0, Checksum.compute(pkt, 0, 20))
        assertEquals(0, Checksum.transportV4(pkt, 20, pkt.size - 20, src, dst, Proto.UDP))
        val u = UdpDatagram.parse(Ipv4.parse(pkt, pkt.size)!!)!!
        assertEquals(5353, u.srcPort); assertEquals(53, u.dstPort)
        assertEquals("hello", String(pkt, u.payloadOff, u.payloadLen))
    }

    @Test
    fun rejectsMalformed() {
        assertNull(Ipv4.parse(ByteArray(10), 10))
        val pkt = PacketBuilder.udp(src, dst, 1, 2, ByteArray(4), 0, 4)
        assertNull(Ipv4.parse(pkt, pkt.size - 1)) // truncated
    }

    @Test
    fun icmpv6Unreachable() {
        val orig = ByteArray(60)
        orig[0] = 0x60; orig[6] = Proto.TCP.toByte()
        for (i in 8 until 40) orig[i] = (i * 3).toByte()
        val r = PacketBuilder.icmpv6Unreachable(orig, orig.size)!!
        assertEquals(40 + 8 + 60, r.size)
        assertEquals(1, r[40].toInt()); assertEquals(4, r[41].toInt())
        // verify checksum with the IPv6 pseudo header
        var s = Checksum.sum(r, 8, 32)
        s += (r.size - 40) + Proto.ICMPV6
        s = Checksum.sum(r, 40, r.size - 40, s)
        assertEquals(0, Checksum.fold(s))
        orig[6] = Proto.ICMPV6.toByte()
        assertNull(PacketBuilder.icmpv6Unreachable(orig, orig.size))
        assertNotNull(PacketBuilder.icmpv6Unreachable(orig.also { it[6] = Proto.UDP.toByte() }, orig.size))
    }

    @Test
    fun tokenBucketRateAccuracy() {
        var now = 0L
        val rate = TokenBucket.bytesPerSec(800) // 100 kB/s
        val b = TokenBucket(rate, nanoTime = { now })
        var taken = 0L
        // poll every 1 ms for 10 s, taking 1360-byte chunks as the session does
        while (now < 10_000_000_000L) {
            while (b.tryTake(1360)) taken += 1360
            now += 1_000_000
        }
        val expected = rate * 10
        assertTrue("taken=$taken expected=$expected", taken in (expected * 95 / 100)..(expected * 112 / 100))
    }

    @Test
    fun tokenBucketBurstCapAndRefund() {
        var now = 0L
        val b = TokenBucket(1000, burstBytes = 3000, nanoTime = { now })
        assertEquals(3000, b.available())
        now += 60_000_000_000L // a minute idle must not accumulate more than the burst
        assertEquals(3000, b.available())
        assertEquals(1000, b.takeUpTo(1000))
        b.give(500)
        assertEquals(2500, b.available())
        assertTrue(!b.tryTake(5000))
        b.setRate(2000)
        assertEquals(3000, TokenBucket.burstFor(2000)) // floor of two segments
    }
}

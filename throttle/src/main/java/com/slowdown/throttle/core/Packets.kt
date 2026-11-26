package com.slowdown.throttle.core

object Proto {
    const val ICMP = 1
    const val TCP = 6
    const val UDP = 17
    const val ICMPV6 = 58
}

object TcpFlags {
    const val FIN = 0x01
    const val SYN = 0x02
    const val RST = 0x04
    const val PSH = 0x08
    const val ACK = 0x10
}

internal fun ByteArray.u8(i: Int) = this[i].toInt() and 0xff
internal fun ByteArray.u16(i: Int) = (u8(i) shl 8) or u8(i + 1)
internal fun ByteArray.i32(i: Int) = (u8(i) shl 24) or (u8(i + 1) shl 16) or (u8(i + 2) shl 8) or u8(i + 3)
internal fun ByteArray.put16(i: Int, v: Int) { this[i] = (v ushr 8).toByte(); this[i + 1] = v.toByte() }
internal fun ByteArray.put32(i: Int, v: Int) {
    this[i] = (v ushr 24).toByte(); this[i + 1] = (v ushr 16).toByte()
    this[i + 2] = (v ushr 8).toByte(); this[i + 3] = v.toByte()
}

/** Parsed IPv4 header. Only the fields the relay needs. */
class Ipv4(val buf: ByteArray, val len: Int) {
    var headerLen = 0; private set
    var totalLen = 0; private set
    var protocol = 0; private set
    var srcIp = 0; private set
    var dstIp = 0; private set
    var isFragment = false; private set
    val payloadOff get() = headerLen
    val payloadLen get() = totalLen - headerLen

    fun dstBytes(): ByteArray = byteArrayOf((dstIp ushr 24).toByte(), (dstIp ushr 16).toByte(), (dstIp ushr 8).toByte(), dstIp.toByte())

    companion object {
        fun parse(buf: ByteArray, len: Int): Ipv4? {
            if (len < 20 || (buf.u8(0) ushr 4) != 4) return null
            val p = Ipv4(buf, len)
            p.headerLen = (buf.u8(0) and 0x0f) * 4
            p.totalLen = buf.u16(2)
            if (p.headerLen < 20 || p.totalLen < p.headerLen || p.totalLen > len) return null
            p.protocol = buf.u8(9)
            val frag = buf.u16(6)
            p.isFragment = (frag and 0x2000) != 0 || (frag and 0x1fff) != 0
            p.srcIp = buf.i32(12)
            p.dstIp = buf.i32(16)
            return p
        }
    }
}

class TcpSegment(
    val srcPort: Int, val dstPort: Int, val seq: Int, val ack: Int, val flags: Int,
    val window: Int, val mss: Int, val buf: ByteArray, val payloadOff: Int, val payloadLen: Int,
) {
    val syn get() = flags and TcpFlags.SYN != 0
    val fin get() = flags and TcpFlags.FIN != 0
    val rst get() = flags and TcpFlags.RST != 0
    val hasAck get() = flags and TcpFlags.ACK != 0

    companion object {
        fun parse(ip: Ipv4): TcpSegment? {
            val o = ip.payloadOff
            if (ip.payloadLen < 20) return null
            val b = ip.buf
            val dataOff = (b.u8(o + 12) ushr 4) * 4
            if (dataOff < 20 || dataOff > ip.payloadLen) return null
            var mss = 0
            var i = o + 20
            val optEnd = o + dataOff
            while (i < optEnd) {
                val kind = b.u8(i)
                if (kind == 0) break
                if (kind == 1) { i++; continue }
                if (i + 1 >= optEnd) break
                val l = b.u8(i + 1)
                if (l < 2 || i + l > optEnd) break
                if (kind == 2 && l == 4) mss = b.u16(i + 2)
                i += l
            }
            return TcpSegment(
                b.u16(o), b.u16(o + 2), b.i32(o + 4), b.i32(o + 8), b.u8(o + 13), b.u16(o + 14), mss,
                b, o + dataOff, ip.payloadLen - dataOff,
            )
        }
    }
}

class UdpDatagram(val srcPort: Int, val dstPort: Int, val buf: ByteArray, val payloadOff: Int, val payloadLen: Int) {
    companion object {
        fun parse(ip: Ipv4): UdpDatagram? {
            if (ip.payloadLen < 8) return null
            val o = ip.payloadOff
            val l = ip.buf.u16(o + 4)
            if (l < 8 || l > ip.payloadLen) return null
            return UdpDatagram(ip.buf.u16(o), ip.buf.u16(o + 2), ip.buf, o + 8, l - 8)
        }
    }
}

object PacketBuilder {
    private var ipId = 0

    private fun ipv4Header(p: ByteArray, total: Int, proto: Int, src: Int, dst: Int) {
        p[0] = 0x45; p[1] = 0
        p.put16(2, total)
        p.put16(4, ipId++ and 0xffff)
        p.put16(6, 0x4000) // DF
        p[8] = 64; p[9] = proto.toByte()
        p.put32(12, src); p.put32(16, dst)
        p.put16(10, Checksum.compute(p, 0, 20))
    }

    fun tcp(
        srcIp: Int, dstIp: Int, srcPort: Int, dstPort: Int, seq: Int, ack: Int, flags: Int, window: Int,
        payload: ByteArray? = null, pOff: Int = 0, pLen: Int = 0, mss: Int = 0,
    ): ByteArray {
        val tcpHdr = if (mss > 0) 24 else 20
        val tcpLen = tcpHdr + pLen
        val p = ByteArray(20 + tcpLen)
        ipv4Header(p, p.size, Proto.TCP, srcIp, dstIp)
        val o = 20
        p.put16(o, srcPort); p.put16(o + 2, dstPort)
        p.put32(o + 4, seq); p.put32(o + 8, ack)
        p[o + 12] = ((tcpHdr / 4) shl 4).toByte()
        p[o + 13] = flags.toByte()
        p.put16(o + 14, window.coerceIn(0, 0xffff))
        if (mss > 0) { p[o + 20] = 2; p[o + 21] = 4; p.put16(o + 22, mss) }
        if (pLen > 0) System.arraycopy(payload!!, pOff, p, o + tcpHdr, pLen)
        p.put16(o + 16, Checksum.transportV4(p, o, tcpLen, srcIp, dstIp, Proto.TCP))
        return p
    }

    fun udp(srcIp: Int, dstIp: Int, srcPort: Int, dstPort: Int, payload: ByteArray, pOff: Int, pLen: Int): ByteArray {
        val udpLen = 8 + pLen
        val p = ByteArray(20 + udpLen)
        ipv4Header(p, p.size, Proto.UDP, srcIp, dstIp)
        val o = 20
        p.put16(o, srcPort); p.put16(o + 2, dstPort); p.put16(o + 4, udpLen)
        System.arraycopy(payload, pOff, p, o + 8, pLen)
        var c = Checksum.transportV4(p, o, udpLen, srcIp, dstIp, Proto.UDP)
        if (c == 0) c = 0xffff
        p.put16(o + 6, c)
        return p
    }

    /**
     * ICMPv6 "destination unreachable / port unreachable" for an IPv6 TCP or UDP packet, so apps fail fast on IPv6
     * and fall back to IPv4. Returns null for anything else (extension headers, ICMPv6, short packets).
     */
    fun icmpv6Unreachable(orig: ByteArray, len: Int): ByteArray? {
        if (len < 40 || (orig.u8(0) ushr 4) != 6) return null
        val nh = orig.u8(6)
        if (nh != Proto.TCP && nh != Proto.UDP) return null
        val quoted = minOf(len, 1280 - 48)
        val icmpLen = 8 + quoted
        val p = ByteArray(40 + icmpLen)
        p[0] = 0x60
        p.put16(4, icmpLen)
        p[6] = Proto.ICMPV6.toByte(); p[7] = 64
        System.arraycopy(orig, 24, p, 8, 16)  // src = original dst
        System.arraycopy(orig, 8, p, 24, 16)  // dst = original src
        p[40] = 1; p[41] = 4
        System.arraycopy(orig, 0, p, 48, quoted)
        var s = Checksum.sum(p, 8, 32)
        s += icmpLen + Proto.ICMPV6
        s = Checksum.sum(p, 40, icmpLen, s)
        p.put16(42, Checksum.fold(s))
        return p
    }
}

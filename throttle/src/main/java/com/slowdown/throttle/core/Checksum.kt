package com.slowdown.throttle.core

/** Internet (RFC 1071) one's complement checksum helpers. */
object Checksum {
    /** Adds the 16-bit big-endian words of [data] to [initial]. An odd trailing byte is padded with zero. */
    fun sum(data: ByteArray, off: Int, len: Int, initial: Long = 0L): Long {
        var s = initial
        var i = off
        val end = off + len
        while (i + 1 < end) {
            s += ((data[i].toInt() and 0xff) shl 8) or (data[i + 1].toInt() and 0xff)
            i += 2
        }
        if (i < end) s += (data[i].toInt() and 0xff) shl 8
        return s
    }

    fun fold(sum: Long): Int {
        var s = sum
        while (s ushr 16 != 0L) s = (s and 0xffff) + (s ushr 16)
        return (s.inv() and 0xffff).toInt()
    }

    fun compute(data: ByteArray, off: Int, len: Int): Int = fold(sum(data, off, len))

    /** IPv4 pseudo header sum for TCP/UDP. */
    fun pseudoV4(src: Int, dst: Int, protocol: Int, length: Int): Long =
        (src ushr 16).toLong() + (src and 0xffff) + (dst ushr 16) + (dst and 0xffff) + protocol + length

    /** Checksum of a TCP/UDP segment at [off]..[off]+[len] including the IPv4 pseudo header. */
    fun transportV4(buf: ByteArray, off: Int, len: Int, src: Int, dst: Int, protocol: Int): Int =
        fold(sum(buf, off, len, pseudoV4(src, dst, protocol, len)))
}

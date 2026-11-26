package com.slowdown.throttle.engine

import com.slowdown.throttle.core.RemoteConnection
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

internal class NioRemote(val channel: SocketChannel) : RemoteConnection {
    override fun read(dst: ByteArray, off: Int, len: Int): Int = channel.read(ByteBuffer.wrap(dst, off, len))
    override fun write(src: ByteArray, off: Int, len: Int): Int = channel.write(ByteBuffer.wrap(src, off, len))
    override fun shutdownOutput() { try { channel.shutdownOutput() } catch (_: Exception) {} }
    override fun close() { try { channel.close() } catch (_: Exception) {} }
}

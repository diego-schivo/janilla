package com.janilla.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleTransfer implements Transfer {

	protected final ByteChannel channel;

	protected final ByteBuffer in;

	protected final Lock inLock = new ReentrantLock();

	protected final ByteBuffer out;

	protected final Lock outLock = new ReentrantLock();

	public SimpleTransfer(ByteChannel channel) {
		this.channel = channel;
		in = ByteBuffer.allocate(capacity());
		out = ByteBuffer.allocate(capacity());
	}

	@Override
	public ByteBuffer in() {
		return in;
	}

	@Override
	public Lock inLock() {
		return inLock;
	}

	@Override
	public ByteBuffer out() {
		return out;
	}

	@Override
	public Lock outLock() {
		return outLock;
	}

	@Override
	public int read() throws IOException {
		inLock.lock();
		try {
			return channel.read(in);
		} finally {
			inLock.unlock();
		}
	}

	@Override
	public void write() throws IOException {
		outLock.lock();
		try {
			channel.write(out);
		} finally {
			outLock.unlock();
		}
	}

	protected int capacity() {
		return 8 * 1024;
	}
}

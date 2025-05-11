/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

public class SecureTransfer {

	protected final ByteChannel channel;

	protected final ByteBuffer in;

	protected final Lock inLock = new ReentrantLock();

	protected final ByteBuffer out;

	protected final Lock outLock = new ReentrantLock();

	protected final SSLEngine engine;

	protected final ByteBuffer in0;

	protected final ByteBuffer out0;

	public SecureTransfer(ByteChannel channel, SSLEngine engine) {
		this.channel = channel;
		this.engine = engine;
		in = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
		out = ByteBuffer.allocate(in.capacity());
		in0 = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
		out0 = ByteBuffer.allocate(in0.capacity());
	}

	public ByteBuffer in() {
		return in;
	}

	public Lock inLock() {
		return inLock;
	}

	public ByteBuffer out() {
		return out;
	}

	public Lock outLock() {
		return outLock;
	}

	public int read() throws IOException {
		inLock.lock();
		try {
			var p = in.position();
			while (!engine.isInboundDone()) {
				handshake();
				if (!in.hasRemaining() || in.position() > p)
					return in.position() - p;
				var n = read0();
				if (n == -1)
					return -1;
			}
			return -1;
		} finally {
			inLock.unlock();
		}
	}

	public void write() throws IOException {
		outLock.lock();
		try {
			var p = out.position();
			while (!engine.isOutboundDone()) {
				handshake();
				if (!out.hasRemaining() || out.position() > p)
					break;
				write0();
			}
		} finally {
			outLock.unlock();
		}
	}

	protected void handshake() throws IOException {
//		var t = engine.getUseClientMode() ? "C" : "S";
		while (engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
			switch (engine.getHandshakeStatus()) {
			case NEED_TASK:
//				System.out.println(t + ": task");
				engine.getDelegatedTask().run();
				break;
			case NEED_UNWRAP:
				read0();
				break;
			case NEED_WRAP:
				write0();
				break;
			default:
				throw new IOException(engine.getHandshakeStatus().toString());
			}
		}
	}

	protected int read0() throws IOException {
		inLock.lock();
		try {
//			var t = engine.getUseClientMode() ? "C" : "S";
			for (var r = in0.position() == 0;;) {
				if (r) {
//					System.out.println(t + ": read");
					@SuppressWarnings("unused")
					var n = channel.read(in0);
//					System.out.println(t + ": read " + n);
					if (n == -1)
						return -1;
				}
				in0.flip();
//				System.out.println(t + ": unwrap");
				@SuppressWarnings("unused")
				var ser = engine.unwrap(in0, in);
//				System.out.println(t + ": unwrap " + ser);
				in0.compact();
				r = ser.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW;
				if (!r)
					return ser.bytesConsumed();
			}
		} finally {
			inLock.unlock();
		}
	}

	protected void write0() throws IOException {
		outLock.lock();
		try {
//			var t = engine.getUseClientMode() ? "C" : "S";
			out0.clear();
//			System.out.println(t + ": wrap");
			@SuppressWarnings("unused")
			var r = engine.wrap(out, out0);
//			System.out.println(t + ": wrap " + r);
			out0.flip();
			while (out0.hasRemaining()) {
//				System.out.println(t + ": write");
				@SuppressWarnings("unused")
				var n = channel.write(out0);
//				System.out.println(t + ": write " + n);
			}
		} finally {
			outLock.unlock();
		}
	}
}

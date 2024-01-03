/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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
package com.janilla.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Path;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import com.janilla.io.ByteBufferByteChannel;
import com.janilla.io.ByteBufferHolder;
import com.janilla.io.FilterByteChannel;
import com.janilla.io.IO;
import com.janilla.net.Net;

public class SSLByteChannel extends FilterByteChannel {

	public static void main(String[] args) throws IOException {
		var k = Path.of(System.getProperty("user.home")).resolve("Downloads/jssesamples/samples/sslengine/testkeys");
		var p = "passphrase".toCharArray();
		var x = Net.getSSLContext(k, p);

		var co = new ByteBufferHolder();
		co.setBuffer(ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY));
		var so = new ByteBufferHolder();
		so.setBuffer(ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY));

		var ce = x.createSSLEngine("client", 80);
		ce.setUseClientMode(true);

		var se = x.createSSLEngine();
		se.setUseClientMode(false);
		se.setNeedClientAuth(true);

		var cs = new SSLByteChannel[2];
		try (var c = new SSLByteChannel(new ByteBufferByteChannel(so, co), ce);
				var s = new SSLByteChannel(new ByteBufferByteChannel(co, so), se);) {
			cs[0] = c;
			cs[1] = s;

			var cb = ByteBuffer.wrap("Hi Server, I'm Client".getBytes());
			c.write(cb);
			var sb = ByteBuffer.allocate(100);
			var z = ByteBuffer.allocate(0);
			while (s.read(sb) == 0) {
				s.write(z);
				c.read(z);
				c.write(cb);
			}
			var t = new String(sb.array(), 0, sb.position());
			System.out.println(t);
			assert t.equals("Hi Server, I'm Client") : t;

			cb = ByteBuffer.wrap("Hello Client, I'm Server".getBytes());
			s.write(cb);
			sb.clear();
			while (c.read(sb) == 0) {
				c.write(z);
				s.read(z);
				s.write(cb);
			}
			t = new String(sb.array(), 0, sb.position());
			System.out.println(t);
			assert t.equals("Hello Client, I'm Server") : t;
		}

		var c = cs[0];
		var s = cs[1];
		while (c.isOpen() || s.isOpen()) {
			if (c.isOpen())
				c.close();
			if (s.isOpen())
				s.close();
		}
	}

	protected SSLEngine engine;

	protected ByteBuffer application1;

	protected ByteBuffer application2;

	protected ByteBuffer packet1;

	protected ByteBuffer packet2;

	protected Status status = Status.OK;

	protected HandshakeStatus handshake = HandshakeStatus.NOT_HANDSHAKING;

	protected Close close;

	public SSLByteChannel(ByteChannel channel, SSLEngine engine) {
		super(channel);
		this.engine = engine;

		var s = engine.getSession();
		application1 = ByteBuffer.allocate(s.getApplicationBufferSize());
		application2 = ByteBuffer.allocate(0);
		packet1 = ByteBuffer.allocate(s.getPacketBufferSize());
		packet2 = ByteBuffer.allocate(s.getPacketBufferSize());
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (application1.position() == 0) {
			switch (handshake) {
			case FINISHED:
			case NOT_HANDSHAKING:
			case NEED_UNWRAP:
				if (packet1.position() == 0 || status == Status.BUFFER_UNDERFLOW) {
					var n = super.read(packet1);
					if (n <= 0)
						return n;
				}
				packet1.flip();
				try {
					var r = engine.unwrap(packet1, application1);

//					System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read r\n\t"
//							+ r.toString().replace("\n", "\n\t"));

					status = r.getStatus();
					handshake = r.getHandshakeStatus();

					switch (status) {
					case OK:
					case BUFFER_UNDERFLOW:
					case CLOSED:
						break;
					default:
						throw new IOException(status.toString());
					}
				} finally {
					packet1.compact();
				}
				break;

			case NEED_WRAP:
				if (packet2.position() == 0) {
					application2.flip();
					try {
						var r = engine.wrap(application2, packet2);

//						System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read r\n\t"
//								+ r.toString().replace("\n", "\n\t"));

						status = r.getStatus();
						handshake = r.getHandshakeStatus();

						if (status != Status.OK)
							throw new IOException(status.toString());
					} finally {
						application2.compact();
					}
				}
				break;

			case NEED_TASK:
				engine.getDelegatedTask().run();
				handshake = engine.getHandshakeStatus();

//				System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read handshake=" + handshake);

				break;

			default:
				throw new IOException(handshake.toString());
			}

//			if (handshake != HandshakeStatus.NOT_HANDSHAKING)
//				System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read handshake=" + handshake);

			if (packet2.position() > 0) {
				packet2.flip();
				try {
					super.write(packet2);
				} finally {
					packet2.compact();
				}
			}
		}

		var p = application1.position();
		var r = dst.remaining();
		var n = Math.min(p, r);
		if (n == 0) {
			if (status == Status.CLOSED)
				n = -1;
		} else {
			application1.flip();
			if (n < p)
				application1.limit(n);
			dst.put(application1);
			if (n < p)
				application1.limit(p);
			application1.compact();
		}
		return n;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		var n = 0;
		switch (handshake) {
		case FINISHED:
		case NOT_HANDSHAKING:
		case NEED_WRAP:
			if (packet2.position() == 0) {
				var r = engine.wrap(src, packet2);

//				System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write r\n\t"
//						+ r.toString().replace("\n", "\n\t"));

				status = r.getStatus();
				handshake = r.getHandshakeStatus();

				if (status != Status.OK)
					throw new IOException(status.toString());

				n = r.bytesConsumed();
			}
			break;

		case NEED_UNWRAP:
			if (packet1.position() == 0)
				super.read(packet1);
			if (packet1.position() > 0) {
				packet1.flip();
				try {
					var r = engine.unwrap(packet1, application1);

//					System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write r\n\t"
//							+ r.toString().replace("\n", "\n\t"));

					status = r.getStatus();
					handshake = r.getHandshakeStatus();

					if (status != Status.OK && status != Status.BUFFER_UNDERFLOW)
						throw new IOException(status.toString());
				} finally {
					packet1.compact();
				}
			}
			break;

		case NEED_TASK:
			engine.getDelegatedTask().run();
			handshake = engine.getHandshakeStatus();

//			System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write handshake=" + handshake);

			break;

		default:
			throw new IOException(handshake.toString());
		}

//		if (handshake != HandshakeStatus.NOT_HANDSHAKING)
//			System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write handshake=" + handshake);

		if (packet2.position() > 0) {
			packet2.flip();
			try {
				super.write(packet2);
			} finally {
				packet2.compact();
			}
		}

		return n;
	}

	@Override
	public boolean isOpen() {
		return close != null ? close.state < 3 : super.isOpen();
	}

	@Override
	public void close() throws IOException {
		if (close == null)
			close = new Close();
		if (close.state < 3)
			try {
				close.run();
			} catch (UncheckedIOException e) {
				throw e.getCause();
			}
	}

	protected class Close implements Runnable {

		int state;

		@Override
		public void run() {
			try {
				state = switch (state) {
				case 0 -> {
					if (packet2.position() > 0) {
						packet2.flip();
						try {
							while (packet2.hasRemaining())
								SSLByteChannel.super.write(packet2);
						} finally {
							packet2.compact();
						}
					}

					if (engine.isOutboundDone())
						yield 1;

					engine.closeOutbound();
					application2.flip();
					try {
						var r = engine.wrap(application2, packet2);

//						System.out.println(Thread.currentThread().getName() + " SSLByteChannel.close r\n\t"
//								+ r.toString().replace("\n", "\n\t"));

						status = r.getStatus();
						handshake = r.getHandshakeStatus();
					} finally {
						application2.compact();
					}
					yield 0;
				}
				case 1 -> {
					if (engine.isInboundDone())
						yield 2;
					if (packet1.position() == 0 || status == Status.BUFFER_UNDERFLOW) {
						var n = SSLByteChannel.super.read(packet1);
						if (n < 0)
							yield 2;
					}
					packet1.flip();
					try {
						var r = engine.unwrap(packet1, application1);

//						System.out.println(Thread.currentThread().getName() + " SSLByteChannel.close r\n\t"
//								+ r.toString().replace("\n", "\n\t"));

						status = r.getStatus();
						handshake = r.getHandshakeStatus();
					} finally {
						packet1.compact();
					}
					yield 1;
				}
				case 2 -> {
					engine.closeInbound();
					SSLByteChannel.super.close();
					yield 3;
				}
				default -> throw new RuntimeException();
				};
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}
}

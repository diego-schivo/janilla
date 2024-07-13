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
package com.janilla.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import com.janilla.io.FilterByteChannel;

public class SSLByteChannel extends FilterByteChannel {

	protected SSLEngine engine;

	protected ByteBuffer application1;

	protected ByteBuffer application2;

	protected ByteBuffer packet1;

	protected ByteBuffer packet2;

	protected SSLEngineResult.Status status;

	protected SSLEngineResult.HandshakeStatus handshake;

	protected SSLException sslException;

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
		if (sslException != null)
			throw new IOException("SSL");
		try {

//			System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read\n\t"
//					+ engine.getHandshakeStatus().toString().replace("\n", "\n\t"));

			while (application1.position() == 0) {
				if (handshake == null)
					handshake = engine.getUseClientMode() ? SSLEngineResult.HandshakeStatus.NEED_WRAP
							: SSLEngineResult.HandshakeStatus.NEED_UNWRAP;

				switch (handshake) {
				case FINISHED:
				case NOT_HANDSHAKING:
				case NEED_UNWRAP:
					if (packet1.position() == 0 || status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
						try {
							var n = super.read(packet1);
							if (n <= 0)
								return n;
						} catch (ClosedChannelException e) {
							return -1;
						}
					packet1.flip();
					try {
						var r = engine.unwrap(packet1, application1);

//						System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read r\n\t"
//								+ r.toString().replace("\n", "\n\t"));

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

//							System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read r\n\t"
//									+ r.toString().replace("\n", "\n\t"));

							status = r.getStatus();
							handshake = r.getHandshakeStatus();

							switch (status) {
							case OK:
							case CLOSED:
								break;
							default:
								throw new IOException(status.toString());
							}
						} finally {
							application2.compact();
						}
					}
					break;

				case NEED_TASK:
					engine.getDelegatedTask().run();

//					System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read handshake=" + handshake
//							+ "->" + engine.getHandshakeStatus());

					handshake = engine.getHandshakeStatus();
					break;

				default:
					throw new IOException(handshake.toString());
				}

//				if (handshake != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
//					System.out
//							.println(Thread.currentThread().getName() + " SSLByteChannel.read handshake=" + handshake);

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
				if (status == SSLEngineResult.Status.CLOSED)
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
		} catch (SSLException e) {
			this.sslException = e;
			throw e;
		}
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		if (sslException != null)
			throw new IOException("SSL");
		try {
//			System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write\n\t"
//					+ engine.getHandshakeStatus().toString().replace("\n", "\n\t"));

			if (handshake == null)
				handshake = engine.getUseClientMode() ? SSLEngineResult.HandshakeStatus.NEED_WRAP
						: SSLEngineResult.HandshakeStatus.NEED_UNWRAP;

			var n = 0;
			do {
				switch (handshake) {
				case FINISHED:
				case NOT_HANDSHAKING:
				case NEED_WRAP:
					if (packet2.position() == 0) {
						var r = engine.wrap(src, packet2);

//					System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write r\n\t"
//							+ r.toString().replace("\n", "\n\t"));

						status = r.getStatus();
						handshake = r.getHandshakeStatus();

						switch (status) {
						case OK:
							break;
						case CLOSED:
							throw new IOException("Stream closed");
						default:
							throw new IOException(status.toString());
						}

						n = r.bytesConsumed();
					}
					break;

				case NEED_UNWRAP:
					if (packet1.position() == 0 || status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
						if (super.read(packet1) > 0) {
							packet1.flip();
							try {
								var r = engine.unwrap(packet1, application1);

//						System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write r\n\t"
//								+ r.toString().replace("\n", "\n\t"));

								status = r.getStatus();
								handshake = r.getHandshakeStatus();

								if (status != SSLEngineResult.Status.OK
										&& status != SSLEngineResult.Status.BUFFER_UNDERFLOW)
									throw new IOException(status.toString());
							} finally {
								packet1.compact();
							}
						}
					break;

				case NEED_TASK:
					engine.getDelegatedTask().run();

//				System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write handshake=" + handshake
//						+ "->" + engine.getHandshakeStatus());

					handshake = engine.getHandshakeStatus();
					break;

				default:
					throw new IOException(handshake.toString());
				}

//			if (handshake != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
//				System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write handshake=" + handshake);

				if (packet2.position() > 0) {
					packet2.flip();
					try {
						if (super.write(packet2) == 0)
							break;
					} finally {
						packet2.compact();
					}
				} else if (!src.hasRemaining())
					break;
			} while (n == 0);

			return n;
		} catch (SSLException e) {
			this.sslException = e;
			throw e;
		}
	}

	@Override
	public boolean isOpen() {
		return state < 3;
	}

	int state;

	@Override
	public void close() throws IOException {
		while (state < 3)
			state = switch (state) {
			case 0 -> {
				if (packet2.position() > 0) {
					packet2.flip();
					try {
						while (packet2.hasRemaining() && SSLByteChannel.super.write(packet2) > 0)
							;
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

//				System.out.println(Thread.currentThread().getName() + " SSLByteChannel.close r\n\t"
//						+ r.toString().replace("\n", "\n\t"));

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
				if (packet1.position() == 0 || status == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
					var n = SSLByteChannel.super.read(packet1);
					if (n < 0)
						yield 2;
				}
				packet1.flip();
				try {
					var r = engine.unwrap(packet1, application1);

//				System.out.println(Thread.currentThread().getName() + " SSLByteChannel.close r\n\t"
//						+ r.toString().replace("\n", "\n\t"));

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
			default -> throw new IllegalStateException();
			};
	}
}

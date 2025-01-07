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
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import com.janilla.io.FilterByteChannel;
import com.janilla.io.IO;

public class SslByteChannel extends FilterByteChannel {

	protected SSLEngine engine;

	protected ByteBuffer applicationInput;

	protected ByteBuffer applicationOutput;

	protected ByteBuffer packetInput;

	protected ByteBuffer packetOutput;

	protected SSLEngineResult.Status status;

	protected SSLEngineResult.HandshakeStatus handshake;

	protected SSLException sslException;

	protected final Lock outboundLock = new ReentrantLock();

	protected ByteBuffer outboundQueue;

	public SslByteChannel(ByteChannel channel, SSLEngine engine) {
		super(channel);
		this.engine = engine;

		var s = engine.getSession();
		applicationInput = ByteBuffer.allocate(s.getApplicationBufferSize());
		applicationOutput = ByteBuffer.allocate(0);
		packetInput = ByteBuffer.allocate(s.getPacketBufferSize());
		packetOutput = ByteBuffer.allocate(s.getPacketBufferSize());
		outboundQueue = ByteBuffer.allocate(packetOutput.capacity());
	}

	public Lock outboundLock() {
		return outboundLock;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (sslException != null)
			throw new IOException("SSL");
		try {

//			System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read\n\t"
//					+ engine.getHandshakeStatus().toString().replace("\n", "\n\t"));

			var z = 0;
			do {
				while (applicationInput.position() == 0) {
					if (handshake == null) {
						handshake = engine.getUseClientMode() ? SSLEngineResult.HandshakeStatus.NEED_WRAP
								: SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
//						System.out.println(
//								Thread.currentThread().getName() + " SSLByteChannel.read handshake=" + handshake);
					}

					switch (handshake) {
					case FINISHED:
					case NOT_HANDSHAKING:
					case NEED_UNWRAP:
						if (packetInput.position() == 0 || status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
							try {
								var n = super.read(packetInput);
								if (n <= 0)
									return z > 0 ? z : n;
							} catch (ClosedChannelException e) {
								return z > 0 ? z : -1;
							}
						packetInput.flip();
						try {
							var r = engine.unwrap(packetInput, applicationInput);

//							System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read r\n\t"
//									+ r.toString().replace("\n", "\n\t"));

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
							packetInput.compact();
						}
						break;

					case NEED_WRAP:
						if (outboundQueue.position() == 0) {
							applicationOutput.flip();
							try {
								outboundLock.lock();
								SSLEngineResult r;
								try {
									r = engine.wrap(applicationOutput, packetOutput);
									if (packetOutput.position() > 0) {
										packetOutput.flip();
										outboundQueue = IO.transferAllRemaining(packetOutput, outboundQueue);
										packetOutput.clear();
									}
								} finally {
									outboundLock.unlock();
								}

//								System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read r\n\t"
//										+ r.toString().replace("\n", "\n\t"));

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
								applicationOutput.compact();
							}
						}
						break;

					case NEED_TASK:
						engine.getDelegatedTask().run();

//						System.out.println(Thread.currentThread().getName() + " SSLByteChannel.read handshake="
//								+ handshake + "->" + engine.getHandshakeStatus());

						handshake = engine.getHandshakeStatus();
						break;

					default:
						throw new IOException(handshake.toString());
					}

//					if (handshake != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
//						System.out.println(
//								Thread.currentThread().getName() + " SSLByteChannel.read handshake=" + handshake);

					if (outboundQueue.position() > 0) {
						outboundLock.lock();
						outboundQueue.flip();
						try {
							super.write(outboundQueue);
//							if (outboundQueue.hasRemaining())
//								System.out.println("remaining=" + outboundQueue.remaining() + ", channel=" + channel
//										+ ", isBlocking="
//										+ (channel instanceof SocketChannel sc ? sc.isBlocking() : "?"));
						} finally {
							outboundQueue.compact();
							outboundLock.unlock();
						}
					}
				}

				var p = applicationInput.position();
				var r = dst.remaining();
				var n = Math.min(p, r);
				if (n == 0) {
					if (status == SSLEngineResult.Status.CLOSED)
						return z > 0 ? z : -1;
				} else {
					applicationInput.flip();
					if (n < p)
						applicationInput.limit(n);
					dst.put(applicationInput);
					if (n < p)
						applicationInput.limit(p);
					applicationInput.compact();
					z += n;
				}
			} while (dst.hasRemaining());
			return z;
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

			var z = 0;
			for (;;) {
//				System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write handshake=" + handshake
//						+ ", packetInput=" + packetInput.position() + ", packetOutput=" + packetOutput.position());
				switch (handshake) {
				case FINISHED:
				case NOT_HANDSHAKING:
				case NEED_WRAP:
					if (outboundQueue.position() == 0) {
						outboundLock.lock();
						SSLEngineResult r;
						try {
							r = engine.wrap(src, packetOutput);
							if (packetOutput.position() > 0) {
								packetOutput.flip();
								outboundQueue = IO.transferAllRemaining(packetOutput, outboundQueue);
								packetOutput.clear();
							}
						} finally {
							outboundLock.unlock();
						}

//						System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write r\n\t"
//								+ r.toString().replace("\n", "\n\t"));

						status = r.getStatus();
						handshake = r.getHandshakeStatus();

						switch (status) {
						case OK:
							break;
						case CLOSED:
							throw new IOException("Stream closed");
						case BUFFER_OVERFLOW:
							throw new IOException(
									status.toString() + ", src=" + src + ", packetOutput=" + packetOutput);
						default:
							throw new IOException(status.toString());
						}

						z += r.bytesConsumed();
					}
					break;

				case NEED_UNWRAP:
					if (packetInput.position() == 0 || status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
						super.read(packetInput);
					if (packetInput.position() > 0) {
						packetInput.flip();
						try {
							var r = engine.unwrap(packetInput, applicationInput);

//							System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write r\n\t"
//									+ r.toString().replace("\n", "\n\t"));

							status = r.getStatus();
							handshake = r.getHandshakeStatus();

							if (status != SSLEngineResult.Status.OK
									&& status != SSLEngineResult.Status.BUFFER_UNDERFLOW)
								throw new IOException(status.toString());
						} finally {
							packetInput.compact();
						}
					}
					break;

				case NEED_TASK:
					engine.getDelegatedTask().run();

//					System.out.println(Thread.currentThread().getName() + " SSLByteChannel.write handshake=" + handshake
//							+ "->" + engine.getHandshakeStatus());

					handshake = engine.getHandshakeStatus();
					break;

				default:
					throw new IOException(handshake.toString());
				}

//				if (handshake != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
//					System.out
//							.println(Thread.currentThread().getName() + " SSLByteChannel.write handshake=" + handshake);

				if (outboundQueue.position() > 0) {
					outboundLock.lock();
					outboundQueue.flip();
					try {
						super.write(outboundQueue);
//						if (outboundQueue.hasRemaining())
//							System.out.println("remaining=" + outboundQueue.remaining() + ", channel=" + channel
//									+ ", isBlocking=" + (channel instanceof SocketChannel sc ? sc.isBlocking() : "?"));
					} finally {
						outboundQueue.compact();
						outboundLock.unlock();
					}
				} else if (!src.hasRemaining())
					break;
			}
			return z;
		} catch (

		SSLException e) {
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
				if (outboundQueue.position() > 0) {
					outboundLock.lock();
					outboundQueue.flip();
					try {
						super.write(outboundQueue);
//						if (outboundQueue.hasRemaining())
//							System.out.println("remaining=" + outboundQueue.remaining() + ", channel=" + channel
//									+ ", isBlocking=" + (channel instanceof SocketChannel sc ? sc.isBlocking() : "?"));
					} finally {
						outboundQueue.compact();
						outboundLock.unlock();
					}
				}

				if (engine.isOutboundDone())
					yield 1;

				engine.closeOutbound();
				applicationOutput.flip();
				try {
					outboundLock.lock();
					SSLEngineResult r;
					try {
						r = engine.wrap(applicationOutput, packetOutput);
						if (packetOutput.position() > 0) {
							packetOutput.flip();
							outboundQueue = IO.transferAllRemaining(packetOutput, outboundQueue);
							packetOutput.clear();
						}
					} finally {
						outboundLock.unlock();
					}

//				System.out.println(Thread.currentThread().getName() + " SSLByteChannel.close r\n\t"
//						+ r.toString().replace("\n", "\n\t"));

					status = r.getStatus();
					handshake = r.getHandshakeStatus();
				} finally {
					applicationOutput.compact();
				}
				yield 0;
			}
			case 1 -> {
//				if (engine.isInboundDone())
//					yield 2;
				if (packetInput.position() == 0 || status == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
					var n = super.read(packetInput);
					if (n < 0)
						yield 2;
				}
				if (packetInput.position() > 0) {
					packetInput.flip();
					try {
						var r = engine.unwrap(packetInput, applicationInput);

//				System.out.println(Thread.currentThread().getName() + " SSLByteChannel.close r\n\t"
//						+ r.toString().replace("\n", "\n\t"));

						status = r.getStatus();
						handshake = r.getHandshakeStatus();
					} finally {
						packetInput.compact();
					}
				}
				yield 1;
			}
			case 2 -> {
//					engine.closeInbound();
				super.close();
				yield 3;
			}
			default -> throw new IllegalStateException();
			};
	}
}

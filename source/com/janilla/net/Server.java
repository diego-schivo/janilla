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
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;

import com.janilla.io.IO;

public class Server {

	private SocketAddress address;

//	private SSLContext sslContext;

	private Protocol protocol;

//	private Handler handler;

	protected volatile Selector selector;

	public SocketAddress getAddress() {
		return address;
	}

	public void setAddress(SocketAddress address) {
		this.address = address;
	}

//	public SSLContext getSslContext() {
//		return sslContext;
//	}
//
//	public void setSslContext(SSLContext sslContext) {
//		this.sslContext = sslContext;
//	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

//	public Handler getHandler() {
//		return handler;
//	}
//
//	public void setHandler(Handler handler) {
//		this.handler = handler;
//	}

	public void serve() {
		try {
			{
				var ssc = ServerSocketChannel.open();
				ssc.socket().bind(address);
				selector = Selector.open();
				ssc.configureBlocking(false);
				ssc.register(selector, SelectionKey.OP_ACCEPT);
			}
			var cc = new ArrayList<Connection>();
			for (;;) {
				var m = System.currentTimeMillis();
				selector.select(1000);
				System.out.println(millis() + " selector.select " + (System.currentTimeMillis() - m));
				for (var kk = selector.selectedKeys().iterator(); kk.hasNext();) {
					var k = kk.next();
					System.out.println("k=" + k);
					kk.remove();
					if (!k.isValid())
						continue;
					if (k.isAcceptable()) {
						var sc = ((ServerSocketChannel) k.channel()).accept();
						sc.configureBlocking(false);
//						var se = sslContext.createSSLEngine();
//						se.setUseClientMode(false);
//						var pp = se.getSSLParameters();
//						pp.setApplicationProtocols(new String[] { protocol.name() });
//						se.setSSLParameters(pp);
//						var c = protocol.buildConnection(sc, se);
						var c = protocol.buildConnection(sc);
						System.out.println("c=" + c.number());
						Thread.startVirtualThread(() -> handle(c));
//						sc.register(selector, SelectionKey.OP_READ).attach(c);
						synchronized (c) {
							while (c.r < 0)
								try {
									c.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
									break;
								}
						}
					}
					if (k.isReadable()) {
						k.cancel();
						cc.add((Connection) k.attachment());
					}
				}
				if (cc.isEmpty())
					continue;
				System.out.println(millis() + " selector.selectNow");
				selector.selectNow();
				for (var c : cc) {
					System.out.println("notify c=" + c.number());
					synchronized (c) {
						c.r++;
						c.notify();
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected void handle(Connection connection) {
		var bb1 = ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY);
		var bb2 = ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY);
		for (;;) {
			synchronized (connection) {
				if (connection.r < 0) {
					connection.r = 0;
					connection.notify();
				}
				while (connection.r == 0)
					try {
						connection.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				connection.r--;
			}
			System.out.println("wait connection=" + connection.number());
			try {
				{
					var n = connection.applicationChannel().read(bb1);
					connection.socketChannel().register(selector, SelectionKey.OP_READ).attach(connection);
					selector.wakeup();
					System.out.println("connection=" + connection.number() + " n=" + n);
				}
//				var k = true;
//				try (var ex = protocol.buildExchange(connection)) {
//					if (ex != null) {
//						Exception e;
//						try {
//							k = handler.handle(ex);
//							e = null;
//						} catch (UncheckedIOException x) {
//							e = x.getCause();
//						} catch (Exception x) {
//							e = x;
//						}
//
//						if (e != null)
//							try {
//								e.printStackTrace();
//								ex.setException(e);
//								k = handler.handle(ex);
//							} catch (Exception x) {
//								k = false;
//							}
//
//						if (((HttpExchange) ex).getRequest().getUri().getPath().endsWith("/base.css")) {
//							System.out.println("""
//									XXXXXXXXXX
//									XXXXXXXXXX
//									XXXXXXXXXX
//									XXXXXXXXXX
//									XXXXXXXXXX""");
//						}
//						System.out.println(millis() + " A");
//					}
//				}
//				System.out.println(millis() + " C " + k);
//				if (k) {
//					connection.socketChannel().register(selector, SelectionKey.OP_READ).attach(connection);
//					System.out.println(millis() + " selector.wakeup");
//					selector.wakeup();
//				} else
//					connection.socketChannel().close();
				bb1.flip();
				protocol.handle(connection, bb1, bb2);
				bb1.compact();
				bb2.flip();
				connection.applicationChannel().write(bb2);
				bb2.compact();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("connection=" + connection.number());
			}
		}
	}

	long start = System.currentTimeMillis();

	long millis() {
		return System.currentTimeMillis() - start;
	}
}

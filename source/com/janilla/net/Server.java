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

	protected SocketAddress address;

	protected Protocol protocol;

	protected volatile Selector selector;

	public void setAddress(SocketAddress address) {
		this.address = address;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

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
//				var m = System.currentTimeMillis();
				selector.select(1000);
//				System.out.println(millis() + " selector.select " + (System.currentTimeMillis() - m));
				for (var kk = selector.selectedKeys().iterator(); kk.hasNext();) {
					var k = kk.next();
//					System.out.println("k=" + k);
					kk.remove();
					if (!k.isValid())
						continue;
					if (k.isAcceptable()) {
						var sc = ((ServerSocketChannel) k.channel()).accept();
						sc.configureBlocking(false);
						var k2 = sc.register(selector, SelectionKey.OP_READ);
						var c = protocol.buildConnection(sc);
						System.out.println("serve, c=" + c.number());
						k2.attach(c);
						Thread.startVirtualThread(() -> handle(c));
//						new Thread(() -> handle(c)).start();
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
//				System.out.println(millis() + " selector.selectNow");
				selector.selectNow();
				for (var c : cc)
					synchronized (c) {
						c.r++;
						if (c.r > 20)
							throw new RuntimeException();
						System.out.println("serve, c=" + c.number() + ", c.r=" + c.r + ", notify");
						c.notify();
					}
				cc.clear();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected void handle(Connection connection) {
		var bb1 = ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY);
		var bb2 = ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY);
		for (;;) {
			System.out.println("handle, connection=" + connection.number() + ", connection.r=" + connection.r);
			synchronized (connection) {
				if (connection.r < 0) {
					connection.r = 0;
					connection.notify();
				}
				while (connection.r == 0)
					try {
						System.out.println("handle, connection=" + connection.number() + ", wait");
						connection.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				connection.r--;
			}
			try {
				{
					var n = connection.applicationChannel().read(bb1);
					connection.socketChannel().register(selector, SelectionKey.OP_READ).attach(connection);
					selector.wakeup();
					System.out.println("handle, connection=" + connection.number() + ", n=" + n);
				}
				bb1.flip();
				protocol.handle(connection, bb1, bb2);
				bb1.compact();
				bb2.flip();
				connection.applicationChannel().write(bb2);
				bb2.compact();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("handle, connection=" + connection.number());
				throw new RuntimeException(e);
			}
		}
	}

	long start = System.currentTimeMillis();

	long millis() {
		return System.currentTimeMillis() - start;
	}
}

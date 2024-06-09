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
package com.janilla.smtp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public class SmtpServer implements Runnable {

	protected int port;

	protected SSLContext sslContext;

	protected Handler handler;

	protected volatile ServerSocketChannel channel;

	protected volatile Selector selector;

	protected volatile boolean stop;

	public void setPort(int port) {
		this.port = port;
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	public InetSocketAddress getAddress() {
		try {
			return channel != null ? (InetSocketAddress) channel.getLocalAddress() : null;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void run() {
		try {
			channel = ServerSocketChannel.open();
			channel.configureBlocking(true);
			channel.socket().bind(new InetSocketAddress(port));

			channel.configureBlocking(false);
			selector = SelectorProvider.provider().openSelector();
			channel.register(selector, SelectionKey.OP_ACCEPT);

			synchronized (this) {
				notifyAll();
			}

			for (;;) {
				selector.select(1000);

				if (stop)
					break;

				var ccb = Stream.<SmtpConnection>builder();
				var n = 0;
				for (var i = selector.selectedKeys().iterator(); i.hasNext();) {
					var k = i.next();

//					System.out.println("k=" + k);

					i.remove();

					if (!k.isValid())
						continue;

					if (k.isAcceptable()) {
						var sc = ((ServerSocketChannel) k.channel()).accept();
						sc.configureBlocking(false);
						var e = sslContext.createSSLEngine();
						e.setUseClientMode(false);
						var c = handler.buildConnection(sc, e);
						c.setRequestType(SmtpRequest.Type.EMPTY);
						sc.register(selector, SelectionKey.OP_WRITE).attach(c);
					}

					if (k.isReadable() || k.isWritable()) {
						var c = (SmtpConnection) k.attachment();
						k.cancel();
						ccb.add(c);
						n++;
					}
				}

				if (n == 0)
					continue;

//				System.out.println("SmtpServer.serve n=" + n + " " + LocalTime.now());
//				var m = System.currentTimeMillis();

				selector.selectNow();

				for (var cc = ccb.build().iterator(); cc.hasNext();) {
					var c = cc.next();
					handle(c);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void stop() {
		stop = true;
		selector.wakeup();
	}

	protected void handle(SmtpConnection connection) {
		try {
			boolean k;
			try (var rq = connection.newRequest(); var rs = connection.newResponse()) {
				var ex = buildExchange(rq, rs, connection);
				k = handler.handle(ex);
			} catch (Exception e) {
				printStackTrace(e);
				k = false;
			}

			if (k) {
				connection.socketChannel().register(selector, SelectionKey.OP_READ).attach(connection);
				selector.wakeup();
			} else
				connection.close();
		} catch (Exception e) {
			printStackTrace(e);
		}
	}

	protected SmtpExchange buildExchange(SmtpRequest request, SmtpResponse response, SmtpConnection connection) {
		var ex = new SmtpExchange();
		ex.setRequest(request);
		ex.setResponse(response);
		ex.setConnection(connection);
		return ex;
	}

	static void printStackTrace(Exception exception) {
		exception.printStackTrace();
	}

	public interface Handler {

		SmtpConnection buildConnection(SocketChannel channel, SSLEngine engine);

		boolean handle(SmtpExchange exchange);
	}
}

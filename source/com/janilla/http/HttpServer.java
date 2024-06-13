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
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public class HttpServer implements Runnable {

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

				var ccb = Stream.<HttpConnection>builder();
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
						SSLEngine e;
						if (sslContext != null) {
							e = sslContext.createSSLEngine();
							e.setUseClientMode(false);
						} else
							e = null;
						var c = buildConnection(sc, e);
						sc.register(selector, SelectionKey.OP_WRITE).attach(c);
					}

					if (k.isReadable() || k.isWritable()) {
						var c = (HttpConnection) k.attachment();
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

	protected HttpConnection buildConnection(SocketChannel channel, SSLEngine engine) {
		return new HttpConnection.Builder().channel(channel).sslEngine(engine).build();
	}

	protected void handle(HttpConnection connection) {
		try {
			var k = false;
			try (var ex = buildExchange(connection)) {
				Exception e;
				try {
					k = handler.handle(ex);
					e = null;
				} catch (UncheckedIOException x) {
					e = x.getCause();
				} catch (Exception x) {
					e = x;
				}

				if (e != null) {
					printStackTrace(e);
					ex.setException(e);
					k = handler.handle(ex);
				}
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

	protected HttpExchange buildExchange(HttpConnection connection) {
		var rq = new HttpRequest.Default();
		rq.setRaw(connection.startRequest());
		rq.setUseInputMode(true);

		var rs = new HttpResponse.Default();
		rs.setRaw(connection.startResponse());
		rs.setUseInputMode(false);

		var ex = createExchange(rq);
		ex.setRequest(rq);
		ex.setResponse(rs);
		ex.setConnection(connection);
		return ex;
	}

	protected HttpExchange createExchange(HttpRequest request) {
		return new HttpExchange();
	}

	static void printStackTrace(Exception exception) {
		exception.printStackTrace();
	}

	public interface Handler {

//		HttpConnection buildConnection(SocketChannel channel, SSLEngine engine);

		boolean handle(HttpExchange exchange);
	}
}

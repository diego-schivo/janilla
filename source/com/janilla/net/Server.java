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
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

import javax.net.ssl.SSLContext;

public class Server {

	private SocketAddress address;

	private SSLContext sslContext;

	private Protocol protocol;

	private Handler handler;

	protected volatile Selector selector;

	public SocketAddress getAddress() {
		return address;
	}

	public void setAddress(SocketAddress address) {
		this.address = address;
	}

	public SSLContext getSslContext() {
		return sslContext;
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public Handler getHandler() {
		return handler;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	public void serve() {
		try {
			var ssc = ServerSocketChannel.open();
			ssc.configureBlocking(true);
			ssc.socket().bind(address);
			ssc.configureBlocking(false);
			selector = SelectorProvider.provider().openSelector();
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			for (;;) {
				selector.select(1000);
				var ccb = java.util.stream.Stream.<Connection>builder();
				var n = 0;
				for (var kk = selector.selectedKeys().iterator(); kk.hasNext();) {
					var k = kk.next();
					kk.remove();
					if (!k.isValid())
						continue;
					if (k.isAcceptable()) {
						var sc = ((ServerSocketChannel) k.channel()).accept();
						sc.configureBlocking(false);
						var se = sslContext.createSSLEngine();
						se.setUseClientMode(false);
						var pp = se.getSSLParameters();
						pp.setApplicationProtocols(new String[] { protocol.name() });
						se.setSSLParameters(pp);
						sc.register(selector, SelectionKey.OP_READ).attach(protocol.buildConnection(sc, se));
					}
					if (k.isReadable()) {
						k.cancel();
						var c = (Connection) k.attachment();
						ccb.add(c);
						n++;
					}
				}
				if (n == 0)
					continue;
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

	protected void handle(Connection connection) {
		try {
			try (var ex = protocol.buildExchange(connection)) {
				if (ex != null)
					handler.handle(ex);
			}
			try {
				connection.channel().register(selector, SelectionKey.OP_READ).attach(connection);
			} catch (CancelledKeyException e) {
				e.printStackTrace();
			}
			selector.wakeup();
		} catch (Exception e) {
			System.out.println("connection.getNumber()=" + connection.getNumber());
			e.printStackTrace();
		}
	}

	public interface Handler {

		boolean handle(Exchange exchange);
	}
}

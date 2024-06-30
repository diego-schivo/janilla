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
package com.janilla.http2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.janilla.io.IO;
import com.janilla.net.SSLByteChannel;

public class Http2Server {

	protected SocketAddress address;

	protected SSLContext sslContext;

	public Http2Server(InetSocketAddress address, SSLContext sslContext) {
		this.address = address;
		this.sslContext = sslContext;
	}

	public void serve() {
		try {
			var ssc = ServerSocketChannel.open();
			ssc.configureBlocking(true);
			ssc.socket().bind(address);
			ssc.configureBlocking(false);
			var s = SelectorProvider.provider().openSelector();
			ssc.register(s, SelectionKey.OP_ACCEPT);
			for (;;) {
				s.select(1000);
				for (var kk = s.selectedKeys().iterator(); kk.hasNext();) {
					var k = kk.next();
					kk.remove();
//				System.err.println("k=" + k);
					if (!k.isValid())
						continue;
					if (k.isAcceptable()) {
						var sc = ((ServerSocketChannel) k.channel()).accept();
						sc.configureBlocking(false);
						sc.register(s, SelectionKey.OP_READ).attach(sc);
					}
					if (k.isReadable()) {
						var sc = (SocketChannel) k.attachment();
						k.cancel();
						var e = sslContext.createSSLEngine();
						e.setUseClientMode(false);
						var pp = e.getSSLParameters();
						pp.setApplicationProtocols(new String[] { "h2" });
						e.setSSLParameters(pp);
						@SuppressWarnings("resource")
						var c = new SSLByteChannel(sc, e);

						var bb = new byte[Http2.CLIENT_CONNECTION_PREFACE_PREFIX.length()];
						var iccpp = IO.read(c, bb) == bb.length ? new String(bb) : null;
						System.err.println("iccpp=" + iccpp);

						var isf1 = (SettingsFrame) Frame.decode(c);
						System.err.println("isf1=" + isf1);

						var osf1 = new SettingsFrame(false, Map.of());
						System.err.println("osf1=" + osf1);
						Frame.encode(osf1, c);

						var isf2 = (SettingsFrame) Frame.decode(c);
						System.err.println("isf2=" + isf2);

						var osf2 = new SettingsFrame(true, Map.of());
						System.err.println("osf2=" + osf2);
						Frame.encode(osf2, c);

						var ihf = (HeadersFrame) Frame.decode(c);
						System.err.println("ihf=" + ihf);

						var ohf = new HeadersFrame(true, false, 1,
								Map.of(":status", "200", "content-length", "11", "content-type", "text/plain"));
						System.err.println("ohf=" + ohf);
						Frame.encode(ohf, c);

						DataFrame odf = new DataFrame(false, true, 1, "Hello World".getBytes());
						System.err.println("odf=" + odf);
						Frame.encode(odf, c);

//						try {
//							sc.register(s, SelectionKey.OP_READ).attach(sc);
//						} catch (CancelledKeyException e2) {
//						}
//						s.wakeup();
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

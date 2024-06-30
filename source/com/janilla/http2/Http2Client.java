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
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.janilla.io.IO;
import com.janilla.net.Net;
import com.janilla.net.SSLByteChannel;

public class Http2Client {

	public static void main(String[] args) {
		System.out.println("main begin");
		try {
			var s = Thread.ofVirtual().start(Http2Client::server);
			Thread.sleep(1000);
			var c = Thread.ofVirtual().start(Http2Client::client);
			c.join();
			s.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("main end");
	}

	static void server() {
		System.out.println("server begin");
		try {
//			Undertow server = Undertow.builder().setServerOption(UndertowOptions.ENABLE_HTTP2, true)
//					.addHttpsListener(8443, "localhost", sslContext()).setHandler(new HttpHandler() {
//
//						@Override
//						public void handleRequest(HttpServerExchange exchange) throws Exception {
//							exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
//							exchange.getResponseSender().send("Hello World");
//						}
//					}).build();
//			server.start();
//			Thread.sleep(2000);
			var hs = new Http2Server(new InetSocketAddress(8443), sslContext());
			hs.serve();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("server end");
	}

	static void client() {
		System.out.println("client begin");
		try {
			var hc = new Http2Client(new InetSocketAddress(8443), sslContext());
			try (var rq = new Http2Request("GET", URI.create("https://localhost:8443/foo")); var rs = hc.send(rq);) {
				System.out.println(new String(IO.readAllBytes(rs.body())));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("client end");
	}

	static SSLContext sslContext() {
		var k = Path.of(System.getProperty("user.home")).resolve("Downloads/jssesamples/samples/sslengine/testkeys2");
		var p = "passphrase".toCharArray();
		return Net.getSSLContext(k, p);
	}

	protected SocketAddress address;

	protected SSLContext sslContext;

	protected SSLByteChannel channel;

	public Http2Client(InetSocketAddress address, SSLContext sslContext) {
		this.address = address;
		this.sslContext = sslContext;
	}

	public Http2Response send(Http2Request request) {
		try {
			if (channel == null) {
				var sc = SocketChannel.open();
				sc.configureBlocking(true);
				sc.connect(address);
				sc.configureBlocking(false);

				var e = sslContext.createSSLEngine();
				e.setUseClientMode(true);
				var pp = e.getSSLParameters();
				pp.setApplicationProtocols(new String[] { "h2" });
				e.setSSLParameters(pp);

				channel = new SSLByteChannel(sc, e);
				var occpf = Http2.CLIENT_CONNECTION_PREFACE_PREFIX;
				System.out.println("occpf=" + occpf);
				IO.write(occpf.getBytes(), channel);

				SettingsFrame osf1 = new SettingsFrame(false, Map.of());
				System.out.println("osf1=" + osf1);
				Frame.encode(osf1, channel);

				var isf1 = (SettingsFrame) Frame.decode(channel);
				System.out.println("isf1=" + isf1);

				SettingsFrame osf2 = new SettingsFrame(true, Map.of());
				System.out.println("osf2=" + osf2);
				Frame.encode(osf2, channel);

				var isf2 = (SettingsFrame) Frame.decode(channel);
				System.out.println("isf2=" + isf2);
			}

			var ohf = new HeadersFrame(true, true, 1,
					Map.of(":authority", request.uri().getAuthority(), ":method", request.method(), ":path",
							request.uri().getPath(), ":scheme", request.uri().getScheme(), "user-agent",
							"Janilla-http-client/2.0.0"));
			System.out.println("ohf=" + ohf);
			Frame.encode(ohf, channel);

			var ihf = (HeadersFrame) Frame.decode(channel);
			System.err.println("ihf=" + ihf);

			var odf = (DataFrame) Frame.decode(channel);
			System.out.println("odf=" + odf);
			return new Http2Response(IO.toReadableByteChannel(ByteBuffer.wrap(odf.data())));

//			var bb = ByteBuffer.allocate(2000);
//			while (channel.read(bb) == 0)
//				;
//			System.out.println(Util.toHexString(Util.toIntStream(Arrays.copyOf(bb.array(), bb.position()))));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

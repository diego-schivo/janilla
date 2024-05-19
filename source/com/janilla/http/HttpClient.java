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

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpRequest.Method;
import com.janilla.http.HttpResponse.Status;
import com.janilla.io.IO;

public class HttpClient implements Closeable {

	public static void main(String[] args) throws IOException {
		try (var c = new HttpClient()) {
			c.setAddress(new InetSocketAddress("www.google.com", 443));
			try {
				var x = SSLContext.getInstance("TLSv1.3");
				x.init(null, null, null);
				c.setSSLContext(x);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
			var u = c.query(d -> {
				var q = d.getRequest();
				q.setMethod(new Method("GET"));
				q.setURI(URI.create("/"));
				q.getHeaders().add("Connection", "close");
				q.close();

				var s = d.getResponse();
				var t = s.getStatus();
				System.out.println(t);
				assert t.equals(new Status(200, "OK")) : t;

				return new String(Channels.newInputStream((ReadableByteChannel) s.getBody()).readAllBytes());
			});
			System.out.println(u);
//			assert u.equals("foo") : u;
		}
	}

	private InetSocketAddress address;

	private SSLContext sslContext;

	private HttpConnection connection;

	public InetSocketAddress getAddress() {
		return address;
	}

	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}

	public SSLContext getSSLContext() {
		return sslContext;
	}

	public void setSSLContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public <T> T query(IO.Function<HttpExchange, T> handler) throws IOException {
		if (connection == null) {
			var c = SocketChannel.open();
			c.configureBlocking(true);
			c.connect(address);
			var e = sslContext != null ? sslContext.createSSLEngine(address.getHostName(), address.getPort()) : null;
			if (e != null) {
				e.setUseClientMode(true);
				var pp = e.getSSLParameters();
				pp.setApplicationProtocols(new String[] { "http/1.1" });
				e.setSSLParameters(pp);
			}
			connection = new HttpConnection(c, e);
		}

		try (var r = connection.getOutput().writeRequest(); var s = connection.getInput().readResponse()) {
			return handler.apply(newExchange(r, s));
		}
	}

	@Override
	public void close() throws IOException {
		if (connection != null)
			connection.close();
	}

	protected HttpExchange newExchange(HttpRequest request, HttpResponse response) {
		var c = new HttpExchange();
		c.setRequest(request);
		c.setResponse(response);
		return c;
	}
}

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
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpRequest.Method;
import com.janilla.http.HttpResponse.Status;
import com.janilla.io.IO;

public class HttpClient implements Closeable {

	public static void main(String[] args) throws IOException {
		var s = new HttpServer();
		s.setHandler(c -> {
			var t = c.getRequest().getMethod().name() + " " + c.getRequest().getURI();
			switch (t) {
			case "GET /foo":
				c.getResponse().setStatus(new Status(200, "OK"));
				var b = (WritableByteChannel) c.getResponse().getBody();
				Channels.newOutputStream(b).write("bar".getBytes());
				break;
			default:
				c.getResponse().setStatus(new Status(404, "Not Found"));
				break;
			}
		});
		new Thread(() -> {
			try {
				s.run();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}, "Server").start();

		synchronized (s) {
			while (s.getAddress() == null) {
				try {
					s.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}

		try (var c = new HttpClient()) {
			c.setAddress(s.getAddress());

			c.query(d -> {
				d.getRequest().setMethod(new Method("GET"));
				d.getRequest().setURI(URI.create("/foo"));
				d.getRequest().getHeaders().add("Connection", "keep-alive");
				d.getRequest().close();

				var t = d.getResponse().getStatus();
				System.out.println(t);
				assert t.equals(new Status(200, "OK")) : t;

				var u = new String(
						Channels.newInputStream((ReadableByteChannel) d.getResponse().getBody()).readAllBytes());
				System.out.println(u);
				assert u.equals("bar") : u;
			});

			c.query(d -> {
				d.getRequest().setMethod(new Method("GET"));
				d.getRequest().setURI(URI.create("/baz"));
				d.getRequest().getHeaders().add("Connection", "close");
				d.getRequest().close();

				var t = d.getResponse().getStatus();
				System.out.println(t);
				assert t.equals(new Status(404, "Not Found")) : t;

				var u = new String(
						Channels.newInputStream((ReadableByteChannel) d.getResponse().getBody()).readAllBytes());
				System.out.println(u);
				assert u.equals("") : u;
			});
		} finally {
			s.stop();
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

	public void query(IO.Consumer<HttpExchange> handler) throws IOException {
		if (connection == null) {
			var c = SocketChannel.open();
			c.configureBlocking(true);
			c.connect(address);
			var e = sslContext != null ? sslContext.createSSLEngine("client", address.getPort()) : null;
			if (e != null)
				e.setUseClientMode(true);
			connection = new HttpConnection(c, e);
		}

		try (var r = connection.getOutput().writeRequest(); var s = connection.getInput().readResponse()) {
			try {
				handler.accept(newExchange(r, s));
			} catch (UncheckedIOException e) {
				throw e.getCause();
			}
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

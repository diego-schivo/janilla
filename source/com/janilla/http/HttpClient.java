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
//package com.janilla.http;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.io.UncheckedIOException;
//import java.net.InetSocketAddress;
//import java.net.URI;
//import java.nio.channels.SocketChannel;
//import java.nio.file.Path;
//import java.util.List;
//import java.util.function.Function;
//
//import javax.net.ssl.SSLContext;
//
//import com.janilla.net.Net;
//
//public class HttpClient implements AutoCloseable {
//
//	public static void main(String[] args) {
//		var s = new HttpServer();
//		{
//			var k = Path.of(System.getProperty("user.home"))
//					.resolve("Downloads/jssesamples/samples/sslengine/testkeys");
//			var p = "passphrase".toCharArray();
//			var x = Net.getSSLContext(k, p);
//			s.setSslContext(x);
//		}
//		s.setHandler(exchange -> {
//			var rq = exchange.getRequest();
//			if (rq.getUri() == null)
//				return false;
//			String t;
//			try {
//				t = new String(((InputStream) rq.getBody()).readAllBytes());
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
//			System.out.println(t);
//			assert t.equals("name=Joe%20User&request=Send%20me%20one%20of%20your%20catalogue") : t;
//			var rs = exchange.getResponse();
//			rs.setStatus(HttpResponse.Status.of(200));
//			rs.setHeaders(List.of(new HttpHeader("Content-Type", "text/plain"),
//					new HttpHeader("Transfer-Encoding", "chunked")));
//			try {
//				((OutputStream) rs.getBody()).write("""
//						7\r
//						Mozilla\r
//						11\r
//						Developer Network\r
//						0\r
//						\r
//						""".getBytes());
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
//			return true;
//		});
//		new Thread(s::run, "Server").start();
//
//		synchronized (s) {
//			while (s.getAddress() == null) {
//				try {
//					s.wait();
//				} catch (InterruptedException e) {
//					throw new RuntimeException(e);
//				}
//			}
//		}
//
//		try (var c = new HttpClient()) {
//			c.setAddress(s.getAddress());
//			{
//				var k = Path.of(System.getProperty("user.home"))
//						.resolve("Downloads/jssesamples/samples/sslengine/testkeys");
//				var p = "passphrase".toCharArray();
//				var x = Net.getSSLContext(k, p);
//				c.setSslContext(x);
//			}
//			try {
//				var u = c.query(ex -> {
//					try (var rq = (HttpRequest) ex.getRequest()) {
//						rq.setMethod(new HttpRequest.Method("POST"));
//						rq.setUri(URI.create("/contact_form.php"));
//						rq.setHeaders(List.of(new HttpHeader("Host", "developer.mozilla.org"),
//								new HttpHeader("Content-Length", "63"),
//								new HttpHeader("Content-Type", "application/x-www-form-urlencoded")));
//						try {
//							((OutputStream) rq.getBody()).write(
//									"name=Joe%20User&request=Send%20me%20one%20of%20your%20catalogue".getBytes());
//						} catch (IOException e) {
//							throw new UncheckedIOException(e);
//						}
//					}
//					try (var rs = (HttpResponse) ex.getResponse()) {
//						try {
//							return new String(((InputStream) rs.getBody()).readAllBytes());
//						} catch (IOException e) {
//							throw new UncheckedIOException(e);
//						}
//					}
//				});
//				System.out.println(u);
//				assert u.equals("""
//						7\r
//						Mozilla\r
//						11\r
//						Developer Network\r
//						0\r
//						\r
//						""") : u;
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		} finally {
//			s.stop();
//		}
//	}
//
//	private InetSocketAddress address;
//
//	private SSLContext sslContext;
//
//	protected HttpConnection connection;
//
//	public void setAddress(InetSocketAddress address) {
//		this.address = address;
//	}
//
//	public void setSslContext(SSLContext sslContext) {
//		this.sslContext = sslContext;
//	}
//
//	public HttpConnection connection() {
//		return connection;
//	}
//
//	public <T> T query(Function<HttpExchange, T> handler) {
//		if (connection == null) {
//			try {
//				var sc = SocketChannel.open();
//				sc.configureBlocking(true);
//				sc.connect(address);
//				var e = sslContext.createSSLEngine(address.getHostName(), address.getPort());
//				e.setUseClientMode(true);
//				connection = new HttpConnection.Builder().channel(sc).sslEngine(e).build();
//				connection.setUseClientMode(true);
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
//		}
//		try (var ex = buildExchange(connection)) {
//			return handler.apply(ex);
//		}
//	}
//
//	@Override
//	public void close() {
//		if (connection != null)
//			connection.close();
//	}
//
//	protected HttpExchange buildExchange(HttpConnection connection) {
//		var rq = new HttpRequest.Default();
//		rq.setRaw(connection.startRequest());
//		rq.setUseInputMode(false);
//
//		var rs = new HttpResponse.Default();
//		rs.setRaw(connection.startResponse());
//		rs.setUseInputMode(true);
//
//		var ex = new HttpExchange();
//		ex.setRequest(rq);
//		ex.setResponse(rs);
//		ex.setConnection(connection);
//		return ex;
//	}
//}

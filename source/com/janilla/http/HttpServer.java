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
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpRequest.Method;
import com.janilla.http.HttpResponse.Status;
import com.janilla.io.IO;
import com.janilla.net.Net;
import com.janilla.web.HandleException;
import com.janilla.web.NotFoundException;
import com.janilla.web.UnauthenticatedException;
import com.janilla.web.WebHandler;

public class HttpServer implements Runnable {

	public static void main(String[] args) throws Exception {
		var s = new HttpServer();
		s.setBufferCapacity(100);
		s.setMaxMessageLength(1000);
		{
			var k = Path.of(System.getProperty("user.home"))
					.resolve("Downloads/jssesamples/samples/sslengine/testkeys");
			var p = "passphrase".toCharArray();
			var x = Net.getSSLContext(k, p);
			s.setSSLContext(x);
		}
		s.setHandler(c -> {
			var t = c.getRequest().getMethod().name() + " " + c.getRequest().getURI();
			switch (t) {
			case "POST /foo":
				c.getResponse().setStatus(new Status(200, "OK"));
				var b = (WritableByteChannel) c.getResponse().getBody();
				try {
					Channels.newOutputStream(b).write("bar".getBytes());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				break;
			default:
				c.getResponse().setStatus(new Status(404, "Not Found"));
				break;
			}
		});
		new Thread(s::run, "Server").start();

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
			try {
				var x = SSLContext.getInstance("TLSv1.2");
				x.init(null, null, null);
				c.setSSLContext(x);
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
			var u = c.query(d -> {
				try (var q = d.getRequest()) {
					q.setMethod(new Method("POST"));
					q.setURI(URI.create("/foo"));
					q.getHeaders().add("Content-Length", "120");
					q.getHeaders().add("Connection", "keep-alive");
					try (var bc = (WritableByteChannel) q.getBody()) {
						for (var i = 0; i < 10; i++)
							IO.write("foobarbazqux".getBytes(), bc);
					}
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}

				try (var z = d.getResponse()) {
					var t = z.getStatus();
					System.out.println(t);
					assert t.equals(new Status(200, "OK")) : t;

					return new String(IO.readAllBytes((ReadableByteChannel) z.getBody()));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			System.out.println(u);
			assert u.equals("bar") : u;

			u = c.query(d -> {
				try (var q = d.getRequest()) {
					q.setMethod(new Method("GET"));
					q.setURI(URI.create("/baz"));
					q.getHeaders().add("Connection", "close");
				}

				try (var z = d.getResponse()) {
					var t = z.getStatus();
					System.out.println(t);
					assert t.equals(new Status(404, "Not Found")) : t;

					return new String(IO.readAllBytes((ReadableByteChannel) z.getBody()));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			System.out.println(u);
			assert u.equals("") : u;
		} finally {
			s.stop();
		}
	}

	private int bufferCapacity = IO.DEFAULT_BUFFER_CAPACITY;

	private Executor executor;

	private WebHandler handler;

	private long idleTimerPeriod;

	private long maxIdleDuration;

	private long maxMessageLength = -1;

	private int port;

	private SSLContext sslContext;

	protected volatile InetSocketAddress address;

	protected volatile Selector selector;

	protected volatile boolean stop;

	public int getBufferCapacity() {
		return bufferCapacity;
	}

	public void setBufferCapacity(int bufferCapacity) {
		this.bufferCapacity = bufferCapacity;
	}

	public Executor getExecutor() {
		return executor;
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	public WebHandler getHandler() {
		return handler;
	}

	public void setHandler(WebHandler handler) {
		this.handler = handler;
	}

	public long getIdleTimerPeriod() {
		return idleTimerPeriod;
	}

	public void setIdleTimerPeriod(long idleTimerPeriod) {
		this.idleTimerPeriod = idleTimerPeriod;
	}

	public long getMaxIdleDuration() {
		return maxIdleDuration;
	}

	public void setMaxIdleDuration(long maxIdleDuration) {
		this.maxIdleDuration = maxIdleDuration;
	}

	public long getMaxMessageLength() {
		return maxMessageLength;
	}

	public void setMaxMessageLength(long maxMessageLength) {
		this.maxMessageLength = maxMessageLength;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public SSLContext getSSLContext() {
		return sslContext;
	}

	public void setSSLContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	@Override
	public void run() {
		try {
			var c = ServerSocketChannel.open();
			c.configureBlocking(true);
			c.socket().bind(new InetSocketAddress(port));
			address = (InetSocketAddress) c.getLocalAddress();

			c.configureBlocking(false);
			selector = SelectorProvider.provider().openSelector();
			c.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		var r = new HashMap<HttpConnection, Long>();
		var t = new Timer(Thread.currentThread().getName() + "-IdleTimer", true);
		{
			var p = idleTimerPeriod > 0 ? idleTimerPeriod : 10000;
			var d = maxIdleDuration > 0 ? maxIdleDuration : 30000;
			t.schedule(new TimerTask() {

				@Override
				public void run() {
					var t = System.currentTimeMillis() - d;
					var s = new HashSet<HttpConnection>();
					synchronized (r) {
						for (var i = r.entrySet().iterator(); i.hasNext();) {
							var e = i.next();
							if (e.getValue() <= t) {
								s.add(e.getKey());
								i.remove();
							}
						}
					}
					if (!s.isEmpty()) {

//						System.out.println(Thread.currentThread().getName() + " IdleTimer s " + s.size());

						for (var c : s)
							c.close();
					}
				}
			}, p, p);
		}

		synchronized (this) {
			notifyAll();
		}

		try {
			for (;;) {
				selector.select(1000);

				if (stop)
					break;

				var l = Stream.<HttpConnection>builder();
				var n = 0;
				for (var i = selector.selectedKeys().iterator(); i.hasNext();) {
					var k = i.next();
					i.remove();

					if (!k.isValid())
						continue;

					if (k.isAcceptable()) {
						var d = ((ServerSocketChannel) k.channel()).accept();
						d.configureBlocking(false);
						var e = sslContext != null ? sslContext.createSSLEngine() : null;
						if (e != null) {
							e.setUseClientMode(false);
							var pp = e.getSSLParameters();
							pp.setApplicationProtocols(new String[] { "http/1.1" });
							e.setSSLParameters(pp);
						}
						var c = new HttpConnection(d, e, bufferCapacity, maxMessageLength);
						synchronized (r) {
							r.put(c, System.currentTimeMillis());
						}
						d.register(selector, SelectionKey.OP_READ).attach(c);
					}

					if (k.isReadable()) {
						var e = (HttpConnection) k.attachment();
						k.cancel();
						l.add(e);
						n++;
					}
				}

				if (n == 0)
					continue;

//				System.out.println("HttpServer.serve n=" + n + " " + LocalTime.now());
//				var m = System.currentTimeMillis();

				selector.selectNow();

				for (var i = l.build().iterator(); i.hasNext();) {
					var c = i.next();
					if (executor != null)
						executor.execute(() -> handle(handler, c, r));
					else
						handle(handler, c, r);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			if (executor instanceof ExecutorService s)
				s.shutdownNow();
			t.cancel();
		}
	}

	public void stop() {
		stop = true;
		selector.wakeup();
	}

	protected void handle(WebHandler handler, HttpConnection connection, Map<HttpConnection, Long> connectionMillis) {
		try {
			String d;
			try (var q = connection.getInput().readRequest(); var s = connection.getOutput().writeResponse()) {

//				System.out.println(Thread.currentThread().getName() + " " + q.getURI());

				handle(handler, q, s);
				var h = q.getHeaders();
				d = Objects.toString(h != null ? h.get("Connection") : null, "close");
			} catch (Exception e) {
				printStackTrace(e);
				d = "close";
			}

			switch (d) {
			case "keep-alive":
				synchronized (connectionMillis) {
					connectionMillis.put(connection, System.currentTimeMillis());
				}
				connection.getChannel().register(selector, SelectionKey.OP_READ).attach(connection);
				selector.wakeup();
				break;
			case "close":
				synchronized (connectionMillis) {
					connectionMillis.remove(connection);
				}
				connection.close();
				break;
			}
		} catch (IOException e) {
			printStackTrace(e);
		}
	}

	protected void handle(WebHandler handler, HttpRequest request, HttpResponse response) {
		var c = buildExchange(request, response);
		Exception e;
		try {
			handler.handle(c);
			e = null;
		} catch (UncheckedIOException x) {
			e = x.getCause();
		} catch (HandleException x) {
			e = x.getCause();
		} catch (Exception x) {
			e = x;
		}
		if (e != null) {
			printStackTrace(e);
			c.setException(e);
			handler.handle(c);
		}
	}

	protected HttpExchange buildExchange(HttpRequest request, HttpResponse response) {
		var e = new HttpExchange();
		e.setRequest(request);
		e.setResponse(response);
		return e;
	}

	static void printStackTrace(Exception exception) {
		switch (exception) {
		case NotFoundException x:
			break;
		case UnauthenticatedException x:
			break;
		case ClosedChannelException x:
			break;
		default:
			if (exception.getMessage() != null)
				switch (exception.getMessage()) {
				case "Broken pipe":
				case "no bytes written":
				case "startLine":
				case "Stream closed":
				case "The requested action is disabled on this public server: please set up and run the application locally":
					break;
				default:
					exception.printStackTrace();
					break;
				}
		}
	}
}

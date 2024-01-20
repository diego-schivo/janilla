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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.file.Path;
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

public class HttpServer implements IO.Runnable {

	public static void main(String[] args) throws IOException {
		var k = Path.of(System.getProperty("user.home")).resolve("Downloads/jssesamples/samples/sslengine/testkeys");
		var p = "passphrase".toCharArray();
		var x = Net.getSSLContext(k, p);

		var s = new HttpServer();
		s.setBufferCapacity(100);
		s.setMaxMessageLength(1000);
		s.setSSLContext(x);
		s.setHandler(c -> {
			var t = c.getRequest().getMethod().name() + " " + c.getRequest().getURI();
			switch (t) {
			case "POST /foo":
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
			c.setSSLContext(x);
			c.query(d -> {
				d.getRequest().setMethod(new Method("POST"));
				d.getRequest().setURI(URI.create("/foo"));
				d.getRequest().getHeaders().add("Content-Length", "120");
				d.getRequest().getHeaders().add("Connection", "keep-alive");
				try (var bc = (WritableByteChannel) d.getRequest().getBody()) {
					for (var i = 0; i < 10; i++)
						IO.write("foobarbazqux".getBytes(), bc);
				}
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

	private int bufferCapacity = IO.DEFAULT_BUFFER_CAPACITY;

	private Executor executor;

	private IO.Consumer<ExchangeContext> handler;

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

	public IO.Consumer<ExchangeContext> getHandler() {
		return handler;
	}

	public void setHandler(IO.Consumer<ExchangeContext> handler) {
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
	public void run() throws IOException {
		{
			var c = ServerSocketChannel.open();
			c.configureBlocking(true);
			c.socket().bind(new InetSocketAddress(port));
			address = (InetSocketAddress) c.getLocalAddress();

			c.configureBlocking(false);
			selector = SelectorProvider.provider().openSelector();
			c.register(selector, SelectionKey.OP_ACCEPT);
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
							try {
								c.close();
							} catch (IOException x) {
							}
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
						if (e != null)
							e.setUseClientMode(false);
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

	protected void handle(IO.Consumer<ExchangeContext> handler, HttpConnection connection,
			Map<HttpConnection, Long> connectionMillis) {
//		System.out.println(
//		Thread.currentThread().getName() + " (" + (System.currentTimeMillis() - m) + ")");

		try {
			String d;
			try (var q = connection.getInput().readRequest(); var s = connection.getOutput().writeResponse()) {

//		System.out.println(Thread.currentThread().getName() + " " + q.getURI() + " ("
//				+ (System.currentTimeMillis() - m) + ")");
				handle(handler, q, s);
				var h = q.getHeaders();
				d = Objects.toString(h != null ? h.get("Connection") : null, "close");
//		System.out.println(Thread.currentThread().getName() + " " + d + " ("
//				+ (System.currentTimeMillis() - m) + ")");
			} catch (Exception e) {
				e.printStackTrace();
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
			e.printStackTrace();
		}

//System.out.println(Thread.currentThread().getName() + " (" + (System.currentTimeMillis() - m)
//		+ ")" + " " + LocalTime.now());
	}

	protected void handle(IO.Consumer<ExchangeContext> handler, HttpRequest request, HttpResponse response)
			throws IOException {
		var c = newExchangeContext(request);
		c.setRequest(request);
		c.setResponse(response);
		Exception e;
		try {
			handler.accept(c);
			e = null;
		} catch (UncheckedIOException x) {
			e = x.getCause();
		} catch (HandleException x) {
			e = x.getCause();
		} catch (Exception x) {
			e = x;
		}
		if (e != null) {
			e.printStackTrace();
			c.setException(e);
			handler.accept(c);
		}
	}

	protected ExchangeContext newExchangeContext(HttpRequest request) {
		return new ExchangeContext();
	}
}

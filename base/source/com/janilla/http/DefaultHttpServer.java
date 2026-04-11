/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.janilla.ioc.DiFactory;
import com.janilla.net.AbstractServer;
import com.janilla.net.FilterTransfer;
import com.janilla.net.SecureTransfer;
import com.janilla.net.Transfer;
import com.janilla.web.HandleException;
import com.janilla.web.NotFoundException;

public class DefaultHttpServer extends AbstractServer implements HttpServer {

	protected final HttpHandler handler;

	protected final DiFactory diFactory;

	public DefaultHttpServer(SocketAddress endpoint, SSLContext sslContext, HttpHandler handler) {
		this(endpoint, sslContext, handler, null);
	}

	public DefaultHttpServer(SocketAddress endpoint, SSLContext sslContext, HttpHandler handler, DiFactory diFactory) {
		super(endpoint, sslContext);
		this.handler = handler;
		this.diFactory = diFactory;
	}

	@Override
	protected String[] applicationProtocols() {
		return new String[] { "h2", "http/1.1" };
	}

	@Override
	protected void handleConnection(Transfer transfer) throws IOException {
//		IO.println("HttpServer.handleConnection");

		var t = transfer instanceof FilterTransfer x ? x.transfer() : transfer;
		if (t instanceof SecureTransfer st) {
			do
				if (st.read() == -1)
					return;
			while (st.in().position() < 16);
//		IO.println("bb=" + new String(st.in().array(), 0, 16));

			var p = st.engine().getApplicationProtocol();
//		IO.println("p=" + p);
			if (p.equals("h2"))
				handleConnection2(t);
		}

		handleConnection1(t);
	}

	protected void handleConnection1(Transfer transfer) throws IOException {
//		IO.println("HttpServer.handleConnection1");
		for (;;) {
//			IO.println("st.in().position()=" + st.in().position());
			var ll = new ArrayList<String>();
			for (;;) {
				int i = 0, b1 = -1, b2;
				for (;; i++, b1 = b2) {
					if (i == transfer.in().position()) {
						var n = transfer.read();
//						IO.println("n=" + n);
						if (n == -1)
							return;
					}
					b2 = transfer.in().get(i);
					if (b1 == '\r' && b2 == '\n')
						break;
				}
//				IO.println("i=" + i);
				transfer.in().flip();
				var bb = new byte[i + 1];
				transfer.in().get(bb);
				transfer.in().compact();
				var l = new String(bb, 0, i - 1);
//				IO.println("s=" + s);
				if (!l.isEmpty())
					ll.add(l);
				else
					break;
			}

			handleEndHeaders1(ll);

			try (var rq = new HttpRequest()) {
				{
					var i = 0;
					for (var l : ll) {
						if (i == 0) {
							var ss = l.split(" ", 3);
							rq.setMethod(ss[0].trim());
							rq.setTarget(ss[1].trim());
						} else
							rq.setHeader(HeaderField.fromLine(l));
						i++;
					}
				}

				var cl = rq.getHeaderValue("Content-Length");
//				IO.println("HttpServer.handleConnection1, cl=" + cl);
				if (cl != null) {
					var bb = new byte[Integer.parseInt(cl)];
					for (var i = 0; i < bb.length;) {
						if (transfer.in().position() == 0)
							transfer.read();
						transfer.in().flip();
						var n = Math.min(transfer.in().remaining(), bb.length - i);
						transfer.in().get(bb, i, n);
						transfer.in().compact();
						i += n;
//						IO.println("HttpServer.handleConnection1, i=" + i);
					}
					rq.setBody(Channels.newChannel(new ByteArrayInputStream(bb)));
				}
				try (var rs = new HttpResponse()) {
					rs.setStatus(0);
					var baos = new ByteArrayOutputStream();
					rs.setBody(Channels.newChannel(baos));
					exchange(rq, rs);
					ll.clear();
					ll.add("HTTP/1.1 " + rs.getStatus() + " OK");
					for (var h : rs.getHeaders())
						if (!h.name().startsWith(":"))
							ll.add(h.toLine());
					ll.add("");
					for (var l : ll) {
						transfer.out().put((l + "\r\n").getBytes());
						transfer.out().flip();
						do
							transfer.write();
						while (transfer.out().hasRemaining());
						transfer.out().clear();
					}
					var bb = baos.toByteArray();
					for (var i = 0; i < bb.length;) {
						var n = Math.min(transfer.out().remaining(), bb.length - i);
						transfer.out().put(bb, i, n);
						transfer.out().flip();
						do
							transfer.write();
						while (transfer.out().hasRemaining());
						transfer.out().clear();
						i += n;
					}
				}
			}
		}
	}

	protected void handleEndHeaders1(List<String> lines) {
	}

	protected void handleConnection2(Transfer transfer) throws IOException {
//		IO.println("HttpServer.handleConnection2");
		var st = (SecureTransfer) transfer;
		while (st.in().position() < 24)
			if (st.read() == -1)
				return;
		st.in().flip();
		var cp = new byte[24];
		st.in().get(cp);
//		IO.println("cp=" + new String(cp));
		class A {
			private static final byte[] CONNECTION_PREFACE_PREFIX = """
					PRI * HTTP/2.0\r
					\r
					SM\r
					\r
					""".getBytes();
		}
		if (!Arrays.equals(cp, A.CONNECTION_PREFACE_PREFIX))
			throw new RuntimeException();

		var ft = new FrameTransfer(st);
		ft.writeFrame(new SettingsFrame(false, List.of(new SettingParameter(SettingName.MAX_CONCURRENT_STREAMS, 100),
				new SettingParameter(SettingName.ENABLE_PUSH, 0))));

		var streams = new HashMap<Integer, List<Frame>>();
		for (;;) {
			var f = ft.readFrame();
			if (f == null)
				break;

//			IO.println("HttpServer.handleConnection2, f=" + f);
			switch (f) {
			case DataFrame _:
			case HeadersFrame _:
				var ff = streams.computeIfAbsent(f.streamIdentifier(), _ -> new ArrayList<>());
				ff.add(f);

				var df = f instanceof DataFrame x ? x : null;
				var hf = f instanceof HeadersFrame x ? x : null;

				if (df != null && df.data().length != 0)
					for (var id : new int[] { f.streamIdentifier(), 0 })
						ft.writeFrame(new WindowUpdateFrame(id, 9 + df.data().length));

				if (hf != null && hf.endHeaders())
					handleEndHeaders2(ff, ft);

				if (df != null ? df.endStream() : hf.endStream()) {
					streams.remove(f.streamIdentifier());
					handleEndStream(ff, ft);
				}
				break;

			case SettingsFrame x:
				if (!x.ack())
					ft.writeFrame(new SettingsFrame(true, List.of()));
				break;

			case PingFrame _:
			case PriorityFrame _:
			case WindowUpdateFrame _:
				break;

			case GoawayFrame _:
				return;

			case RstStreamFrame _:
				streams.remove(f.streamIdentifier());
				break;

			default:
				throw new RuntimeException(f.toString());
			}
		}
	}

	protected void handleEndHeaders2(List<Frame> frames, FrameTransfer transfer) {
	}

	protected void handleEndStream(List<Frame> frames, FrameTransfer transfer) {
		Thread.startVirtualThread(() -> handleStream(frames, transfer));
	}

	protected void handleStream(List<Frame> frames, FrameTransfer transfer) {
		try (var rq = new HttpRequest()) {
			var id = frames.getFirst().streamIdentifier();
			var hff = new ArrayList<HeaderField>();
			var dbb = ByteBuffer.allocate(frames.stream().filter(x -> x instanceof DataFrame)
					.mapToInt(x -> ((DataFrame) x).data().length).sum());
			for (var f : frames)
				if (f instanceof HeadersFrame x)
					hff.addAll(x.fields());
				else
					dbb.put(((DataFrame) f).data());
			rq.setHeaders(hff);
//			IO.println("HttpServer.handleStream, " + rq.getMethod() + " " + rq.getScheme() + "://" + rq.getAuthority()
//					+ rq.getTarget());
			rq.setBody(Channels.newChannel(new ByteArrayInputStream(dbb.array())));
			try (var rs = new HttpResponse()) {
				rs.setStatus(0);
				rs.setBody(new WritableByteChannel() {

					private boolean closed;

					private long written;

					@Override
					public boolean isOpen() {
						return !closed;
					}

					@Override
					public void close() throws IOException {
//						IO.println("HttpServer.handleStream, close");
						if (closed)
							return;
						transfer.writeFrame(
								written == 0 ? new HeadersFrame(false, true, true, id, false, 0, 0, rs.getHeaders())
										: new DataFrame(false, true, id, new byte[0]));
//						var bb = encoder.encodeFrame(f);
//						writeFrame(transfer, bb);
						closed = true;
					}

					@Override
					public int write(ByteBuffer src) throws IOException {
//						IO.println("HttpServer.handleStream, WritableByteChannel.write");
						if (closed)
							throw new IOException("closed");
						var w = written;
						while (src.hasRemaining()) {
							var n = Math.min(src.remaining(), 16384);
							if (written == 0) {
								transfer.writeFrame(
										new HeadersFrame(false, true, false, id, false, 0, 0, rs.getHeaders()));
//								writeFrame(transfer, encoder.encodeFrame(f));
							}
							var bb = new byte[n];
							src.get(bb);
							transfer.writeFrame(new DataFrame(false, false, id, bb));
//							writeFrame(transfer, encoder.encodeFrame(f));
							written += n;
						}
						var n = (int) (written - w);
//						IO.println("HttpServer.handleStream, WritableByteChannel.write, n=" + n);
						return n;
					}
				});
				exchange(rq, rs);
//				IO.println("HttpServer.handleStream, " + rq.getMethod() + " " + rq.getScheme() + "://"
//						+ rq.getAuthority() + rq.getTarget() + " " + rs.getStatus());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void exchange(HttpRequest request, HttpResponse response) {
		var ex = createExchange(request, response);
		ScopedValue.where(HTTP_EXCHANGE, ex).call(() -> handleExchange(ex));
	}

	@Override
	public HttpExchange createExchange(HttpRequest request, HttpResponse response) {
		var c = diFactory != null ? diFactory.classFor(HttpExchange.class) : null;
		return c != null ? diFactory.newInstance(c, Map.of("request", request, "response", response))
				: new SimpleHttpExchange(request, response);
	}

	@Override
	public boolean handleExchange(HttpExchange exchange) {
		if (exchange == null)
			throw new NullPointerException();

		var k = true;
		Exception e;
		try {
			k = handler.handle(exchange);
			e = null;
		} catch (HandleException x) {
			e = x.getCause();
		} catch (UncheckedIOException x) {
			e = x.getCause();
		} catch (RuntimeException x) {
			if (x.getCause() instanceof Error y)
				throw y;
			e = x;
		}
//		IO.println("HttpServer.handleExchange, e=" + e);
		if (e != null)
			try {
				if (e instanceof NotFoundException)
					IO.println(e.getClass().getSimpleName() + ": " + e.getMessage());
				else
					e.printStackTrace();
				exchange = exchange.withException(e);
				k = handler.handle(exchange);
			} catch (RuntimeException x) {
				if (x.getCause() instanceof Error y)
					throw y;
				x.printStackTrace();
				k = false;
			}
		return k;
	}
}

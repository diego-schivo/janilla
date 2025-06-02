/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
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
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.janilla.io.IO;
import com.janilla.net.SecureServer;
import com.janilla.net.SecureTransfer;
import com.janilla.web.HandleException;

public class HttpServer extends SecureServer {

	public static final ScopedValue<HttpExchange> HTTP_EXCHANGE = ScopedValue.newInstance();

	protected final HttpHandler handler;

	public HttpServer(SSLContext sslContext, HttpHandler handler) {
		super(sslContext);
		this.handler = handler;
	}

	@Override
	protected SSLEngine createSslEngine() {
		var se = super.createSslEngine();
		var spp = se.getSSLParameters();
		spp.setApplicationProtocols(new String[] { "h2", "http/1.1" });
		se.setSSLParameters(spp);
		return se;
	}

	@Override
	protected void handleConnection(SecureTransfer st) throws IOException {
		do
			st.read();
		while (st.in().position() < 16);
//		System.out.println("bb=" + new String(st.in().array(), 0, 16));

		var ap = st.engine().getApplicationProtocol();
//		System.out.println("ap=" + ap);
		if (ap.equals("h2"))
			handleConnection2(st);
		else
			handleConnection1(st);
	}

	protected void handleConnection1(SecureTransfer st) throws IOException {
		for (;;) {
//			System.out.println("st.in().position()=" + st.in().position());
			var sb = Stream.<String>builder();
			for (;;) {
				int i = 0, b1 = -1, b2;
				for (;; i++, b1 = b2) {
					if (i == st.in().position()) {
						var n = st.read();
						System.out.println("n=" + n);
						if (n == -1)
							return;
					}
					b2 = st.in().get(i);
					if (b1 == '\r' && b2 == '\n')
						break;
				}
//				System.out.println("i=" + i);
				st.in().flip();
				var bb = new byte[i + 1];
				st.in().get(bb);
				st.in().compact();
				var s = new String(bb, 0, i - 1);
//				System.out.println("s=" + s);
				if (!s.isEmpty())
					sb.add(s);
				else
					break;
			}
			try (var rq = new HttpRequest()) {
				{
					var i = 0;
					for (var s : sb.build().toList()) {
						if (i == 0) {
							var ss = s.split(" ", 3);
							rq.setMethod(ss[0].trim());
							rq.setTarget(ss[1].trim());
						} else {
							if (rq.getHeaders() == null)
								rq.setHeaders(new ArrayList<>());
							rq.getHeaders().add(HeaderField.fromLine(s));
						}
						i++;
					}
				}
				var cl = rq.getHeaderValue("Content-Length");
				if (cl != null) {
					var bb = new byte[Integer.parseInt(cl)];
					for (var i = 0; i < bb.length;) {
						if (st.in().position() == 0)
							st.read();
						st.in().flip();
						var n = Math.min(st.in().remaining(), bb.length - i);
						st.in().get(bb, i, n);
						st.in().compact();
						i += n;
					}
					rq.setBody(Channels.newChannel(new ByteArrayInputStream(bb)));
				}
				try (var rs = new HttpResponse()) {
					rs.setHeaders(new ArrayList<>(List.of(new HeaderField(":status", null))));
					var baos = new ByteArrayOutputStream();
					rs.setBody(Channels.newChannel(baos));
					var ex = createExchange(rq);
					ex.setRequest(rq);
					ex.setResponse(rs);
					ScopedValue.where(HTTP_EXCHANGE, ex).call(() -> handleExchange(ex));
					sb = Stream.<String>builder();
					sb.add("HTTP/1.1 " + rs.getHeaderValue(":status") + " OK");
					for (var h : rs.getHeaders())
						if (!h.name().startsWith(":"))
							sb.add(h.toLine());
					sb.add("");
					for (var s : sb.build().toList()) {
						st.out().put((s + "\r\n").getBytes());
						st.out().flip();
						do
							st.write();
						while (st.out().hasRemaining());
						st.out().clear();
					}
					var bb = baos.toByteArray();
					for (var i = 0; i < bb.length;) {
						var n = Math.min(st.out().remaining(), bb.length - i);
						st.out().put(bb, i, n);
						st.out().flip();
						do
							st.write();
						while (st.out().hasRemaining());
						st.out().clear();
						i += n;
					}
				}
			}
		}
	}

	protected void handleConnection2(SecureTransfer st) throws IOException {
		while (st.in().position() < 24)
			st.read();
		st.in().flip();
		var cp = new byte[24];
		st.in().get(cp);
//		System.out.println("cp=" + new String(cp));
		if (!Arrays.equals(cp, """
				PRI * HTTP/2.0\r
				\r
				SM\r
				\r
				""".getBytes()))
			throw new RuntimeException();

		var he = new HttpEncoder();
		var hd = new HttpDecoder();

		st.outLock().lock();
		try {
			st.out().put(he.encodeFrame(new Frame.Settings(false,
					List.of(new Setting.Parameter(Setting.Name.MAX_CONCURRENT_STREAMS, 100)))));
			st.out().flip();
			do
				st.write();
			while (st.out().hasRemaining());
		} finally {
			st.outLock().unlock();
		}

		var streams = new HashMap<Integer, List<Frame>>();
		for (;;) {
			var bb = readFrame(st);
			var f = hd.decodeFrame(bb);
//			System.out.println("f=" + f);
			switch (f) {
			case Frame.Data _:
			case Frame.Headers _:
				var ff = streams.computeIfAbsent(f.streamIdentifier(), _ -> new ArrayList<>());
				ff.add(f);
				var es = f instanceof Frame.Data x ? x.endStream() : ((Frame.Headers) f).endStream();
				if (es) {
					streams.remove(f.streamIdentifier());
					Thread.startVirtualThread(() -> handleStream(ff, st, he));
				}
				break;
			case @SuppressWarnings("unused") Frame.Settings s:
//				System.out.println("s=" + s);
				st.outLock().lock();
				try {
					st.out().clear();
					st.out().put(he.encodeFrame(new Frame.Settings(true, List.of())));
					st.out().flip();
					do
						st.write();
					while (st.out().hasRemaining());
				} finally {
					st.outLock().unlock();
				}
				break;
			case Frame.Ping _:
			case Frame.WindowUpdate _:
				break;
			default:
				throw new RuntimeException(f.toString());
			}
		}
	}

	protected byte[] readFrame(SecureTransfer st) throws IOException {
		st.inLock().lock();
		try {
			if (st.in().remaining() < 3) {
				st.in().compact();
				do
					st.read();
				while (st.in().position() < 3);
				st.in().flip();
			}
			var pl = (Short.toUnsignedInt(st.in().getShort(st.in().position())) << 8)
					| Byte.toUnsignedInt(st.in().get(st.in().position() + Short.BYTES));
			if (pl > 16384)
				throw new RuntimeException();
			var bb = new byte[9 + pl];
			if (st.in().remaining() < bb.length) {
				st.in().compact();
				do
					st.read();
				while (st.in().position() < bb.length);
				st.in().flip();
			}
			st.in().get(bb);
			return bb;
		} finally {
			st.inLock().unlock();
		}
	}

	protected void handleStream(List<Frame> ff, SecureTransfer st, HttpEncoder he) {
		try (var rq = new HttpRequest()) {
			var si = ff.getFirst().streamIdentifier();
			var hff = new ArrayList<HeaderField>();
			var dbb = ByteBuffer.allocate(ff.stream().filter(x -> x instanceof Frame.Data)
					.mapToInt(x -> ((Frame.Data) x).data().length).sum());
			for (var f : ff)
				if (f instanceof Frame.Headers x)
					hff.addAll(x.fields());
				else
					dbb.put(((Frame.Data) f).data());
			rq.setHeaders(hff);
			rq.setBody(IO.toReadableByteChannel(dbb.flip()));
//			System.out.println("HttpServer.handleStream, rq=" + rq.getTarget());
//			ff.clear();
			try (var rs = new HttpResponse()) {
				rs.setHeaders(new ArrayList<>(List.of(new HeaderField(":status", null))));
//				var baos = new ByteArrayOutputStream();
//				rs.setBody(Channels.newChannel(baos));
				rs.setBody(new WritableByteChannel() {

					private boolean closed;

					private long written;

					@Override
					public boolean isOpen() {
						return !closed;
					}

					@Override
					public void close() throws IOException {
//						System.out.println("HttpServer.handleStream, close");
						if (closed)
							return;
						var f = written == 0 ? new Frame.Headers(false, true, true, si, false, 0, 0, rs.getHeaders())
								: new Frame.Data(false, true, si, new byte[0]);
						var bb = he.encodeFrame(f);
						writeFrame(st, bb);
						closed = true;
					}

					@Override
					public int write(ByteBuffer src) throws IOException {
//						System.out.println("HttpServer.handleStream, write");
						if (closed)
							throw new IOException("closed");
						var w = written;
						while (src.hasRemaining()) {
							var n = Math.min(src.remaining(), 16384);
							if (written == 0) {
								var f = new Frame.Headers(false, true, false, si, false, 0, 0, rs.getHeaders());
								var bb = he.encodeFrame(f);
								writeFrame(st, bb);
							}
							var bb = new byte[n];
							src.get(bb);
							var f = new Frame.Data(false, false, si, bb);
							bb = he.encodeFrame(f);
							writeFrame(st, bb);
							written += n;
						}
						return (int) (written - w);
					}
				});
				var ex = createExchange(rq);
				ex.setRequest(rq);
				ex.setResponse(rs);
				ScopedValue.where(HTTP_EXCHANGE, ex).call(() -> handleExchange(ex));
//				var bb = baos.toByteArray();
//				ff.add(new Frame.Headers(false, true, bb.length == 0, si, false, 0, 0, rs.getHeaders()));
//				for (var o = 0; o < bb.length;) {
//					var l = Math.min(bb.length - o, 16384);
//					ff.add(new Frame.Data(false, o + l == bb.length, si, Arrays.copyOfRange(bb, o, o + l)));
//					o += l;
//				}
			}
//			for (var f : ff) {
//				var bb = he.encodeFrame(f);
//				writeFrame(st, bb);
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected HttpExchange createExchange(HttpRequest request) {
		return new HttpExchange();
	}

	protected boolean handleExchange(HttpExchange he) {
		var k = true;
		Exception e;
		try {
			k = handler.handle(he);
			e = null;
		} catch (HandleException x) {
			e = x.getCause();
		} catch (UncheckedIOException x) {
			e = x.getCause();
		} catch (Exception x) {
			e = x;
		}
		if (e != null)
			try {
				e.printStackTrace();
				he.setException(e);
				k = handler.handle(he);
			} catch (Exception x) {
				x.printStackTrace();
				k = false;
			}
		return k;
	}

	protected void writeFrame(SecureTransfer st, byte[] bb) throws IOException {
		st.outLock().lock();
		try {
			st.out().clear();
			for (var o = 0; o < bb.length;) {
				var l = Math.min(bb.length - o, st.out().remaining());
				st.out().put(bb, o, l);
				if (!st.out().hasRemaining()) {
					st.out().flip();
					do
						st.write();
					while (st.out().hasRemaining());
					st.out().clear();
				}
				o += l;
			}
			st.out().flip();
			while (st.out().hasRemaining())
				st.write();
		} finally {
			st.outLock().unlock();
		}
	}
}

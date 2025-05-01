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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
		spp.setApplicationProtocols(new String[] { "h2" });
		se.setSSLParameters(spp);
		return se;
	}

	@Override
	protected void handleConnection(SecureTransfer st) throws IOException {
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

		byte[] cp;
		st.inLock().lock();
		try {
			do
				st.read();
			while (st.in().position() < 24);
			st.in().flip();
			cp = new byte[24];
			st.in().get(cp);
		} finally {
			st.inLock().unlock();
		}
//		System.out.println("cp=" + new String(cp));
		if (!Arrays.equals(cp, """
				PRI * HTTP/2.0\r
				\r
				SM\r
				\r
				""".getBytes()))
			throw new RuntimeException();

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
//			System.out.println("rq=" + rq);
			ff.clear();
			try (var rs = new HttpResponse()) {
				rs.setHeaders(new ArrayList<>(List.of(new HeaderField(":status", null))));
				var baos = new ByteArrayOutputStream();
				rs.setBody(Channels.newChannel(baos));
				var ex = createExchange(rq);
				ex.setRequest(rq);
				ex.setResponse(rs);
				ScopedValue.where(HTTP_EXCHANGE, ex).call(() -> handleExchange(ex));
				var bb = baos.toByteArray();
				ff.add(new Frame.Headers(false, true, bb.length == 0, si, false, 0, 0, rs.getHeaders()));
				for (var o = 0; o < bb.length;) {
					var l = Math.min(bb.length - o, 16384);
					ff.add(new Frame.Data(false, o + l == bb.length, si, Arrays.copyOfRange(bb, o, o + l)));
					o += l;
				}
			}
			for (var f : ff) {
				var bb = he.encodeFrame(f);
				writeFrame(st, bb);
			}
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

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
package com.janilla.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.function.Function;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpMessageReadableByteChannel;
import com.janilla.http.HttpMessageWritableByteChannel;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse.Status;

public class ResourceHandlerFactory implements WebHandlerFactory {

	public static void main(String[] args) throws Exception {
		var f = new ResourceHandlerFactory();
		f.setToInputStream(u -> u.getPath().equals("/test.html") ? new ByteArrayInputStream("""
				<html>
					<head>
						<title>My test page</title>
					</head>
					<body>
						<p>My cat is very grumpy</p>
					</body>
				</html>""".getBytes()) : null);

		var i = new ByteArrayInputStream("""
				GET /test.html HTTP/1.1\r
				Content-Length: 0\r
				\r
				""".getBytes());
		var o = new ByteArrayOutputStream();
		try (var r = new HttpMessageReadableByteChannel(Channels.newChannel(i));
				var q = r.readRequest();
				var w = new HttpMessageWritableByteChannel(Channels.newChannel(o));
				var s = w.writeResponse()) {
			var c = new HttpExchange();
			c.setRequest(q);
			c.setResponse(s);
			var h = f.createHandler(q, c);
			h.handle(c);
		}

		var s = o.toString();
		System.out.println(s);
		assert Objects.equals(s, """
				HTTP/1.1 200 OK\r
				Cache-Control: max-age=3600\r
				Content-Length: 109\r
				\r
				<html>
					<head>
						<title>My test page</title>
					</head>
					<body>
						<p>My cat is very grumpy</p>
					</body>
				</html>""") : s;
	}

	protected Function<URI, InputStream> toInputStream;

	public Function<URI, InputStream> getToInputStream() {
		return toInputStream;
	}

	public void setToInputStream(Function<URI, InputStream> toInputStream) {
		this.toInputStream = toInputStream;
	}

	@Override
	public WebHandler createHandler(Object object, HttpExchange exchange) {
		var u = object instanceof HttpRequest q ? q.getURI() : null;
		var i = u != null ? toInputStream.apply(u) : null;
		return i != null ? c -> handle(i, (HttpRequest) object, c) : null;
	}

	protected void handle(InputStream stream, HttpRequest request, HttpExchange exchange) {
		try {
			var s = exchange.getResponse();
			s.setStatus(new Status(200, "OK"));
			var hh = s.getHeaders();
			hh.set("Cache-Control", "max-age=3600");

			var u = request.getURI().toString();
			var e = u.substring(u.lastIndexOf('.') + 1);
			switch (e) {
			case "ico":
				hh.set("Content-Type", "image/x-icon");
				break;
			case "js":
				hh.set("Content-Type", "text/javascript");
				break;
			case "svg":
				hh.set("Content-Type", "image/svg+xml");
				break;
			}

			var b = (WritableByteChannel) s.getBody();
			stream.transferTo(Channels.newOutputStream(b));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}
	}
}

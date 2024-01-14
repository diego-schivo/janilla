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
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.function.Function;

import com.janilla.http.ExchangeContext;
import com.janilla.http.HttpMessageReadableByteChannel;
import com.janilla.http.HttpMessageWritableByteChannel;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse.Status;
import com.janilla.io.IO;

public class ResourceHandlerFactory implements HandlerFactory {

	public static void main(String[] args) throws IOException {
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
		var r = new HttpMessageReadableByteChannel(Channels.newChannel(i));
		var o = new ByteArrayOutputStream();
		var w = new HttpMessageWritableByteChannel(Channels.newChannel(o));

		try (var q = r.readRequest(); var s = w.writeResponse()) {
			var h = f.createHandler(q);
			var c = new ExchangeContext();
			c.setRequest(q);
			c.setResponse(s);
			h.accept(c);
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
	public IO.Consumer<ExchangeContext> createHandler(Object object) {
		var u = object instanceof HttpRequest q ? q.getURI() : null;
		var i = u != null ? toInputStream.apply(u) : null;
		return i != null ? c -> handle(i, (HttpRequest) object, c) : null;
	}

	protected void handle(InputStream stream, HttpRequest request, ExchangeContext context) throws IOException {
		try {
			context.getResponse().setStatus(new Status(200, "OK"));
			context.getResponse().getHeaders().set("Cache-Control", "max-age=3600");

			var u = request.getURI().toString();
			if (u.endsWith(".js"))
				context.getResponse().getHeaders().set("Content-Type", "text/javascript");
			if (u.endsWith(".ico"))
				context.getResponse().getHeaders().set("Content-Type", "image/x-icon");

			var b = (WritableByteChannel) context.getResponse().getBody();
			stream.transferTo(Channels.newOutputStream(b));
		} finally {
			stream.close();
		}
	}
}

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
package com.janilla.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.java.Java;

public class DefaultFileHandlerFactory implements FileHandlerFactory {

	protected final ResourceMap resourceMap;

	public DefaultFileHandlerFactory(ResourceMap resourceMap) {
		this.resourceMap = resourceMap;
	}

	@Override
	public HttpHandler createHandler(Object object) {
		if (object instanceof HttpRequest rq) {
			var f = resourceMap.get(rq.getPath());
			if (f != null)
				return x -> {
					handle(f, x);
					return true;
				};
		}
		return null;
	}

	protected void handle(Resource file, HttpExchange exchange) {
//		IO.println("FileHandlerFactory.handle, file=" + file);
		var rs = exchange.response();
		rs.setStatus(200);
		rs.setHeaderValue("cache-control", "max-age=3600");
		switch (file.path().substring(file.path().lastIndexOf('.') + 1).toLowerCase()) {
		case "html":
			rs.setHeaderValue("content-type", "text/html");
			break;
		case "ico":
			rs.setHeaderValue("content-type", "image/x-icon");
			break;
		case "js":
			rs.setHeaderValue("content-type", "text/javascript");
			break;
		case "svg":
			rs.setHeaderValue("content-type", "image/svg+xml");
			break;
		}
		rs.setHeaderValue("content-length", String.valueOf(file.size()));

		try (var in = switch (file) {
		case DefaultResource x -> x.newInputStream();
		case ZipEntryResource x -> {
			var u = x.archive().uri();
//			IO.println("FileHandlerFactory.handle, u=" + u);
			var s = u.toString();
			if (!s.startsWith("jar:"))
				u = URI.create("jar:" + s);
			var p = Java.zipFileSystem(u).getPath(x.path());
//			IO.println("FileHandlerFactory.handle, p=" + p);
			yield Files.newInputStream(p);
		}
		default -> throw new IllegalArgumentException();
		}; var out = Channels.newOutputStream((WritableByteChannel) rs.getBody())) {
			if (in == null)
				throw new NullPointerException(file.toString());
			in.transferTo(out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

//	public static void main(String[] args) throws Exception {
//	var f = new FileHandlerFactory();
//	f.setToInputStream(u -> u.getPath().equals("/test.html") ? new ByteArrayInputStream("""
//			<html>
//				<head>
//					<title>My test page</title>
//				</head>
//				<body>
//					<p>My cat is very grumpy</p>
//				</body>
//			</html>""".getBytes()) : null);
//
//	var i = new ByteArrayInputStream("""
//			GET /test.html HTTP/1.1\r
//			Content-Length: 0\r
//			\r
//			""".getBytes());
//	var o = new ByteArrayOutputStream();
//	try (var r = new HttpMessageReadableByteChannel(Channels.newChannel(i));
//			var q = r.readRequest();
//			var w = new HttpMessageWritableByteChannel(Channels.newChannel(o));
//			var s = w.writeResponse()) {
//		var c = new HttpExchange();
//		c.setRequest(q);
//		c.setResponse(s);
//		var h = f.createHandler(q, c);
//		h.handle(c);
//	}
//
//	var s = o.toString();
//	IO.println(s);
//	assert Objects.equals(s, """
//			HTTP/1.1 200 OK\r
//			Cache-Control: max-age=3600\r
//			Content-Length: 109\r
//			\r
//			<html>
//				<head>
//					<title>My test page</title>
//				</head>
//				<body>
//					<p>My cat is very grumpy</p>
//				</body>
//			</html>""") : s;
//}
}

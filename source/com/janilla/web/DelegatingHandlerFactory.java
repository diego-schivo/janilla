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
import java.nio.channels.Channels;
import java.util.function.Function;

import com.janilla.http.ExchangeContext;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpMessageReadableByteChannel;
import com.janilla.http.HttpMessageWritableByteChannel;

public class DelegatingHandlerFactory implements HandlerFactory {

	public static void main(String[] args) throws IOException {
		var f = new DelegatingHandlerFactory();
		{
			var f1 = new ResourceHandlerFactory();
			f1.setToInputStream(u -> u.getPath().equals("/test.html") ? new ByteArrayInputStream("""
					<html>
						<head>
							<title>My test page</title>
						</head>
						<body>
							<p>My cat is very grumpy</p>
						</body>
					</html>""".getBytes()) : null);
			var f2 = new ExceptionHandlerFactory();
			var a = new HandlerFactory[] { f1, f2 };
			f.setToHandler(o -> {
				if (a != null)
					for (var g : a) {
						var h = g.createHandler(o);
						if (h != null)
							return h;
					}
				return null;
			});
		}

		var t = new String[] { """
				GET /test.html HTTP/1.1\r
				Content-Length: 0\r
				\r
				""", """
				GET /test-not-found.html HTTP/1.1\r
				Content-Length: 0\r
				\r
				""" };
		for (var j = 0; j < t.length; j++)
			try (var i = new ByteArrayInputStream(t[j].getBytes());
					var r = new HttpMessageReadableByteChannel(Channels.newChannel(i));
					var o = new ByteArrayOutputStream();
					var w = new HttpMessageWritableByteChannel(Channels.newChannel(o))) {

				try (var q = r.readRequest(); var s = w.writeResponse()) {
					var c = new ExchangeContext();
					c.setRequest(q);
					c.setResponse(s);
					try {
						var h = f.createHandler(q);
						if (h == null)
							throw new NotFoundException();
						h.handle(c);
					} catch (Exception e) {
						var h = f.createHandler(e);
						h.handle(c);
					}
				}

				var s = o.toString();
				System.out.println(s);
				assert s.equals(j == 0 ? """
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
						</html>""" : """
						HTTP/1.1 404 Not Found\r
						Cache-Control: no-cache\r
						Content-Length: 0\r
						\r
						""") : s;
			}
	}

	Function<Object, HttpHandler> toHandler;

	public Function<Object, HttpHandler> getToHandler() {
		return toHandler;
	}

	public void setToHandler(Function<Object, HttpHandler> toHandler) {
		this.toHandler = toHandler;
	}

	@Override
	public HttpHandler createHandler(Object object) {
		return toHandler != null ? toHandler.apply(object) : null;
	}
}

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
import java.nio.channels.Channels;
import java.util.function.BiFunction;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpMessageReadableByteChannel;
import com.janilla.http.HttpMessageWritableByteChannel;

public class DelegatingHandlerFactory implements WebHandlerFactory {

	public static void main(String[] args) throws Exception {
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
			var a = new WebHandlerFactory[] { f1, f2 };
			f.setToHandler((o, c) -> {
				if (a != null)
					for (var g : a) {
						var h = g.createHandler(o, c);
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
					var c = new HttpExchange();
					c.setRequest(q);
					c.setResponse(s);
					try {
						var h = f.createHandler(q, c);
						if (h == null)
							throw new NotFoundException();
						h.handle(c);
					} catch (Exception e) {
						var h = f.createHandler(e, c);
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

	BiFunction<Object, HttpExchange, WebHandler> toHandler;

	public BiFunction<Object, HttpExchange, WebHandler> getToHandler() {
		return toHandler;
	}

	public void setToHandler(BiFunction<Object, HttpExchange, WebHandler> toHandler) {
		this.toHandler = toHandler;
	}

	@Override
	public WebHandler createHandler(Object object, HttpExchange exchange) {
		return toHandler != null ? toHandler.apply(object, exchange) : null;
	}
}

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

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpMessageReadableByteChannel;
import com.janilla.http.HttpMessageWritableByteChannel;
import com.janilla.http.HttpResponse.Status;

public class ExceptionHandlerFactory implements WebHandlerFactory {

	public static void main(String[] args) throws Exception {
		var f = new ExceptionHandlerFactory();

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
			var e = new NotFoundException();
			var c = new HttpExchange();
			c.setRequest(q);
			c.setResponse(s);
			var h = f.createHandler(e, c);
			h.handle(c);
		}

		var t = o.toString();
		System.out.println(t);
		assert t.equals("""
				HTTP/1.1 404 Not Found\r
				Cache-Control: no-cache\r
				Content-Length: 0\r
				\r
				""") : t;
	}

	@Override
	public WebHandler createHandler(Object object, HttpExchange exchange) {
		if (object instanceof Exception e) {
			var f = e.getClass().getAnnotation(Error.class);
			return c -> handle(f, c);
		}
		return null;
	}

	protected void handle(Error error, HttpExchange exchange) {
		var s = exchange.getResponse();
		var t = error != null ? new Status(error.code(), error.text()) : new Status(500, "Internal Server Error");

//		System.out.println("t=" + t);

		s.setStatus(t);
		s.getHeaders().set("Cache-Control", "no-cache");
	}
}

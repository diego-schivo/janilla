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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

import com.janilla.io.IO;

public class FilterHttpRequest extends FilterHttpMessage<HttpRequest> implements HttpRequest {

	public static void main(String[] args) throws Exception {
		var i = new ByteArrayInputStream("""
				POST /foo HTTP/1.1\r
				Content-Length: 3\r
				\r
				baz""".getBytes());
		try (var c = new HttpMessageReadableByteChannel(Channels.newChannel(i));
				var r = new FilterHttpRequest(c.readRequest()) {

					@Override
					public URI getURI() {
						return URI.create("/bar");
					}
				}) {
			var t = r.getMethod().name() + " " + r.getURI();
			System.out.println(t);
			assert Objects.equals(t, "POST /bar") : t;

			var b = (ReadableByteChannel) r.getBody();
			t = new String(IO.readAllBytes(b));
			System.out.println(t);
			assert Objects.equals(t, "baz") : t;
		}
	}

	protected FilterHttpRequest(HttpRequest message) {
		super(message);
	}

	@Override
	public Method getMethod() {
		return message.getMethod();
	}

	@Override
	public void setMethod(Method value) {
		message.setMethod(value);
	}

	@Override
	public URI getURI() {
		return message.getURI();
	}

	@Override
	public void setURI(URI value) {
		message.setURI(value);
	}
}

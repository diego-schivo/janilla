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

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class HttpMessage implements Closeable {

	private List<HeaderField> headers;

	private Channel body;

	public List<HeaderField> getHeaders() {
		return headers;
	}

	public void setHeaders(List<HeaderField> headers) {
		this.headers = headers;
	}

	public Channel getBody() {
		return body;
	}

	public void setBody(Channel body) {
		this.body = body;
	}

	public Stream<HeaderField> getHeaders(String name) {
		return headers != null ? headers.stream().filter(x -> x.name().equals(name)) : Stream.empty();
	}

	public HeaderField getHeader(String name) {
		return getHeaders(name).findFirst().orElse(null);
	}

	public void setHeader(HeaderField header) {
		if (headers == null)
			headers = new ArrayList<>();
		else {
			var i = 0;
			for (var x : headers) {
				if (x.name().equals(header.name())) {
					headers.set(i, header);
					return;
				}
				i++;
			}
		}
		headers.add(header);
	}

	public Stream<String> getHeaderValues(String name) {
		return getHeaders(name).map(HeaderField::value);
	}

	public String getHeaderValue(String name) {
		var x = getHeader(name);
		return x != null ? x.value() : null;
	}

	public void setHeaderValue(String name, String value) {
		setHeader(new HeaderField(name, value));
	}

	@Override
	public void close() throws IOException {
		if (body != null)
			body.close();
	}
}

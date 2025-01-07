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

import java.nio.channels.Channel;
import java.util.List;

public abstract class HttpMessage {

	private List<HeaderField> headers;

	private Channel body;

	// *******************
	// Getters and Setters

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

	// Getters and Setters
	// *******************

	public String getHeaderValue(String name) {
		return headers != null
				? headers.stream().filter(x -> x.name().equals(name)).findFirst().map(HeaderField::value).orElse(null)
				: null;
	}

	public void setHeaderValue(String name, String value) {
		var i = 0;
		for (var h : headers) {
			if (h.name().equals(name)) {
				headers.set(i, h.withValue(value));
				return;
			}
			i++;
		}
		headers.add(new HeaderField(name, value));
	}
}

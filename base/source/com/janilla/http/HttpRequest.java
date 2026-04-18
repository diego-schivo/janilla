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
package com.janilla.http;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HttpRequest extends HttpMessage {

	public HttpRequest() {
	}

	public HttpRequest(String method, URI uri) {
		this(method, uri, null);
	}

	public HttpRequest(String method, URI uri, String cookie) {
//		IO.println("HttpRequest, method=" + method + ", uri=" + uri + ", cookie=" + cookie);
		if (method != null && !method.isEmpty())
			setHeaderValue(":method", method);
		if (uri != null)
			setUri(uri);
		if (cookie != null && !cookie.isEmpty())
			setCookie(cookie);
	}

	public String getPath() {
		var t = getHeaderValue(":path");
		var i = t != null ? t.indexOf('?') : -1;
		return URLDecoder.decode(i != -1 ? t.substring(0, i) : t, StandardCharsets.UTF_8);
	}

	public String getQuery() {
		var t = getHeaderValue(":path");
		var i = t != null ? t.indexOf('?') : -1;
		return i != -1 ? t.substring(i + 1) : null;
	}

	public URI getUri() {
		return URI.create(getHeaderValue(":scheme") + "://" + getHeaderValue(":authority") + getHeaderValue(":path"));
	}

	public void setUri(URI uri) {
		setHeaderValue(":scheme", uri.getScheme());
		setHeaderValue(":authority", uri.getAuthority());
		var b = new StringBuilder(uri.getRawPath());
		if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty())
			b.append('?').append(uri.getRawQuery());
		setHeaderValue(":path", b.toString());
	}

	public void setBasicAuthorization(String credentials) {
		setHeaderValue("authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()));
	}

	public void setCookie(String cookie) {
		setHeaderValue("cookie", cookie);
	}
}

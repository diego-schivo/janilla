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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.janilla.net.Net;
import com.janilla.util.EntryList;

public abstract class Http {

	public static EntryList<String, String> parseCookieHeader(String string) {
		return Net.parseEntryList(string, ";", "=");
	}

	public static String formatSetCookieHeader(String name, String value, ZonedDateTime expires, String path,
			String sameSite) {
		var b = new StringBuilder();
		b.append(name + "=" + (value != null ? value : ""));
		if (expires != null) {
			b.append("; Expires="
					+ expires.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O", Locale.ENGLISH)));
		}
		b.append("; Path=" + path);
		b.append("; SameSite=" + sameSite);
		return b.toString();
	}

//	public static String fetch(URI uri, HttpRequest.Method method, Map<String, String> headers, String body) {
//		try (var c = new HttpClient()) {
//			var p = uri.getPort();
//			if (p == -1)
//				p = switch (uri.getScheme()) {
//				case "http" -> 80;
//				case "https" -> 443;
//				default -> throw new RuntimeException();
//				};
//			c.setAddress(new InetSocketAddress(uri.getHost(), p));
//			if (uri.getScheme().equals("https"))
//				try {
//					var x = SSLContext.getInstance("TLSv1.2");
//					x.init(null, null, null);
//					c.setSslContext(x);
//				} catch (GeneralSecurityException e) {
//					throw new RuntimeException(e);
//				}
//			return c.query(e -> {
//				try (var q = e.getRequest()) {
//					q.setMethod(method);
//					q.setUri(URI.create(uri.getPath()));
//					var hh = q.getHeaders();
//					for (var f : headers.entrySet())
//						hh.add(new HttpHeader(f.getKey(), f.getValue()));
//					if (hh.stream().noneMatch(x -> x.name().equals("Host")))
//						hh.add(new HttpHeader("Host", uri.getHost()));
//					IO.write(body.getBytes(), (WritableByteChannel) q.getBody());
//				} catch (IOException f) {
//					throw new UncheckedIOException(f);
//				}
//
//				try (var s = e.getResponse()) {
//					return new String(IO.readAllBytes((ReadableByteChannel) s.getBody()));
//				} catch (IOException f) {
//					throw new UncheckedIOException(f);
//				}
//			});
//		}
//	}
}

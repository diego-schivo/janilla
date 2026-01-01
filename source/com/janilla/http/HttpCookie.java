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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public record HttpCookie(String name, String value, String domain, ZonedDateTime expires, boolean httpOnly,
		Integer maxAge, boolean partitioned, String path, String sameSite, boolean secure) {

	private static final DateTimeFormatter EXPIRES_FORMATTER = DateTimeFormatter
			.ofPattern("EEE, dd MMM yyyy HH:mm:ss O", Locale.ENGLISH);

	public static HttpCookie of(String name, String value) {
		return new HttpCookie(name, value, null, null, false, null, false, null, null, false);
	}

	public static HttpCookie parse(String string) {
		var i = 0;
		String n = null;
		String v = null;
		String d = null;
		ZonedDateTime e = null;
		var ho = false;
		Integer ma = null;
		var p1 = false;
		String p2 = null;
		String ss = null;
		var s = false;
		for (var x : string.split(";")) {
			var yy = Arrays.stream(x.split("=", 2)).map(String::trim).toArray(String[]::new);
			if (i == 0) {
				n = yy[0];
				if (yy.length > 1 && !yy[1].isEmpty())
					v = yy[1];
			} else
				switch (yy[0]) {
				case "Domain":
					if (yy.length > 1 && !yy[1].isEmpty())
						d = yy[1];
					break;
				case "Expires":
					if (yy.length > 1 && !yy[1].isEmpty())
						e = ZonedDateTime.parse(yy[1], EXPIRES_FORMATTER);
					break;
				case "HttpOnly":
					ho = true;
					break;
				case "MaxAge":
					if (yy.length > 1 && !yy[1].isEmpty())
						ma = Integer.parseInt(yy[1]);
					break;
				case "Partitioned":
					p1 = true;
					break;
				case "Path":
					if (yy.length > 1 && !yy[1].isEmpty())
						p2 = yy[1];
					break;
				case "SameSite":
					if (yy.length > 1 && !yy[1].isEmpty())
						ss = yy[1];
					break;
				case "Secure":
					s = true;
					break;
				}
			i++;
		}
		return new HttpCookie(n, v, d, e, ho, ma, p1, p2, ss, s);
	}

	public HttpCookie withDomain(String domain) {
		return new HttpCookie(name, value, domain, expires, httpOnly, maxAge, partitioned, path, sameSite, secure);
	}

	public HttpCookie withExpires(ZonedDateTime expires) {
		return new HttpCookie(name, value, domain, expires, httpOnly, maxAge, partitioned, path, sameSite, secure);
	}

	public HttpCookie withHttpOnly(boolean httpOnly) {
		return new HttpCookie(name, value, domain, expires, httpOnly, maxAge, partitioned, path, sameSite, secure);
	}

	public HttpCookie withMaxAge(Integer maxAge) {
		return new HttpCookie(name, value, domain, expires, httpOnly, maxAge, partitioned, path, sameSite, secure);
	}

	public HttpCookie withPartitioned(boolean partitioned) {
		return new HttpCookie(name, value, domain, expires, httpOnly, maxAge, partitioned, path, sameSite, secure);
	}

	public HttpCookie withPath(String path) {
		return new HttpCookie(name, value, domain, expires, httpOnly, maxAge, partitioned, path, sameSite, secure);
	}

	public HttpCookie withSameSite(String sameSite) {
		return new HttpCookie(name, value, domain, expires, httpOnly, maxAge, partitioned, path, sameSite, secure);
	}

	public HttpCookie withSecure(boolean secure) {
		return new HttpCookie(name, value, domain, expires, httpOnly, maxAge, partitioned, path, sameSite, secure);
	}

	public String format() {
		var b = new StringBuilder();
		b.append(name + "=" + Objects.toString(value, ""));
		if (domain != null)
			b.append("; Domain=" + domain);
		if (expires != null)
			b.append("; Expires=" + expires.format(EXPIRES_FORMATTER));
		if (httpOnly)
			b.append("; HttpOnly");
		if (maxAge != null)
			b.append("; MaxAge=" + maxAge);
		if (partitioned)
			b.append("; Partitioned");
		if (path != null)
			b.append("; Path=" + path);
		if (sameSite != null)
			b.append("; SameSite=" + sameSite);
		if (secure)
			b.append("; Secure");
		return b.toString();
	}
}

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

public record HttpCookie(String name, String value, String domain, ZonedDateTime expires, boolean httpOnly,
		Integer maxAge, boolean partitioned, String path, String sameSite, boolean secure) {

	private static DateTimeFormatter EXPIRES_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O",
			Locale.ENGLISH);

	public static HttpCookie of(String name, String value) {
		return new HttpCookie(name, value, null, null, false, null, false, null, null, false);
	}

	public static HttpCookie parse(String text) {
		var ss0 = text.split(";");
		String n = null;
		String v = null;
		String d = null;
		ZonedDateTime e = null;
		var ho = false;
		Integer ma = null;
		var pr = false;
		String pt = null;
		String ss = null;
		var s = false;
		for (var i = 0; i < ss0.length; i++) {
			var j = ss0[i].indexOf('=');
			var s1 = ss0[i].substring(0, j).trim();
			var s2 = ss0[i].substring(j + 1).trim();
			if (i == 0) {
				n = s1;
				if (s2.length() > 0)
					v = s2;
			} else
				switch (s1) {
				case "Domain":
					if (s2.length() > 0)
						d = s2;
					break;
				case "Expires":
					if (s2.length() > 0)
						e = ZonedDateTime.parse(s2, EXPIRES_FORMATTER);
					break;
				case "HttpOnly":
					ho = true;
					break;
				case "MaxAge":
					if (s2.length() > 0)
						ma = Integer.parseInt(s2);
					break;
				case "Partitioned":
					pr = true;
					break;
				case "Path":
					if (s2.length() > 0)
						pt = s2;
					break;
				case "SameSite":
					if (s2.length() > 0)
						ss = s2;
					break;
				case "Secure":
					s = true;
					break;
				}
		}
		return new HttpCookie(n, v, d, e, ho, ma, pr, pt, ss, s);
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
		var sb = new StringBuilder();
		sb.append(name + "=" + (value != null ? value : ""));
		if (domain != null) {
			sb.append("; Domain=" + domain);
		}
		if (expires != null) {
			sb.append("; Expires=" + expires.format(EXPIRES_FORMATTER));
		}
		if (httpOnly) {
			sb.append("; HttpOnly");
		}
		if (maxAge != null) {
			sb.append("; MaxAge=" + maxAge);
		}
		if (partitioned) {
			sb.append("; Partitioned");
		}
		if (path != null) {
			sb.append("; Path=" + path);
		}
		if (sameSite != null) {
			sb.append("; SameSite=" + sameSite);
		}
		if (secure) {
			sb.append("; Secure");
		}
		return sb.toString();
	}
}

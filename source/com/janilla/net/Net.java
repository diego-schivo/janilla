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
package com.janilla.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.janilla.java.Java;

public final class Net {

	private Net() {
		throw new Error("no instances");
	}

	public static SSLContext getSSLContext(Map.Entry<String, InputStream> keyStore, char[] password) {
		try {
			var ks = KeyStore.getInstance(keyStore.getKey());
			ks.load(keyStore.getValue(), password);
			var kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, password);
			var tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);
			var sc = SSLContext.getInstance("TLSv1.3");
			sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			return sc;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Java.EntryList<String, String> parseEntryList(String string, String delimiter1, String delimiter2) {
		return string != null ? Arrays.stream(string.split(delimiter1)).map(s -> {
			var i = s.indexOf(delimiter2);
			var k = (i >= 0 ? s.substring(0, i) : s).trim();
			var v = i >= 0 ? s.substring(i + 1).trim() : null;
			return new AbstractMap.SimpleImmutableEntry<>(urlDecode(k), urlDecode(v));
		}).reduce(new Java.EntryList<>(), (l, e) -> {
			l.add(e.getKey(), e.getValue());
			return l;
		}, (a, _) -> a) : null;
	}

	public static Java.EntryList<String, String> parseQueryString(String string) {
		return parseEntryList(string, "&", "=");
	}

	public static String urlDecode(String string) {
		return string != null ? URLDecoder.decode(string, StandardCharsets.UTF_8) : null;
	}

	public static String urlEncode(String string) {
		return string != null ? URLEncoder.encode(string, StandardCharsets.UTF_8) : null;
	}

	@SafeVarargs
	public static String uriString(String pathname, Map.Entry<String, String>... search) {
		var s = Arrays.stream(search).filter(Objects::nonNull)
				.map(x -> x.getValue() != null ? urlEncode(x.getKey()) + "=" + urlEncode(x.getValue())
						: urlEncode(x.getKey()))
				.collect(Collectors.joining("&"));
		return Stream.of(pathname, s).filter(x -> x != null && !x.isEmpty()).collect(Collectors.joining("?"));
	}

//	public static String uriString(String pathname, String name1, String value1) {
//		return pathname + "?" + (value1 != null ? urlEncode(name1) + "=" + urlEncode(value1) : urlEncode(name1));
//	}
}

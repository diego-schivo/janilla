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
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.janilla.util.EntryList;

public interface Net {

	static URI createURI(String scheme, String host, int port, String path, String query) {
		var b = new StringBuilder();
		if (scheme != null && !scheme.isEmpty())
			b.append(scheme).append("://");
		if (host != null && !host.isEmpty())
			b.append(host);
		if (port >= 0)
			b.append(':').append(port);
		if (path != null && !path.isEmpty())
			b.append(path);
		if (query != null && !query.isEmpty())
			b.append('?').append(query);
		return URI.create(b.toString());
	}

	static String formatQueryString(EntryList<String, String> list) {
		return list != null
				? list.stream().map(e -> e.getValue() != null ? urlEncode(e.getKey()) + "=" + urlEncode(e.getValue())
						: urlEncode(e.getKey())).collect(Collectors.joining("&"))
				: null;
	}

	static SSLContext getSSLContext(String type, InputStream stream, char[] password) {
		try {
			var ks = KeyStore.getInstance(type);
			ks.load(stream, password);
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

	static EntryList<String, String> parseEntryList(String string, String delimiter1, String delimiter2) {
		return string != null ? Arrays.stream(string.split(delimiter1)).map(s -> {
			var i = s.indexOf(delimiter2);
			var k = (i >= 0 ? s.substring(0, i) : s).trim();
			var v = i >= 0 ? s.substring(i + 1).trim() : null;
			return new AbstractMap.SimpleEntry<>(urlDecode(k), urlDecode(v));
		}).reduce(new EntryList<>(), (l, e) -> {
			l.add(e.getKey(), e.getValue());
			return l;
		}, (a, _) -> a) : null;
	}

	static EntryList<String, String> parseQueryString(String string) {
		return parseEntryList(string, "&", "=");
	}

	static String urlDecode(String string) {
		return string != null ? URLDecoder.decode(string, StandardCharsets.UTF_8) : null;
	}

	static String urlEncode(String string) {
		return string != null ? URLEncoder.encode(string, StandardCharsets.UTF_8) : null;
	}
}

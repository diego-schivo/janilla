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
package com.janilla.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

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

	public static String urlDecode(String string) {
		return string != null ? URLDecoder.decode(string, StandardCharsets.UTF_8) : null;
	}

	public static String urlEncode(String string) {
		return string != null ? URLEncoder.encode(string, StandardCharsets.UTF_8) : null;
	}
}

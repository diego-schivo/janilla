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
package com.janilla.json;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public interface Jwt {

	public static void main(String[] args) {
		var h = Stream.of(Map.entry("alg", "HS256"), Map.entry("typ", "JWT"))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (v, w) -> v, LinkedHashMap::new));
		var p = Stream.of(Map.entry("loggedInAs", "admin"), Map.entry("iat", 1422779638))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (v, w) -> v, LinkedHashMap::new));
		var t = Jwt.generateToken(h, p, "secretkey");
		System.out.println(t);
		assert t.equals(
				"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJsb2dnZWRJbkFzIjoiYWRtaW4iLCJpYXQiOjE0MjI3Nzk2Mzh9.gzSraSYS8EXBxLN_oWnFSRgCzcmJmMjLiuyu5CSpyHI=")
				: t;

		var q = Jwt.verifyToken(t, "secretkey");
		System.out.println(q);
		assert q.equals(p) : q;
	}

	static String generateToken(Map<String, ?> header, Map<String, ?> payload, String key) {
		if (!header.equals(Map.of("alg", "HS256", "typ", "JWT")))
			throw new IllegalArgumentException("header=" + header);

		var h = Json.format(header);
		var p = Json.format(payload);
//		System.out.println("h=" + h + ", p=" + p);

		var e = Base64.getUrlEncoder();
		var t = e.encodeToString(h.getBytes()) + "." + e.encodeToString(p.getBytes());
//		System.out.println("t=" + t);

		Mac m;
		try {
			m = Mac.getInstance("HmacSHA256");
		} catch (NoSuchAlgorithmException f) {
			throw new RuntimeException(f);
		}
		var k = new SecretKeySpec(key.getBytes(), "HmacSHA256");
		try {
			m.init(k);
		} catch (InvalidKeyException f) {
			throw new RuntimeException(f);
		}

		var a = m.doFinal(t.getBytes());
//		System.out.println("a=" + Arrays.toString(a));
		var s = e.encodeToString(a);
//		System.out.println("s=" + s);
		t += "." + s;
		return t;
	}

	static Map<String, ?> verifyToken(String token, String key) {
		var i = token.indexOf('.');
		var j = token.lastIndexOf('.');
		if (i < 0 || j <= i)
			throw new IllegalArgumentException("token=" + token);

		var d = Base64.getUrlDecoder();
		var h = Json.parse(new String(d.decode(token.substring(0, i))));

		if (!h.equals(Map.of("alg", "HS256", "typ", "JWT")))
			throw new IllegalArgumentException("h=" + h);

		Mac m;
		try {
			m = Mac.getInstance("HmacSHA256");
		} catch (NoSuchAlgorithmException f) {
			throw new RuntimeException(f);
		}
		var k = new SecretKeySpec(key.getBytes(), "HmacSHA256");
		try {
			m.init(k);
		} catch (InvalidKeyException f) {
			throw new RuntimeException(f);
		}
		var t = token.substring(0, j);
		var a = m.doFinal(t.getBytes());

		var e = Base64.getUrlEncoder();
		var s = e.encodeToString(a);
		if (!s.equals(token.substring(j + 1)))
			throw new IllegalArgumentException("s=" + s);

		var z = new String(d.decode(token.substring(i + 1, j)));
		@SuppressWarnings("unchecked")
		var p = (Map<String, ?>) Json.parse(z);
		return p;
	}
}

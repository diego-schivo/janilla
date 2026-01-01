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
package com.janilla.json;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public interface Jwt {

	static String generateToken(Map<String, ?> header, Map<String, ?> payload, String key) {
		if (!header.equals(Map.of("alg", "HS256", "typ", "JWT")))
			throw new IllegalArgumentException("header=" + header);

		var h = Json.format(header);
		var p = Json.format(payload);
//		IO.println("Jwt.generateToken, h=" + h + ", p=" + p);

		var ue = Base64.getUrlEncoder();
		var ss = new String[] { ue.encodeToString(h.getBytes()), ue.encodeToString(p.getBytes()), null };

		Mac m;
		try {
			m = Mac.getInstance("HmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		var k = new SecretKeySpec(key.getBytes(), "HmacSHA256");
		try {
			m.init(k);
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}

		var bb = m.doFinal((ss[0] + "." + ss[1]).getBytes());
//		IO.println("Jwt.generateToken, bb=" + Arrays.toString(bb));
		ss[2] = ue.encodeToString(bb);
//		IO.println("ss=" + Arrays.toString(ss));
		return Arrays.stream(ss).collect(Collectors.joining("."));
	}

	static Map<String, ?> verifyToken(String token, String key) {
		var i1 = token.indexOf('.');
		var i2 = token.lastIndexOf('.');
		if (i1 == -1 || i2 <= i1)
			throw new IllegalArgumentException("token=" + token);

		var ud = Base64.getUrlDecoder();
		var hs = new String(ud.decode(token.substring(0, i1)));
		var h = Json.parse(hs);

		if (!h.equals(Map.of("alg", "HS256", "typ", "JWT")))
			throw new IllegalArgumentException("h=" + h);

		Mac m;
		try {
			m = Mac.getInstance("HmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		var k = new SecretKeySpec(key.getBytes(), "HmacSHA256");
		try {
			m.init(k);
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}
		var bb = m.doFinal(token.substring(0, i2).getBytes());

		var ue = Base64.getUrlEncoder();
		var s = ue.encodeToString(bb);
		if (!s.equals(token.substring(i2 + 1)))
			throw new IllegalArgumentException("s=" + s);

		var ps = new String(ud.decode(token.substring(i1 + 1, i2)));
		@SuppressWarnings("unchecked")
		var p = (Map<String, ?>) Json.parse(ps);
		return p;
	}

	public static void main(String[] args) {
		var h = Stream.of(Map.entry("alg", "HS256"), Map.entry("typ", "JWT"))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (_, x) -> x, LinkedHashMap::new));
		var p = Stream.of(Map.entry("loggedInAs", "admin"), Map.entry("iat", 1422779638))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (_, x) -> x, LinkedHashMap::new));
		var t = Jwt.generateToken(h, p, "secretkey");
		IO.println(t);
		assert t.equals(
				"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJsb2dnZWRJbkFzIjoiYWRtaW4iLCJpYXQiOjE0MjI3Nzk2Mzh9.gzSraSYS8EXBxLN_oWnFSRgCzcmJmMjLiuyu5CSpyHI=")
				: t;

		var q = Jwt.verifyToken(t, "secretkey");
		IO.println(q);
		assert q.equals(p) : q;
	}
}

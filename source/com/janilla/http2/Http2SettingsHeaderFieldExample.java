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
package com.janilla.http2;

import java.util.Arrays;
import java.util.Base64;
import java.util.stream.IntStream;

import com.janilla.util.Util;

public class Http2SettingsHeaderFieldExample {

	public static void main(String[] args) {
		var rq = """
				GET / HTTP/1.1\r
				Connection: Upgrade, HTTP2-Settings\r
				Host: localhost:8080\r
				HTTP2-Settings: AAEAAEAAAAIAAAABAAMAAABkAAQBAAAAAAUAAEAA\r
				Upgrade: h2c\r
				User-Agent: Java-http-client/22.0.1\r
				\r
				""";
		BitsReader jj;
		{
			var v = rq.lines().filter(x -> x.startsWith("HTTP2-Settings:"))
					.map(x -> x.substring("HTTP2-Settings:".length()).trim()).findFirst().get();
			var bb = Base64.getUrlDecoder().decode(v);
			var ii = IntStream.range(0, bb.length).map(x -> bb[x]).toArray();
			var h = Util.toHexString(Arrays.stream(ii));
			System.out.println("h=" + h);
			jj = new BitsReader(Arrays.stream(ii).iterator());
//		var l = IntStream.range(0, 3).map(x -> jj.nextInt()).reduce(0, (a, b) -> (a << 8) | b);
//		System.out.println("l=" + l);
		}
		while (jj.hasNext()) {
			var i = jj.nextInt(16);
			var s = SettingName.of(i);
			var v = jj.nextInt(32);
			System.out.println(s + "=" + v);
		}
	}
}

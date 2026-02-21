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
package com.janilla.java;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UriQueryBuilder {

	protected final List<String[]> parameters = new ArrayList<>();

	public UriQueryBuilder() {
	}

	public UriQueryBuilder(String rawQuery) {
		if (rawQuery != null && !rawQuery.isEmpty())
			for (var s : rawQuery.split("&")) {
				var ss = s.split("=", 2);
				append(URLDecoder.decode(ss[0], StandardCharsets.UTF_8),
						URLDecoder.decode(ss[1], StandardCharsets.UTF_8));
			}
	}

	public Stream<String> names() {
		return parameters.stream().map(x -> x[0]).distinct();
	}

	public Stream<String> values(String name) {
		return parameters.stream().filter(x -> x[0].equals(name)).map(x -> x.length == 2 ? x[1] : null);
	}

	public UriQueryBuilder append(String name, String value) {
		if (value != null)
			parameters.add(new String[] { name, value });
		return this;
	}

	public UriQueryBuilder delete(String name) {
		parameters.removeIf(x -> x[0].equals(name));
		return this;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<String, ?> toMap() {
		Map<String, ?> m = new LinkedHashMap<>();
		names().forEach(n -> {
			var v = values(n).findFirst().orElse(null);
			var m1 = (Map) m;
			var ss = n.split("\\.");
			for (var i = 0; i < ss.length; i++) {
				if (ss[i].endsWith("]")) {
					var ss2 = ss[i].split("\\[", 2);
					var i2 = Integer.parseInt(ss2[1].substring(0, ss2[1].length() - 1));
					if (i2 < 0 || i2 >= 1000)
						throw new RuntimeException();
					var l = (List) m1.computeIfAbsent(ss2[0], _ -> new ArrayList());
					while (l.size() <= i2)
						l.add(null);
					if (i < ss.length - 1) {
						var m2 = (Map) l.get(i2);
						if (m2 == null) {
							m2 = new LinkedHashMap<>();
							l.set(i2, m2);
						}
						m1 = m2;
					} else
						l.set(i2, v);
				} else if (i < ss.length - 1)
					m1 = (Map) m1.computeIfAbsent(ss[i], _ -> new LinkedHashMap<>());
				else
					m1.put(ss[i], v);
			}
		});
		return m;
	}

	@Override
	public String toString() {
		return parameters.stream().map(x -> URLEncoder.encode(x[0], StandardCharsets.UTF_8) + "="
				+ URLEncoder.encode(x[1], StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
	}

	public static void main(String[] args) {
		var u = URI.create("/foo?" + new UriQueryBuilder().append("foo", "bar").append("baz", "quz"));
		IO.println(u.getQuery());
	}
}

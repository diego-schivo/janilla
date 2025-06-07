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
package com.janilla.cms;

import java.io.IOException;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.ParameterizedType;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.janilla.http.HttpRequest;
import com.janilla.net.Net;
import com.janilla.reflect.Reflection;
import com.janilla.util.Util;

public class Cms {

	public static Map<String, Map<String, Map<String, Object>>> schema(Class<?> dataClass) {
		var m1 = new HashMap<String, Map<String, Map<String, Object>>>();
		var q = new ArrayDeque<Class<?>>();
		q.add(dataClass);
		Function<Class<?>, String> f = x -> x.getName().substring(x.getPackageName().length() + 1).replace('$', '.');
		var skip = Set.of("createdAt", "updatedAt", "documentStatus");
		do {
			var c = q.remove();
//			System.out.println("WebsiteTemplate.schema, c=" + c);
			var v = c.getAnnotation(Versions.class);
			var d = v != null && v.drafts();
			var m2 = new LinkedHashMap<String, Map<String, Object>>();
			Reflection.properties(c).filter(x -> !(skip.contains(x.name()) || (x.name().equals("publishedAt") && !d)))
					.forEach(x -> {
//				System.out.println("WebsiteTemplate.schema, x=" + x);
						var m3 = new LinkedHashMap<String, Object>();
						m3.put("type", f.apply(x.type().isEnum() ? String.class : x.type()));
						List<Class<?>> cc;
						if (x.type() == List.class) {
							var c2 = (Class<?>) ((ParameterizedType) x.genericType()).getActualTypeArguments()[0];
							var apt = x.annotatedType() instanceof AnnotatedParameterizedType y ? y : null;
							var ta = apt != null ? apt.getAnnotatedActualTypeArguments()[0].getAnnotation(Types.class)
									: null;
							if (c2 == Long.class) {
								cc = List.of();
								m3.put("elementTypes", List.of(f.apply(c2)));
								if (ta != null)
									m3.put("referenceType", f.apply(ta.value()[0]));
							} else {
								cc = ta != null ? Arrays.asList(ta.value())
										: c2.isInterface() ? Arrays.asList(c2.getPermittedSubclasses()) : List.of(c2);
								m3.put("elementTypes", cc.stream().map(f).toList());
							}
						} else if (x.type() == Set.class) {
							var c2 = (Class<?>) ((ParameterizedType) x.genericType()).getActualTypeArguments()[0];
							if (c2.isEnum()) {
								m3.put("elementTypes", List.of(f.apply(String.class)));
								m3.put("options",
										Arrays.stream(c2.getEnumConstants()).map(y -> ((Enum<?>) y).name()).toList());
							}
							cc = List.of();
						} else if (x.type().getPackageName().startsWith("java.")) {
							if (x.type() == Long.class) {
								var ta = x.annotatedType().getAnnotation(Types.class);
								if (ta != null)
									m3.put("referenceType", f.apply(ta.value()[0]));
							}
							cc = List.of();
						} else if (x.type() == Document.Reference.class) {
							var ta = x.annotatedType().getAnnotation(Types.class);
							if (ta != null)
								m3.put("referenceTypes", Arrays.stream(ta.value()).map(f).toList());
							cc = List.of();
						} else if (x.type().isEnum()) {
							m3.put("options",
									Arrays.stream(x.type().getEnumConstants()).map(y -> ((Enum<?>) y).name()).toList());
							cc = List.of();
						} else if (!m1.containsKey(f.apply(x.type())))
							cc = List.of(x.type());
						else
							cc = List.of();
						m2.put(x.name(), m3);
						q.addAll(cc);
					});
			m1.put(f.apply(c), m2);
		} while (!q.isEmpty());
		return m1.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, _) -> x, LinkedHashMap::new));
	}

	public static Map<String, byte[]> files(HttpRequest request) throws IOException {
		byte[][] bbb;
		{
			var b = request.getHeaderValue("content-type").split(";")[1].trim().substring("boundary=".length());
			var ch = (ReadableByteChannel) request.getBody();
			var bb = Channels.newInputStream(ch).readAllBytes();
			var s = ("--" + b).getBytes();
			var ii = Util.findIndexes(bb, s);
			bbb = IntStream.range(0, ii.length - 1)
					.mapToObj(i -> Arrays.copyOfRange(bb, ii[i] + s.length + 2, ii[i + 1] - 2)).toArray(byte[][]::new);
		}
		Map<String, byte[]> m = new LinkedHashMap<>();
		for (var bb : bbb) {
			var i = Util.findIndexes(bb, "\r\n\r\n".getBytes(), 1)[0];
			var hh = Net.parseEntryList(new String(bb, 0, i), "\r\n", ":");
			var n = Arrays.stream(hh.get("Content-Disposition").split(";")).map(String::trim)
					.filter(x -> x.startsWith("filename=")).map(x -> x.substring(x.indexOf('=') + 1))
					.map(x -> x.startsWith("\"") && x.endsWith("\"") ? x.substring(1, x.length() - 1) : x).findFirst()
					.orElseThrow();
			bb = Arrays.copyOfRange(bb, i + 4, bb.length);
			m.put(n, bb);
		}
		return m;
	}
}

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
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
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
import java.util.stream.IntStream;

import com.janilla.http.HttpRequest;
import com.janilla.http.MultipartFormData;
import com.janilla.net.Net;
import com.janilla.reflect.Reflection;

public class Cms {

	public static Map<String, Map<String, Map<String, Object>>> schema(Class<?> dataClass) {
		var m1 = new HashMap<String, Map<String, Map<String, Object>>>();
		var q = new ArrayDeque<Class<?>>();
		q.add(dataClass);
		Function<Type, String> n = t -> {
			if (t instanceof TypeVariable v)
				return v.getName();
			var c = (Class<?>) (t instanceof ParameterizedType pt ? pt.getRawType() : t);
			return c.getName().substring(c.getPackageName().length() + 1).replace('$', '.');
		};
//		var skip = Set.of("createdAt", "updatedAt", "documentStatus");
		var skip = Set.of("createdAt", "updatedAt", "documentStatus", "publishedAt");
		do {
			var c = q.remove();
//			IO.println("WebsiteTemplate.schema, c=" + c);
			var v = c.getAnnotation(Versions.class);
			var d = v != null && v.drafts();
			var m2 = new LinkedHashMap<String, Map<String, Object>>();
//			Reflection.properties(c).filter(x -> !(skip.contains(x.name()) || (x.name().equals("publishedAt") && !d)))
			Reflection.properties(c).filter(x -> !skip.contains(x.name()))
					.forEach(p -> {
//				IO.println("WebsiteTemplate.schema, x=" + x);
						var m3 = new LinkedHashMap<String, Object>();
						m3.put("type",
								n.apply(p.type() == Class.class || p.type().isEnum() ? String.class : p.genericType()));
						List<Class<?>> cc;
						if (p.type() == List.class) {
							var c2 = (Class<?>) ((ParameterizedType) p.genericType()).getActualTypeArguments()[0];
							var apt = p.annotatedType() instanceof AnnotatedParameterizedType y ? y : null;
							var ta = apt != null ? apt.getAnnotatedActualTypeArguments()[0].getAnnotation(Types.class)
									: null;
							if (c2 == Long.class) {
								cc = List.of();
								m3.put("elementTypes", List.of(n.apply(c2)));
								if (ta != null)
									m3.put("referenceType", n.apply(ta.value()[0]));
							} else {
								cc = ta != null ? Arrays.asList(ta.value())
										: c2.isInterface() ? Arrays.asList(c2.getPermittedSubclasses()) : List.of(c2);
								m3.put("elementTypes", cc.stream().map(n).toList());
							}
						} else if (p.type() == Set.class) {
							var c2 = (Class<?>) ((ParameterizedType) p.genericType()).getActualTypeArguments()[0];
							m3.put("elementTypes", List.of(n.apply(c2.isEnum() ? String.class : c2)));
							if (c2.isEnum())
//								m3.put("elementTypes", List.of(f.apply(String.class)));
								m3.put("options",
										Arrays.stream(c2.getEnumConstants()).map(y -> ((Enum<?>) y).name()).toList());
							cc = List.of();
						} else if (p.type().getPackageName().startsWith("java.")) {
							if (p.type() == Long.class) {
								var ta = p.annotatedType().getAnnotation(Types.class);
								if (ta != null)
									m3.put("referenceType", n.apply(ta.value()[0]));
							}
							cc = List.of();
						} else if (p.type() == DocumentReference.class) {
							var ta = p.annotatedType().getAnnotation(Types.class);
							if (ta != null)
								m3.put("referenceTypes", Arrays.stream(ta.value()).map(n).toList());
							cc = List.of();
						} else if (p.type().isEnum()) {
							m3.put("options",
									Arrays.stream(p.type().getEnumConstants()).map(y -> ((Enum<?>) y).name()).toList());
							cc = List.of();
						} else if (!m1.containsKey(n.apply(p.type())))
							cc = List.of(p.type());
						else
							cc = List.of();
						m2.put(p.name(), m3);
						q.addAll(cc);
					});
			m1.put(n.apply(c), m2);
		} while (!q.isEmpty());
		return m1.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(LinkedHashMap::new,
				(x, y) -> x.put(y.getKey(), y.getValue()), Map::putAll);
	}

	public static Map<String, byte[]> files(HttpRequest request) throws IOException {
		byte[][] bbb;
		{
			var b = request.getHeaderValue("content-type").split(";")[1].trim().substring("boundary=".length());
			var ch = (ReadableByteChannel) request.getBody();
			var bb = Channels.newInputStream(ch).readAllBytes();
			var s = ("--" + b).getBytes();
			var ii = MultipartFormData.findIndexes(bb, s);
			bbb = IntStream.range(0, ii.length - 1)
					.mapToObj(i -> Arrays.copyOfRange(bb, ii[i] + s.length + 2, ii[i + 1] - 2)).toArray(byte[][]::new);
		}
		Map<String, byte[]> m = new LinkedHashMap<>();
		for (var bb : bbb) {
			var i = MultipartFormData.findIndexes(bb, "\r\n\r\n".getBytes(), 1)[0];
//			var hh = Net.parseEntryList(new String(bb, 0, i), "\r\n", ":");
//			var n = Arrays.stream(hh.get("Content-Disposition").split(";")).map(String::trim)
//					.filter(x -> x.startsWith("filename=")).map(x -> x.substring(x.indexOf('=') + 1))
//					.map(x -> x.startsWith("\"") && x.endsWith("\"") ? x.substring(1, x.length() - 1) : x).findFirst()
//					.orElseThrow();
//			bb = Arrays.copyOfRange(bb, i + 4, bb.length);
//			m.put(n, bb);
			throw new RuntimeException();
		}
		return m;
	}
}

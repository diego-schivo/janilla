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
package com.janilla.util;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import com.janilla.io.IO;
import com.janilla.json.Json;
import com.janilla.reflect.Reflection;

public interface Util {

	static Class<?> getClass(Path p) {
		var t = !Files.isDirectory(p) ? p.toString() : null;
		t = t != null && t.endsWith(".class") ? t : null;
		t = t != null ? t.substring(0, t.length() - ".class".length()).replace(File.separatorChar, '.') : null;
		Class<?> c;
		try {
			c = t != null ? Class.forName(t) : null;
		} catch (ClassNotFoundException e) {
			c = null;
		}
		return c;
	}

	static Stream<Class<?>> getPackageClasses(String package1) {
		var b = Stream.<Class<?>>builder();
		IO.acceptPackageFiles(package1, f -> {
			var c = Util.getClass(f);
			if (c != null)
				b.add(c);
		});
		return b.build();
	}

	static String capitalizeFirstChar(String string) {
		if (string == null || string.length() == 0)
			return string;
		return string.substring(0, 1).toUpperCase() + string.substring(1);
	}

	static boolean startsWithIgnoreCase(String string, String prefix) {
		return string == prefix || (prefix != null && prefix.length() <= string.length()
				&& string.regionMatches(true, 0, prefix, 0, prefix.length()));
	}

	static Map<String, Object> buildTree(EntryList<String, String> input) {
		var m = new LinkedHashMap<String, Object>();
		for (var t : input) {
			Map<String, Object> n = m;
			var k = t.getKey().split("\\.");
			for (var i = 0; i < k.length; i++) {
				if (k[i].endsWith("]")) {
					var l = k[i].split("\\[", 2);
					var j = Integer.parseInt(l[1].substring(0, l[1].length() - 1));
					@SuppressWarnings("unchecked")
					var a = (List<Object>) n.computeIfAbsent(l[0], x -> new ArrayList<Object>());
					while (a.size() <= j)
						a.add(null);
					if (i < k.length - 1) {
						@SuppressWarnings("unchecked")
						var o = (Map<String, Object>) a.get(j);
						if (o == null) {
							o = new LinkedHashMap<String, Object>();
							a.set(j, o);
						}
						n = o;
					} else
						a.set(j, t.getValue());
				} else if (i < k.length - 1) {
					@SuppressWarnings("unchecked")
					var o = (Map<String, Object>) n.computeIfAbsent(k[i], x -> new LinkedHashMap<String, Object>());
					n = o;
				} else
					n.put(k[i], t.getValue());
			}
		}
		return m;
	}

	static <T> T convertTree(Map<String, Object> tree, Class<T> target) {
		BiFunction<String, Type, Object> c = (name, type) -> {
			var i = tree.get(name);
			var o = i != null ? switch (i) {
			case List<?> l -> {
				var u = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
				var m = new ArrayList<>();
				for (var e : l) {
					@SuppressWarnings("unchecked")
					var f = (Map<String, Object>) e;
					m.add(convertTree(f, u));
				}
				yield m;
			}
			case Map<?, ?> m -> {
				@SuppressWarnings("unchecked")
				var n = (Map<String, Object>) m;
				yield convertTree(n, (Class<?>) type);
			}
			default -> Json.convert(i, (Class<?>) type);
			} : Json.convert(i, (Class<?>) type);
			return o;
		};

		try {
			if (target.isRecord()) {
				var a = new ArrayList<Object>();
				for (var d : target.getRecordComponents()) {
					var n = d.getName();
					var t = d.getGenericType();
					var b = c.apply(n, t);
					a.add(b);
				}
				@SuppressWarnings("unchecked")
				var t = (T) target.getConstructors()[0].newInstance(a.toArray());
				return t;
			}
			var t = (T) target.getConstructor().newInstance();
			for (var i = Reflection.properties(target).map(n -> {
				var s = Reflection.setter(target, n);
				return s != null ? Map.entry(n, s) : null;
			}).filter(Objects::nonNull).iterator();i.hasNext();) {
				var e = i.next();
				var n = e.getKey();
				var s = e.getValue();
				var u = s.getGenericParameterTypes()[0];
				var b = c.apply(n, u);
				if (b != null)
					s.invoke(t, b);
			}
			return t;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}

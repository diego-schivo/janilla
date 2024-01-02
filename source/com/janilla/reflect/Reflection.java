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
package com.janilla.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Reflection {

	private static Map<Class<?>, Map<String, Method[]>> methods = new ConcurrentHashMap<>();

	public static Stream<String> properties(Class<?> class1) {
		var m = methods.computeIfAbsent(class1, Reflection::compute);
		return m.keySet().stream();
	}

	public static Method getter(Class<?> class1, String name) {
		var m = methods.computeIfAbsent(class1, Reflection::compute);
		var a = m.get(name);
		return a != null ? a[0] : null;
	}

	public static Method setter(Class<?> class1, String name) {
		var m = methods.computeIfAbsent(class1, Reflection::compute);
		var a = m.get(name);
		return a != null ? a[1] : null;
	}

	public static void copy(Object source, Object destination) {
		copy(source, destination, null);
	}

	public static void copy(Object source, Object destination, Predicate<String> filter) {
		var c = source.getClass();
		var d = destination.getClass();
		for (var i = properties(d).iterator(); i.hasNext();) {
			var n = i.next();
			var s = setter(d, n);
			if (s == null || (filter != null && !filter.test(n)))
				continue;
			var g = getter(c, n);
			if (g == null)
				continue;
			try {
				var v = g.invoke(source);
				s.invoke(destination, v);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static Map<String, Method[]> compute(Class<?> class1) {
		var a = Stream.<Class<?>>iterate(class1, c -> c != null && c != Object.class, c -> c.getSuperclass())
				.toArray(Class[]::new);
		var m = new HashMap<String, Method[]>();
		for (var i = a.length - 1; i >= 0; i--)
			for (var n : a[i].getDeclaredMethods()) {
				if (switch (n.getName()) {
				case "getClass", "hashCode", "toString" -> true;
				default -> false;
				})
					continue;
				if (Modifier.isPublic(n.getModifiers())) {
					var g = n.getReturnType() != Void.TYPE && n.getParameterCount() == 0 ? n : null;
					var s = n.getReturnType() == Void.TYPE && n.getParameterCount() == 1 ? n : null;
					if (g != null || s != null) {
						var k = n.getName();
						if (k.length() > 3 && k.startsWith(g != null ? "get" : "set")
								&& Character.isUpperCase(k.charAt(3)))
							k = Character.toLowerCase(k.charAt(3)) + k.substring(4);
						var b = m.computeIfAbsent(k, l -> new Method[2]);
						if (g != null)
							b[0] = g;
						if (s != null)
							b[1] = s;
					}
				}
			}
		return m;
	}
}

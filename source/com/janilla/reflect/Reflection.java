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
			var s = filter == null || filter.test(n) ? setter(d, n) : null;
			var g = s != null ? getter(c, n) : null;
			if (g != null)
				try {
					var v = g.invoke(source);
					s.invoke(destination, v);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
		}
	}

	private static Map<String, Method[]> compute(Class<?> class1) {
		var m = new HashMap<String, Method[]>();
		for (var n : class1.getMethods()) {
			if (Modifier.isStatic(n.getModifiers()) || n.getDeclaringClass() == Object.class || switch (n.getName()) {
			case "hashCode", "toString" -> true;
			default -> false;
			})
				continue;
			var g = n.getReturnType() != Void.TYPE && n.getParameterCount() == 0 ? n : null;
			var s = n.getReturnType() == Void.TYPE && n.getParameterCount() == 1 ? n : null;
			if (g != null || s != null) {
				var k = n.getName();
				var p = g != null ? (n.getReturnType() == Boolean.TYPE ? "is" : "get") : "set";
				if (k.length() > p.length() && k.startsWith(p) && Character.isUpperCase(k.charAt(p.length()))) {
					var i = p.length();
					do {
						i++;
					} while (i < k.length() && Character.isUpperCase(k.charAt(i)));
					k = k.substring(p.length(), i).toLowerCase() + k.substring(i);
				}
				var b = m.computeIfAbsent(k, l -> new Method[2]);
				if (g != null)
					b[0] = g;
				if (s != null)
					b[1] = s;
			}
		}
		return m;
	}
}

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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Reflection {

	private static Map<Class<?>, Map<String, Property>> properties = new ConcurrentHashMap<>();

	public static Stream<String> properties(Class<?> class1) {
		var m = properties.computeIfAbsent(class1, Reflection::compute);
		return m.keySet().stream();
	}

	public static Property property(Class<?> class1, String name) {
		var m = properties.computeIfAbsent(class1, Reflection::compute);
		var a = m.get(name);
		return a;
	}

	public static void copy(Object source, Object destination) {
		copy(source, destination, null);
	}

	public static void copy(Object source, Object destination, Predicate<String> filter) {
		var c = source.getClass();
		var d = destination.getClass();
		for (var i = properties(d).iterator(); i.hasNext();) {
			var n = i.next();
			var s = filter == null || filter.test(n) ? property(d, n) : null;
			var g = s != null ? property(c, n) : null;
			if (g != null)
				try {
					var v = g.get(source);
					s.set(destination, v);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
		}
	}

	private static Map<String, Property> compute(Class<?> class1) {
		var m1 = new HashMap<String, Member[]>();
		for (var m : class1.getMethods()) {
			if (Modifier.isStatic(m.getModifiers()) || m.getDeclaringClass() == Object.class || switch (m.getName()) {
			case "hashCode", "toString" -> true;
			default -> false;
			})
				continue;
			var g = m.getReturnType() != Void.TYPE && m.getParameterCount() == 0 ? m : null;
			var s = m.getReturnType() == Void.TYPE && m.getParameterCount() == 1 ? m : null;
			if (g != null || s != null) {
				var k = Property.name(m);
				var b = m1.computeIfAbsent(k, l -> new Member[2]);
				if (g != null)
					b[0] = g;
				if (s != null)
					b[1] = s;
			}
		}
		for (var f : class1.getFields()) {
			if (Modifier.isStatic(f.getModifiers()))
				continue;
			m1.computeIfAbsent(Property.name(f), l -> new Member[] { f });
		}
		var m2 = m1.entrySet().stream().map(e -> {
			var mm = e.getValue();
			var p = mm.length == 1 ? Property.of((Field) mm[0]) : Property.of((Method) mm[0], (Method) mm[1]);
			return Map.entry(e.getKey(), p);
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return m2.keySet().stream().map(n -> {
			Field f;
			try {
				f = class1.getDeclaredField(n);
			} catch (NoSuchFieldException e) {
				f = null;
			}
			var o = f != null ? f.getAnnotation(Order.class) : null;
			return new SimpleEntry<>(n, o != null ? o.value() : null);
		}).sorted(Comparator.comparing(Map.Entry::getValue, Comparator.nullsLast(Comparator.naturalOrder())))
				.collect(Collectors.toMap(e -> e.getKey(), e -> m2.get(e.getKey()), (v, w) -> v, LinkedHashMap::new));
	}
}

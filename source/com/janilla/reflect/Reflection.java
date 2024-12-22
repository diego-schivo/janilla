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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Reflection {

	private static Map<Class<?>, Map<String, Property>> properties = new ConcurrentHashMap<>();

	public static Stream<String> properties(Class<?> class1) {
//		System.out.println("Reflection.properties, class1=" + class1);
		var m = properties.computeIfAbsent(class1, Reflection::compute);
		return m.keySet().stream();
	}

	public static Stream<Property> properties2(Class<?> class1) {
//		System.out.println("Reflection.properties2, class1=" + class1);
		var m = properties.computeIfAbsent(class1, Reflection::compute);
		return m.values().stream();
	}

	public static Property property(Class<?> class1, String name) {
//		System.out.println("Reflection.property, class1=" + class1);
		var m = properties.computeIfAbsent(class1, Reflection::compute);
		return m.get(name);
	}

	public static <T> T copy(Object source, T destination) {
		return copy(source, destination, null);
	}

	public static <T> T copy(Object source, T destination, Predicate<String> filter) {
		var c1 = source.getClass();
		var c2 = destination.getClass();
		if (c2.isRecord()) {
			var cc = c2.getRecordComponents();
			var aa = new Object[cc.length];
			for (var i = 0; i < cc.length; i++) {
				var n = cc[i].getName();
				var g = filter == null || filter.test(n) ? property(c1, n) : null;
				aa[i] = g != null ? g.get(source) : property(c2, n).get(destination);
			}
			try {
				@SuppressWarnings("unchecked")
				var t = (T) c2.getConstructors()[0].newInstance(aa);
				return t;
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}
		for (var i = properties(c2).iterator(); i.hasNext();) {
			var n = i.next();
			var s = filter == null || filter.test(n) ? property(c2, n) : null;
			var g = s != null ? property(c1, n) : null;
			if (g != null)
				s.set(destination, g.get(source));
		}
		return destination;
	}

	private static Map<String, Property> compute(Class<?> class1) {
//		System.out.println("Reflection.compute, class1=" + class1);
		if (!Modifier.isPublic(class1.getModifiers())) {
			if (Map.Entry.class.isAssignableFrom(class1))
				class1 = Map.Entry.class;
			else
				throw new IllegalArgumentException();
		}
		var mm = new HashMap<String, Member[]>();
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
				var b = mm.computeIfAbsent(k, l -> new Member[2]);
				if (g != null)
					b[0] = g;
				if (s != null)
					b[1] = s;
			}
		}
		for (var f : class1.getFields()) {
			if (Modifier.isStatic(f.getModifiers()))
				continue;
			mm.computeIfAbsent(Property.name(f), l -> new Member[] { f });
		}
		if (class1.isArray())
			mm.computeIfAbsent("length", l -> {
				Method m;
				try {
					m = Array.class.getMethod("getLength", Object.class);
				} catch (NoSuchMethodException e) {
					throw new RuntimeException(e);
				}
				return new Member[] { m, null };
			});
		var cc = class1.isRecord() ? class1.getRecordComponents() : null;
		var oo = cc != null
				? IntStream.range(0, cc.length).boxed().collect(Collectors.toMap(i -> cc[i].getName(), i -> i + 1))
				: null;
		var c = class1;
		return mm.values().stream()
				.map(x -> x.length == 1 ? Property.of((Field) x[0]) : Property.of((Method) x[0], (Method) x[1]))
				.map(p -> {
					Field f;
					try {
						f = c.getDeclaredField(p.getName());
					} catch (NoSuchFieldException e) {
						f = null;
					}
					var o = f != null ? f.getAnnotation(Order.class) : null;
					return new AbstractMap.SimpleEntry<>(p,
							o != null ? Integer.valueOf(o.value()) : oo != null ? oo.get(p.getName()) : null);
				}).sorted(Comparator.comparing(Map.Entry::getValue, Comparator.nullsLast(Comparator.naturalOrder())))
				.map(Map.Entry::getKey).flatMap(p -> {
					Field f;
					try {
						f = c.getDeclaredField(p.getName());
					} catch (NoSuchFieldException e) {
						f = null;
					}
					return f != null && f.isAnnotationPresent(Flatten.class) ? properties2(f.getType())
							.filter(q -> !mm.containsKey(q.getName())).map(q -> Property.of(p, q)) : Stream.of(p);
				}).collect(Collectors.toMap(Property::getName, p -> p, (v, w) -> v, LinkedHashMap::new));
	}
}

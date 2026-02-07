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
package com.janilla.web;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.java.Reflection;

public class InvocationResolver {

	public static final ScopedValue<InvocationResolver> INSTANCE = ScopedValue.newInstance();

	protected final Map<String, InvocationGroup> groups;

	protected final Comparator<Invocation> invocationComparator;

	protected final Map<Pattern, InvocationGroup> regexGroups;

	public InvocationResolver(List<Invocable> invocables, Function<Class<?>, Object> instanceResolver,
			Comparator<Invocation> invocationComparator) {
		this.invocationComparator = invocationComparator;

		record A(String m1, Method m2) {
		}
		record B(String p, Class<?> t, List<A> aa) {
		}
		var bb = invocables.stream().map(tm -> {
//			IO.println("InvocationHandlerFactory, tm=" + tm);
			var t = tm.type();
			var m = tm.method();
			var h1 = t.getAnnotation(Handle.class);
			var p1 = h1 != null ? h1.path() : null;
			var h2 = Reflection.inheritedAnnotation(m, Handle.class);
			var p2 = h2 != null ? h2.path() : null;
//			IO.println("InvocationHandlerFactory, h1=" + h1 + ", h2=" + h2);
			if (p2 != null) {
				if (p2.startsWith("/"))
					p1 = null;
				var p = Stream.of(p1, p2).filter(x -> x != null && !x.isEmpty()).collect(Collectors.joining("/"));
//				IO.println("InvocationHandlerFactory, p=" + p + ", m=" + m + ", h2=" + h2);
				return new B(p, tm.type(), new ArrayList<>(List.of(new A(h2.method(), tm.method()))));
			} else
				return null;
		}).filter(Objects::nonNull).collect(Collectors.toMap(B::p, x -> x, (x, y) -> {
			if (x.t == y.t) {
				x.aa.addAll(y.aa);
				return x;
			}
			return y;
		}, LinkedHashMap::new));
		var oo = new HashMap<Class<?>, Object>();
		groups = ScopedValue.where(INSTANCE, this)
				.call(() -> bb.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> {
					var b = x.getValue();
					var o = oo.computeIfAbsent(b.t, y -> {
						if (instanceResolver != null)
							return instanceResolver.apply(y);
						try {
							return y.getConstructor().newInstance();
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException(e);
						}
					});
					return new InvocationGroup(o, b.aa.stream().collect(Collectors.toMap(y -> y.m1, y -> y.m2,
							(y1, y2) -> y1.getDeclaringClass().isAssignableFrom(y2.getDeclaringClass()) ? y2 : y1,
							LinkedHashMap::new)));
				})));

		var kk = groups.keySet().stream().filter(k -> k.contains("(") && k.contains(")")).toList();
		regexGroups = kk.stream().sorted(Comparator.comparingInt((String x) -> x.indexOf('(')).reversed())
				.collect(Collectors.toMap(k -> Pattern.compile(k), groups::get, (_, x) -> x, LinkedHashMap::new));
		groups.keySet().removeAll(kk);
//		IO.println("m=" + m + "\nx=" + x);
	}

	public Stream<Invocation> lookup(String method, String path) {
		var s = groups(path).map(i -> {
			var m = i.methods().get(method);
			if (m == null)
				m = i.methods().get("");
			return m != null ? new Invocation(i.object(), m, i.regexGroups()) : null;
		}).filter(Objects::nonNull);
		return invocationComparator != null ? s.sorted(invocationComparator) : s;
	}

	public Stream<InvocationGroup> groups(String path) {
		if (path == null)
			return Stream.empty();
		var a = Optional.ofNullable(groups.get(path)).stream();
		var b = regexGroups.entrySet().stream().map(x -> {
			var m = x.getKey().matcher(path);
			if (m.matches()) {
				var ig = x.getValue();
				var ss = IntStream.range(1, 1 + m.groupCount()).mapToObj(m::group).toArray(String[]::new);
				return ss.length != 0 ? ig.withRegexGroups(ss) : ig;
			}
			return null;
		}).filter(Objects::nonNull);
		return Stream.concat(a, b);
	}
}

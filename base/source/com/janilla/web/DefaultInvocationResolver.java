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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.java.Scope;
import com.janilla.java.JavaReflect;

public class DefaultInvocationResolver implements InvocationResolver {

	private static final Logger LOGGER = System.getLogger(DefaultInvocationResolver.class.getName());

	protected final Function<Class<?>, Object> instanceResolver;

	protected final Comparator<Invocation> invocationComparator;

	protected final Predicate<MethodChoice> methodPredicate;

	protected final Map<String, Entry1> map;

	protected Map<String, InvocationGroup> groups;

	protected Map<Pattern, InvocationGroup> regexGroups;

	public DefaultInvocationResolver(List<Invocable> invocables, Function<Class<?>, Object> instanceResolver,
			Comparator<Invocation> invocationComparator, Predicate<MethodChoice> methodPredicate) {
		LOGGER.log(Level.DEBUG, "invocables={0}", invocables);

		this.instanceResolver = instanceResolver;
		this.invocationComparator = invocationComparator != null ? invocationComparator : (_, _) -> 0;
		this.methodPredicate = methodPredicate != null ? methodPredicate : _ -> true;

		map = invocables.stream().map(tm -> {
//			IO.println("InvocationHandlerFactory, tm=" + tm);
			var t = tm.type();
			var m = tm.method();

			var h1 = t.getAnnotation(Handle.class);
			var p1 = h1 != null ? h1.path() : null;

			var aa = JavaReflect.inheritedAnnotation(m, Handle.class);
			var p2 = aa != null ? aa.annotation().path() : null;
			var s2 = aa != null ? aa.annotated().getAnnotation(Scope.class) : null;
//			IO.println("InvocationHandlerFactory, h1=" + h1 + ", h2=" + h2);

			if (p2 != null) {
				if (p2.startsWith("/"))
					p1 = null;
				var p = Stream.of(p1, p2).filter(x -> x != null && !x.isEmpty()).collect(Collectors.joining("/"));
//				IO.println("InvocationHandlerFactory, p=" + p + ", m=" + m + ", h2=" + h2);
				return new Entry1(p, tm.type(), List.of(new Entry2(aa.annotation().method(),
						List.of(new MethodChoice(tm.method(), s2 != null ? Set.of(s2.value()) : Set.of())))));
			} else
				return null;
		}).filter(Objects::nonNull).collect(Collectors.toMap(Entry1::path, x -> x, (a, b) -> {
			if (a.type == b.type) {
				var m = Stream.of(a, b).flatMap(x -> x.entries.stream())
						.collect(Collectors.toMap(Entry2::method, x -> x.entries, (a2, b2) -> {
							var mm = a2 instanceof ArrayList<MethodChoice> x ? x : new ArrayList<>(a2);
							mm.addAll(b2);
							return mm;
						}, LinkedHashMap::new));
				return new Entry1(a.path, a.type,
						m.entrySet().stream().map(x -> new Entry2(x.getKey(), x.getValue())).toList());
			}
			return b;
		}, LinkedHashMap::new));
	}

	@Override
	public Stream<Invocation> lookup(String method, String path) {
		LOGGER.log(Level.DEBUG, "method={0}, path={1}", method, path);

		var ii = groups(path).map(g -> {
			var cc = g.choices().get(method);
			if (cc == null)
				cc = g.choices().get("");
			LOGGER.log(Level.DEBUG, "cc={0}", cc);

			cc = new ArrayList<>(cc != null ? cc : List.of());
			Collections.reverse(cc);
			return cc.stream().filter(methodPredicate)
					.map(m -> new Invocation(g.instance(), m.method(), g.regexGroups())).findFirst().orElse(null);
		}).filter(Objects::nonNull).sorted(invocationComparator);
		return ii;
	}

	@Override
	public Stream<InvocationGroup> groups(String path) {
		if (path == null)
			return Stream.empty();

		if (groups == null)
			initGroups();

		var gg1 = Optional.ofNullable(groups.get(path)).stream();
		var gg2 = regexGroups.entrySet().stream().map(x -> {
			var m = x.getKey().matcher(path);
			if (m.matches()) {
				var g = x.getValue();
				var ss = IntStream.range(1, 1 + m.groupCount()).mapToObj(m::group).toArray(String[]::new);
				return ss.length != 0 ? g.withRegexGroups(ss) : g;
			}
			return null;
		}).filter(Objects::nonNull);
		return Stream.concat(gg1, gg2);
	}

	synchronized void initGroups() {
		if (groups != null)
			return;

		var ii = new HashMap<Class<?>, Object>();
		groups = map.values().stream().collect(Collectors.toMap(Entry1::path, e1 -> {
			var i = ii.computeIfAbsent(e1.type,
//					y -> {
//				if (instanceResolver != null)
//					return instanceResolver.apply(y);
//				try {
//					return y.getConstructor().newInstance();
//				} catch (ReflectiveOperationException e) {
//					throw new RuntimeException(e);
//				}
//			}
					instanceResolver);
			return new InvocationGroup(i, e1.entries.stream()
					.collect(Collectors.toMap(e2 -> e2.method, e2 -> e2.entries, (_, x) -> x, LinkedHashMap::new)));
		}));

		var kk = groups.keySet().stream().filter(p -> p.contains("(") && p.contains(")")).toList();
		regexGroups = kk.stream().sorted(Comparator.comparingInt((String x) -> x.indexOf('(')).reversed())
				.collect(Collectors.toMap(k -> Pattern.compile(k), groups::get, (_, x) -> x, LinkedHashMap::new));
		groups.keySet().removeAll(kk);
//		IO.println("DefaultInvocationResolver.initGroups, groups=" + groups);
	}

	record Entry1(String path, Class<?> type, List<Entry2> entries) {
	}

	record Entry2(String method, List<MethodChoice> entries) {
	}
}

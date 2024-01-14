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
package com.janilla.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.http.HttpMessageReadableByteChannel;
import com.janilla.http.HttpRequest;
import com.janilla.util.Lazy;

public class AnnotationDrivenToMethodInvocation implements Function<HttpRequest, MethodInvocation> {

	public static void main(String[] args) throws IOException {
		class C {

			@Handle(method = "GET", uri = "/foo/(.*)")
			public void foo(Path path) {
			}

			@Override
			public int hashCode() {
				return 0;
			}

			@Override
			public boolean equals(Object obj) {
				return obj != null && obj.getClass() == C.class;
			}
		}
		var f = new AnnotationDrivenToMethodInvocation() {

			@Override
			protected Object getInstance(Class<?> c) {
				return new C();
			}
		};
		f.setTypes(Collections.singleton(C.class));

		var is = new ByteArrayInputStream("""
				GET /foo/bar HTTP/1.1\r
				Content-Length: 0\r
				\r
				""".getBytes());
		var rc = new HttpMessageReadableByteChannel(Channels.newChannel(is));
		MethodInvocation i;
		try (var rq = rc.readRequest()) {
			i = f.apply(rq);
		}
		System.out.println(i);

		Method m;
		try {
			m = C.class.getMethod("foo", Path.class);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		var j = new MethodInvocation(m, new C(), List.of("bar"));
		assert i.equals(j) : i;
	}

	private Iterable<Class<?>> types;

	Supplier<Map<String, Value>> invocations1 = Lazy.of(() -> {
		Map<String, Value> m = new HashMap<>();
		for (var t : types) {
			if (Modifier.isInterface(t.getModifiers()) || Modifier.isAbstract(t.getModifiers()))
				continue;

			Object o = null;
			for (var n : t.getMethods()) {
				if (!n.isAnnotationPresent(Handle.class))
					continue;

				if (o == null)
					o = getInstance(t);

				var p = o;
				var v = m.computeIfAbsent(n.getAnnotation(Handle.class).uri(), k -> new Value(new HashSet<>(), p));
				v.methods().add(n);
			}
		}
		return m;
	});

	Supplier<Map<Pattern, Value>> invocations2 = Lazy.of(() -> {
		var m = invocations1.get();
		var s = m.keySet().stream().filter(k -> k.contains("(")).collect(Collectors.toSet());
		var x = s.stream().collect(Collectors.toMap(k -> Pattern.compile(k), m::get));
		m.keySet().removeAll(s);
		return x;
	});

	public void setTypes(Iterable<Class<?>> types) {
		this.types = types;
	}

	@Override
	public MethodInvocation apply(HttpRequest r) {
		return getValueAndGroupsStream(r).map(w -> {
			var v = w.value();
			var m = v.method(r);
			return m != null ? new MethodInvocation(m, v.object(), w.groups()) : null;
		}).filter(Objects::nonNull).findFirst().orElse(null);
	}

	public Stream<ValueAndGroups> getValueAndGroupsStream(HttpRequest q) {
		var i = invocations1.get();
		var j = invocations2.get();
//		System.out.println(
//				Thread.currentThread().getName() + " ToEndpointInvocation invocations1=" + i + ", invocations2=" + j);

		var b = Stream.<ValueAndGroups>builder();

		URI u;
		try {
			u = q.getURI();
		} catch (NullPointerException e) {
			u = null;
		}
		var p = u != null ? u.getPath() : null;
		var v = p != null ? i.get(p) : null;
		if (v != null)
			b.add(new ValueAndGroups(v, null));

		for (var e : j.entrySet()) {
			var m = e.getKey().matcher(p);
			if (m.matches()) {
				v = e.getValue();
				var s = IntStream.range(1, 1 + m.groupCount()).mapToObj(m::group).toList();
				b.add(new ValueAndGroups(v, s));
			}
		}

		return b.build();
	}

	protected Object getInstance(Class<?> c) {
		try {
			return c.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public record Value(Set<Method> methods, Object object) {

		Method method(HttpRequest request) {
			var s = request.getMethod() != null ? request.getMethod().name() : null;
			if (s == null)
				s = "";
			Method m = null;
			for (var n : methods()) {
				var t = n.getAnnotation(Handle.class).method();
				if (t == null)
					t = "";
				if (t.equalsIgnoreCase(s))
					return n;
				if (t.isEmpty())
					m = n;
			}
			return m;
		}
	}

	public record ValueAndGroups(Value value, List<String> groups) {
	}
}

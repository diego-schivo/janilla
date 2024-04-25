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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.io.IO;
import com.janilla.json.Json;
import com.janilla.net.Net;
import com.janilla.reflect.Parameter;
import com.janilla.reflect.Reflection;
import com.janilla.util.EntryList;
import com.janilla.util.EntryTree;

public class MethodArgumentsResolver implements BiFunction<MethodInvocation, HttpExchange, Object[]> {

	Function<String, Class<?>> typeResolver;

	public void setTypeResolver(Function<String, Class<?>> typeResolver) {
		this.typeResolver = typeResolver;
	}

	@Override
	public Object[] apply(MethodInvocation i, HttpExchange c) {
		var q = Net.parseQueryString(c.getRequest().getURI().getQuery());
		IO.Supplier<String> b = switch (c.getRequest().getMethod().name()) {
		case "POST", "PUT" -> {
			var s = IO.Lazy.of(() -> {
				var d = (ReadableByteChannel) c.getRequest().getBody();
				return new String(Channels.newInputStream(d).readAllBytes());
			});
			var t = c.getRequest().getHeaders().get("Content-Type");
			if (t != null)
				switch (t.split(";")[0]) {
				case "application/x-www-form-urlencoded":
					EntryList<String, String> v;
					try {
						v = Net.parseQueryString(s.get());
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					if (v == null)
						;
					else if (q == null)
						q = v;
					else
						q.addAll(v);
					break;
				case "application/json":
					Object o;
					try {
						o = Json.parse(s.get());
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					if (o instanceof Map<?, ?> m && !m.isEmpty()) {
						if (q == null)
							q = new EntryList<>();
						for (var e : m.entrySet())
							q.add(e.getKey().toString(), e.getValue().toString());
					}
					break;
				}
			yield s;
		}
		default -> null;
		};

		var m = i.method();
		var g = i.groups();
		var t = m.getParameterTypes();
		var u = m.getParameterAnnotations();
		var n = Arrays.stream(u)
				.map(a -> Arrays.stream(a).filter(e -> e.annotationType() == Parameter.class).findFirst().orElse(null))
				.toArray(Parameter[]::new);
		var a = new Object[t.length];
		for (var j = 0; j < a.length; j++) {
			var h = g != null && j < g.size() ? g.get(j) : null;
			var k = h == null && n[j] != null ? n[j].name() : null;
			var w = k != null ? q : null;
			a[j] = resolveArgument(t[j], c, () -> h != null ? new String[] { h }
					: w != null
							? w.stream().filter(f -> f.getKey().equals(k)).map(Entry::getValue).toArray(String[]::new)
							: null,
					q, b);
		}
		return a;
	}

	protected Object resolveArgument(Type type, HttpExchange exchange, Supplier<String[]> values,
			EntryList<String, String> entries, IO.Supplier<String> body) {
		var c = type instanceof Class<?> x ? x : null;
		if (c != null && HttpExchange.class.isAssignableFrom(c))
			return exchange;
		if (c != null && HttpRequest.class.isAssignableFrom(c))
			return exchange.getRequest();
		if (c != null && HttpResponse.class.isAssignableFrom(c))
			return exchange.getResponse();
		if (c != null && !c.getPackageName().startsWith("java."))
			switch (Objects.toString(exchange.getRequest().getHeaders().get("Content-Type"), "").split(";")[0]) {
			case "application/json":
//				System.out.println("body=" + body);
				try {
					return body != null ? Json.convert(Json.parse(body.get()), c, typeResolver) : null;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			case "application/x-www-form-urlencoded": {
				var t = newEntryTree();
				t.setTypeResolver(typeResolver);
				entries.forEach(t::add);
				return t.convert(c);
			}
			default:
				try {
					if (c.isRecord()) {
						var m = Arrays.stream(c.getRecordComponents()).collect(
								Collectors.toMap(x -> x.getName(), x -> x.getType(), (x, y) -> x, LinkedHashMap::new));
						var a = m.entrySet().stream().map(e -> {
							var k = e.getKey();
							var t = e.getValue();
							var w = entries;
							return resolveArgument(t, exchange,
									() -> w != null ? w.stream().filter(f -> f.getKey().equals(k)).map(Entry::getValue)
											.toArray(String[]::new) : null,
									entries, body);
						}).toArray();
						var o = c.getConstructors()[0].newInstance(a);
						return o;
					} else {
						Constructor<?> d;
						try {
							d = c.getConstructor();
						} catch (NoSuchMethodException e) {
							d = null;
						}
						if (d != null) {
							var o = d.newInstance();
							for (var i = Reflection.properties(c).iterator(); i.hasNext();) {
								var n = i.next();
								var s = Reflection.property(c, n);
								if (s == null)
									continue;
								var w = entries;
								var v = resolveArgument((Type) s.getType(), exchange,
										() -> w != null
												? w.stream().filter(f -> f.getKey().equals(n)).map(Entry::getValue)
														.toArray(String[]::new)
												: null,
										entries, body);
								s.set(o, v);
							}
							return o;
						}
					}
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
				break;
			}
		var a = values.get();
		return parseParameter(a, type);
	}

	protected Object parseParameter(String[] strings, Type type) {
		var c = (Class<?>) type;
		var i = c.isArray() || Collection.class.isAssignableFrom(c) ? strings
				: (strings != null && strings.length > 0 ? strings[0] : null);
		return Json.convert(i, c, typeResolver);
	}

	protected EntryTree newEntryTree() {
		return new EntryTree();
	}
}

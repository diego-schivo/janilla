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
import java.lang.reflect.Type;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.janilla.http.ExchangeContext;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.json.Json;
import com.janilla.net.Net;
import com.janilla.reflect.Parameter;
import com.janilla.util.EntryList;

public class MethodArgumentsResolver implements BiFunction<Invocation, ExchangeContext, Object[]> {

	@Override
	public Object[] apply(Invocation i, ExchangeContext c) {
		var q = Net.parseQueryString(c.getRequest().getURI().getQuery());
		var b = switch (c.getRequest().getMethod().name()) {
		case "POST", "PUT" -> {
			var d = (ReadableByteChannel) c.getRequest().getBody();
			String s;
			try {
				s = new String(Channels.newInputStream(d).readAllBytes());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			switch (Objects.toString(c.getRequest().getHeaders().get("Content-Type"), "")) {
			case "application/x-www-form-urlencoded":
				var v = Net.parseQueryString(s);
				if (v == null)
					;
				else if (q == null)
					q = v;
				else
					q.addAll(v);
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
			var k = h == null && n[j] != null ? n[j].value() : null;
			var w = k != null ? q : null;
			a[j] = resolveArgument(t[j], n[j], c, () -> h != null ? new String[] { h }
					: w != null
							? w.stream().filter(f -> f.getKey().equals(k)).map(Entry::getValue).toArray(String[]::new)
							: null,
					q, b);
		}
		return a;
	}

	protected Object resolveArgument(Type type, Parameter parameter, ExchangeContext context, Supplier<String[]> values,
			EntryList<String, String> entries, String body) {
		if (type == HttpRequest.class)
			return context.getRequest();
		if (type == HttpResponse.class)
			return context.getResponse();
		switch (Objects.toString(context.getRequest().getHeaders().get("Content-Type"), "")) {
		case "application/json":
			if (type instanceof Class<?> c && c != String.class)
				return Json.parse(body, Json.parseCollector(c));
			break;
		default:
			if (type instanceof Class c && c.isRecord()) {
				var d = c.getConstructors()[0];
				var t = d.getParameterTypes();
				var u = d.getParameterAnnotations();

				var n = Arrays.stream(u).map(a -> Arrays.stream(a).filter(e -> e.annotationType() == Parameter.class)
						.findFirst().orElse(null)).toArray(Parameter[]::new);
				var a = new Object[t.length];
				for (var j = 0; j < a.length; j++) {
					var k = n[j] != null ? n[j].value() : null;
					var w = k != null ? entries : null;
					a[j] = resolveArgument(t[j], n[j], context, () -> w != null
							? w.stream().filter(f -> f.getKey().equals(k)).map(Entry::getValue).toArray(String[]::new)
							: null, entries, body);
				}

				Object o;
				try {
					o = d.newInstance(a);
				} catch (ReflectiveOperationException x) {
					throw new RuntimeException(x);
				}
				return o;
			}
			break;
		}
		var a = values.get();
		return a != null && a.length > 0 ? parseParameter(a[0], type) : null;
	}

	protected Object parseParameter(String s, Type type) {
		if (s == null || s.isEmpty())
			return type == String.class ? s : null;
		if (type == Integer.class || type == Integer.TYPE)
			return Integer.parseInt(s);
		if (type == Long.class || type == Long.TYPE)
			return Long.parseLong(s);
		if (type == Path.class)
			return Path.of(s);
		if (type == UUID.class)
			return UUID.fromString(s);
		return s;
	}
}

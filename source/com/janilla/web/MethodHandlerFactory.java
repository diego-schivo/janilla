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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.frontend.RenderEngine;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.io.IO;
import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.net.Net;
import com.janilla.reflect.Reflection;
import com.janilla.util.EntryList;
import com.janilla.util.EntryTree;
import com.janilla.util.Lazy;

public class MethodHandlerFactory implements WebHandlerFactory {

//	public static void main(String[] args) throws Exception {
//		class C {
//
//			@SuppressWarnings("unused")
//			public String foo() {
//				return "bar";
//			}
//		}
//		var c = new C();
//		var f1 = new MethodHandlerFactory();
//		f1.setToInvocation(r -> {
//			var u = r.getURI();
//			var p = u.getPath();
//			var n = p.startsWith("/") ? p.substring(1) : null;
//			Method m;
//			try {
//				m = C.class.getMethod(n);
//			} catch (NoSuchMethodException e) {
//				m = null;
//			} catch (SecurityException e) {
//				throw new RuntimeException(e);
//			}
//			return m != null ? new MethodInvocation(m, c, null) : null;
//		});
//		var f2 = new TemplateHandlerFactory();
//		var f = new DelegatingHandlerFactory();
//		{
//			var a = new WebHandlerFactory[] { f1, f2 };
//			f.setToHandler((o, d) -> {
//				if (a != null)
//					for (var g : a) {
//						var h = g.createHandler(o, d);
//						if (h != null)
//							return h;
//					}
//				return null;
//			});
//		}
//		f1.setMainFactory(f);
//
//		var is = new ByteArrayInputStream("""
//				GET /foo HTTP/1.1\r
//				Content-Length: 0\r
//				\r
//				""".getBytes());
//		var os = new ByteArrayOutputStream();
//		try (var rc = new HttpMessageReadableByteChannel(Channels.newChannel(is));
//				var rq = rc.readRequest();
//				var wc = new HttpMessageWritableByteChannel(Channels.newChannel(os));
//				var rs = wc.writeResponse()) {
//			var d = new HttpExchange();
//			d.setRequest(rq);
//			d.setResponse(rs);
//			var h = f.createHandler(rq, d);
//			h.handle(d);
//		}
//
//		var s = os.toString();
//		System.out.println(s);
//		assert Objects.equals(s, """
//				HTTP/1.1 200 OK\r
//				Cache-Control: no-cache\r
//				Content-Length: 4\r
//				\r
//				bar
//				""") : s;
//	}

	protected Iterable<Class<?>> types;

	protected Comparator<Invocation> comparator;

	protected Function<String, Class<?>> typeResolver;

	protected Function<Class<?>, Object> toInstance;

	protected WebHandlerFactory mainFactory;

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
				var v = m.computeIfAbsent(n.getAnnotation(Handle.class).path(), k -> new Value(new HashSet<>(), p));
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
//		System.out.println("m=" + m + "\nx=" + x);
		return x;
	});

	public void setTypes(Iterable<Class<?>> types) {
		this.types = types;
	}

	public void setComparator(Comparator<Invocation> comparator) {
		this.comparator = comparator;
	}

	public void setTypeResolver(Function<String, Class<?>> typeResolver) {
		this.typeResolver = typeResolver;
	}

	public void setToInstance(Function<Class<?>, Object> toInstance) {
		this.toInstance = toInstance;
	}

	public void setMainFactory(WebHandlerFactory mainFactory) {
		this.mainFactory = mainFactory;
	}

	@Override
	public WebHandler createHandler(Object object, HttpExchange exchange) {
		var i = object instanceof HttpRequest q ? toInvocation(q) : null;
		return i != null ? c -> handle(i, c) : null;
	}

	protected Object getInstance(Class<?> type) {
		if (toInstance != null)
			return toInstance.apply(type);
		try {
			return type.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	protected Invocation toInvocation(HttpRequest r) {
		var s = getValueAndGroupsStream(r).map(w -> {
			var v = w.value();
			var m = v.method(r);
			return m != null ? new Invocation(m, v.object(), w.groups()) : null;
		}).filter(Objects::nonNull);
		if (comparator != null)
			s = s.sorted(comparator);
		return s.findFirst().orElse(null);
	}

	public Stream<ValueAndGroups> getValueAndGroupsStream(HttpRequest q) {
		var i = invocations1.get();
		var j = invocations2.get();
//		System.out.println(Thread.currentThread().getName() + " AnnotationDrivenToMethodInvocation invocations1=" + i
//				+ ", invocations2=" + j);

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

		if (p != null)
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

//	Supplier<BiFunction<Invocation, HttpExchange, Object[]>> argumentsResolver2 = Lazy
//			.of(() -> argumentsResolver != null ? argumentsResolver : new MethodArgumentsResolver());

	protected void handle(Invocation invocation, HttpExchange exchange) {
//		var aa = argumentsResolver2.get().apply(invocation, exchange);
		var aa = resolveArguments(invocation, exchange);

		var m = invocation.method();
//		System.out.println("m=" + m + " invocation.object()=" + invocation.object() + " a=" + Arrays.toString(aa));
		Object o;
		try {
			o = aa != null ? m.invoke(invocation.object(), aa) : m.invoke(invocation.object());
		} catch (InvocationTargetException e) {
			var f = e.getTargetException();
			throw f instanceof Exception g ? new HandleException(g) : new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		var s = exchange.getResponse();
		if (m.getReturnType() == Void.TYPE) {
			if (s.getStatus() == null) {
				s.setStatus(new HttpResponse.Status(204, "No Content"));
				s.getHeaders().set("Cache-Control", "no-cache");
			}
		} else if (o instanceof Path f && m.isAnnotationPresent(Attachment.class)) {
			s.setStatus(new HttpResponse.Status(200, "OK"));
			var h = s.getHeaders();
			h.set("Cache-Control", "max-age=3600");
			h.set("Content-Disposition", "attachment; filename=\"" + f.getFileName() + "\"");
			try {
				h.set("Content-Length", String.valueOf(Files.size(f)));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			try (var fc = Files.newByteChannel(f); var bc = (WritableByteChannel) s.getBody()) {
				IO.transfer(fc, bc);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else if (o instanceof URI v) {
			s.setStatus(new HttpResponse.Status(302, "Found"));
			s.getHeaders().set("Cache-Control", "no-cache");
			s.getHeaders().set("Location", v.toString());
		} else {
			s.setStatus(new HttpResponse.Status(200, "OK"));

			var h = s.getHeaders();
			if (h.get("Cache-Control") == null)
				s.getHeaders().set("Cache-Control", "no-cache");
			if (h.get("Content-Type") == null) {
				var n = exchange.getRequest().getURI().getPath();
				var i = n != null ? n.lastIndexOf('.') : -1;
				var e = i >= 0 ? n.substring(i + 1) : null;
				if (e != null)
					switch (e) {
					case "html":
						h.set("Content-Type", "text/html");
						break;
					case "js":
						h.set("Content-Type", "text/javascript");
						break;
					}
			}

			render(RenderEngine.Entry.of(null, o, m.getAnnotatedReturnType()), exchange);
		}
	}

	protected Object[] resolveArguments(Invocation invocation, HttpExchange exchange) {
		var q = Net.parseQueryString(exchange.getRequest().getURI().getQuery());
		Supplier<String> b = switch (exchange.getRequest().getMethod().name()) {
		case "POST", "PUT" -> {
			var s = Lazy.of(() -> {
				var d = (ReadableByteChannel) exchange.getRequest().getBody();
				try {
					return new String(Channels.newInputStream(d).readAllBytes());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			var t = exchange.getRequest().getHeaders().get("Content-Type");
			if (t != null)
				switch (t.split(";")[0]) {
				case "application/x-www-form-urlencoded":
					var v = Net.parseQueryString(s.get());
					if (v == null)
						;
					else if (q == null)
						q = v;
					else
						q.addAll(v);
					break;
				case "application/json":
					var o = Json.parse(s.get());
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

		var m = invocation.method();
		var g = invocation.groups();
		var t = m.getParameterTypes();
		var u = m.getParameterAnnotations();
		var n = Arrays.stream(u)
				.map(a -> Arrays.stream(a).filter(e -> e.annotationType() == Bind.class).findFirst().orElse(null))
				.toArray(Bind[]::new);
		var a = new Object[t.length];
		for (var j = 0; j < a.length; j++) {
			var h = g != null && j < g.size() ? g.get(j) : null;
			var k = h == null && n[j] != null ? (!n[j].parameter().isEmpty() ? n[j].parameter() : n[j].value()) : null;
			var w = k != null ? q : null;
			var f = n[j];
			a[j] = resolveArgument(t[j], exchange,
					() -> h != null ? new String[] { h }
							: w != null ? w.stream().filter(x -> x.getKey().equals(k)).map(Map.Entry::getValue)
									.toArray(String[]::new) : null,
					q, b, f != null && f.resolver() != Bind.NullResolver.class ? () -> {
						try {
							return f.resolver().getConstructor().newInstance();
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException(e);
						}
					} : null);
		}
		return a;
	}

	protected Object resolveArgument(Type type, HttpExchange exchange, Supplier<String[]> values,
			EntryList<String, String> entries, Supplier<String> body,
			Supplier<UnaryOperator<Converter.MapType>> resolver) {
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
//		System.out.println("body=" + body);
				if (body == null)
					return null;
				var z = new Converter();
				if (resolver != null)
					z.setResolver(resolver.get());
				return z.convert(Json.parse(body.get()), c);
			case "application/x-www-form-urlencoded": {
				var t = createEntryTree();
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
									() -> w != null
											? w.stream().filter(f -> f.getKey().equals(k)).map(Map.Entry::getValue)
													.toArray(String[]::new)
											: null,
									entries, body, null);
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
								var v = resolveArgument(s.getType(), exchange,
										() -> w != null
												? w.stream().filter(f -> f.getKey().equals(n)).map(Map.Entry::getValue)
														.toArray(String[]::new)
												: null,
										entries, body, null);
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

	protected EntryTree createEntryTree() {
		return new EntryTree();
	}

	protected Object parseParameter(String[] strings, Type type) {
		var c = (Class<?>) type;
		var i = c.isArray() || Collection.class.isAssignableFrom(c) ? strings
				: (strings != null && strings.length > 0 ? strings[0] : null);
		var z = new Converter();
		z.setResolver(x -> x.map().containsKey("$type")
				? new Converter.MapType(x.map(), typeResolver.apply((String) x.map().get("$type")))
				: null);
		return z.convert(i, c);
	}

	protected void render(Object object, HttpExchange exchange) {
		var h = mainFactory.createHandler(object, exchange);
		if (h != null)
			h.handle(exchange);
	}

	public record Invocation(Method method, Object object, List<String> groups) {
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

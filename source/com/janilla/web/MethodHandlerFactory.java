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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.frontend.RenderEngine;
import com.janilla.http.HeaderField;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
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

	protected Comparator<Invocation> invocationComparator;

	protected Function<String, Class<?>> typeResolver;

	protected Function<Class<?>, Object> targetResolver;

	protected WebHandlerFactory mainFactory;

	Supplier<Map<String, Invocable>> invocables = Lazy.of(() -> {
		Map<String, Invocable> ii = new HashMap<>();
		for (var t : types) {
			if (Modifier.isInterface(t.getModifiers()) || Modifier.isAbstract(t.getModifiers()))
				continue;

			Object o = null;
			for (var m : t.getMethods()) {
				if (!m.isAnnotationPresent(Handle.class))
					continue;

				if (o == null)
					o = resolveTarget(t);

				var p = o;
				var i = ii.computeIfAbsent(m.getAnnotation(Handle.class).path(),
						k -> new Invocable(p, new HashMap<>()));
				MethodHandle h;
				try {
					h = MethodHandles.publicLookup().unreflect(m);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				i.methodHandles.put(m, h);
			}
		}
		return ii;
	});

	Supplier<Map<Pattern, Invocable>> regexInvocables = Lazy.of(() -> {
		var ii = invocables.get();
		var kk = ii.keySet().stream().filter(k -> k.contains("(") && k.contains(")")).collect(Collectors.toSet());
		var jj = kk.stream().collect(Collectors.toMap(k -> Pattern.compile(k), ii::get));
		ii.keySet().removeAll(kk);
//		System.out.println("m=" + m + "\nx=" + x);
		return jj;
	});

	public void setTypes(Iterable<Class<?>> types) {
		this.types = types;
	}

	public void setInvocationComparator(Comparator<Invocation> comparator) {
		this.invocationComparator = comparator;
	}

	public void setTypeResolver(Function<String, Class<?>> typeResolver) {
		this.typeResolver = typeResolver;
	}

	public void setTargetResolver(Function<Class<?>, Object> targetResolver) {
		this.targetResolver = targetResolver;
	}

	public void setMainFactory(WebHandlerFactory mainFactory) {
		this.mainFactory = mainFactory;
	}

	@Override
	public HttpHandler createHandler(Object object, HttpExchange exchange) {
		var i = object instanceof HttpRequest q ? toInvocation(q) : null;
		return i != null ? x -> {
			handle(i, (HttpExchange) x);
			return true;
		} : null;
	}

	protected Object resolveTarget(Class<?> type) {
		if (targetResolver != null)
			return targetResolver.apply(type);
		try {
			return type.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public Stream<Map.Entry<Invocable, String[]>> resolveInvocables(HttpRequest request) {
		var ii = invocables.get();
		var jj = regexInvocables.get();
//		System.out.println(Thread.currentThread().getName() + " AnnotationDrivenToMethodInvocation invocations1=" + i
//				+ ", invocations2=" + j);

		var b = Stream.<Map.Entry<Invocable, String[]>>builder();
//		URI u;
//		try {
//			u = request.getUri();
//		} catch (NullPointerException e) {
//			u = null;
//		}
//		var p = u != null ? u.getPath() : null;
		var p = request.getPath();
		if (p != null) {
			var i = ii.get(p);
			if (i != null)
				b.add(new AbstractMap.SimpleEntry<>(i, null));

			for (var e : jj.entrySet()) {
				var m = e.getKey().matcher(p);
				if (m.matches()) {
					i = e.getValue();
					var s = IntStream.range(1, 1 + m.groupCount()).mapToObj(m::group).toArray(String[]::new);
					b.add(Map.entry(i, s));
				}
			}
		}
		return b.build();
	}

	protected Invocation toInvocation(HttpRequest request) {
		String a;
		{
//			var n = request.getMethod() != null ? request.getMethod().name() : null;
			var n = request.getMethod();
			if (n == null)
				n = "";
			a = n;
		}
		var jj = resolveInvocables(request).map(x -> {
			var i = x.getKey();
			Map.Entry<Method, MethodHandle> e = null;
			for (var f : i.methodHandles.entrySet()) {
				var b = f.getKey().getAnnotation(Handle.class).method();
				if (b == null)
					b = "";
				if (b.equalsIgnoreCase(a)) {
					e = f;
					break;
				}
				if (b.isEmpty())
					e = f;
			}
			return e != null ? new Invocation(i.target, e.getKey(), e.getValue(), x.getValue()) : null;
		}).filter(Objects::nonNull);
		if (invocationComparator != null)
			jj = jj.sorted(invocationComparator);
		return jj.findFirst().orElse(null);
	}

	protected void handle(Invocation invocation, HttpExchange exchange) {
		var aa = resolveArguments(invocation, exchange);

		var m = invocation.method;
//		System.out.println("m=" + m + " invocation.object()=" + invocation.object() + " a=" + Arrays.toString(aa));
		Object o;
		try {
			var bb = new Object[1 + aa.length];
			bb[0] = invocation.target;
			if (aa.length > 0)
				System.arraycopy(aa, 0, bb, 1, aa.length);
			o = invocation.methodHandle.invokeWithArguments(bb);
		} catch (Throwable e) {
			switch (e) {
			case RuntimeException x:
				throw x;
			case Exception x:
				throw new HandleException(x);
			default:
				throw new RuntimeException(e);
			}
		}

		var rs = exchange.getResponse();
		if (m.getReturnType() == Void.TYPE) {
			if (rs.getStatus() == 0) {
//				rs.setStatus(HttpResponse.Status.of(204));
				rs.setStatus(204);
				var hh = rs.getHeaders();
				hh.add(new HeaderField("cache-control", "no-cache"));
			}
		} else if (o instanceof Path f && m.isAnnotationPresent(Attachment.class)) {
//			rs.setStatus(HttpResponse.Status.of(200));
			rs.setStatus(200);
			var hh = rs.getHeaders();
			hh.add(new HeaderField("cache-control", "max-age=3600"));
			hh.add(new HeaderField("Content-Disposition", "attachment; filename=\"" + f.getFileName() + "\""));
			try {
				hh.add(new HeaderField("content-length", String.valueOf(Files.size(f))));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
//			try (var sc = Files.newByteChannel(f); var tc = (WritableByteChannel) s.getBody()) {
//				IO.transfer(sc, tc);
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
//			try (var sc = Files.newInputStream(f); var tc = (OutputStream) rs.getBody()) {
//				sc.transferTo(tc);
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
			try {
				rs.setBody(Files.readAllBytes(f));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else if (o instanceof URI v) {
//			rs.setStatus(HttpResponse.Status.of(302));
			rs.setStatus(302);
			var hh = rs.getHeaders();
			if (hh == null)
				rs.setHeaders(hh = new ArrayList<>());
			hh.add(new HeaderField("cache-control", "no-cache"));
			hh.add(new HeaderField("location", v.toString()));
		} else {
//			rs.setStatus(HttpResponse.Status.of(200));
			rs.setStatus(200);
			var hh = rs.getHeaders();
			if (hh == null)
				rs.setHeaders(hh = new ArrayList<>());
			if (hh.stream().noneMatch(x -> x.name().equals("cache-control")))
				hh.add(new HeaderField("cache-control", "no-cache"));
			if (hh.stream().noneMatch(x -> x.name().equals("content-type"))) {
//				var p = exchange.getRequest().getUri().getPath();
				var p = exchange.getRequest().getPath();
				var i = p != null ? p.lastIndexOf('.') : -1;
				var e = i >= 0 ? p.substring(i + 1) : null;
				if (e != null)
					switch (e) {
					case "html":
						hh.add(new HeaderField("content-type", "text/html"));
						break;
					case "js":
						hh.add(new HeaderField("content-type", "text/javascript"));
						break;
					}
			}
			render(RenderEngine.Entry.of(null, o, m.getAnnotatedReturnType()), exchange);
		}
	}

	protected Object[] resolveArguments(Invocation invocation, HttpExchange exchange) {
		var rq = exchange.getRequest();
//		var qs = Net.parseQueryString(rq.getUri().getQuery());
		var qs = Net.parseQueryString(rq.getQuery());
		var mn = rq.getMethod();
		Supplier<String> z = switch (mn) {
		case "POST", "PUT" -> {
			var s = Lazy.of(() -> {
//				var c = (ReadableByteChannel) rq.getBody();
//				var c = (InputStream) rq.getBody();
//				try {
//					return new String(IO.readAllBytes(c));
//					return new String(c.readAllBytes());
//				} catch (IOException e) {
//					throw new UncheckedIOException(e);
//				}
				return rq.getBody() != null ? new String(rq.getBody()) : null;
			});
			var t = rq.getHeaders().stream().filter(x -> x.name().equals("content-type")).map(HeaderField::value)
					.findFirst().orElse(null);
			if (t != null)
				switch (t.split(";")[0]) {
				case "application/x-www-form-urlencoded":
					var v = Net.parseQueryString(s.get());
					if (v == null)
						;
					else if (qs == null)
						qs = v;
					else
						qs.addAll(v);
					break;
				case "application/json":
					var a = s.get();
					var o = a != null ? Json.parse(a) : null;
					if (o instanceof Map<?, ?> m && !m.isEmpty()) {
						if (qs == null)
							qs = new EntryList<>();
						for (var e : m.entrySet())
							qs.add(e.getKey().toString(), e.getValue().toString());
					}
					break;
				}
			yield s;
		}
		default -> null;
		};

		var m = invocation.method;
		var gg = invocation.regexGroups;
		var ptt = m.getParameterTypes();
		var paa = m.getParameterAnnotations();
		var bb = Arrays.stream(paa)
				.map(x -> Arrays.stream(x).filter(y -> y.annotationType() == Bind.class).findFirst().orElse(null))
				.toArray(Bind[]::new);
		var aa = new Object[ptt.length];
		for (var i = 0; i < aa.length; i++) {
			var g = gg != null && i < gg.length ? gg[i] : null;
			var b = bb[i];
			var p = g == null && b != null ? (!b.parameter().isEmpty() ? b.parameter() : b.value()) : null;
			var qs2 = p != null ? qs : null;
			aa[i] = resolveArgument(ptt[i], exchange,
					() -> g != null ? new String[] { g }
							: qs2 != null ? qs2.stream().filter(x -> x.getKey().equals(p)).map(Map.Entry::getValue)
									.toArray(String[]::new) : null,
					qs, z, b != null && b.resolver() != Bind.NullResolver.class ? () -> {
						try {
							return b.resolver().getConstructor().newInstance();
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException(e);
						}
					} : null);
		}
		return aa;
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
		if (c != null && !c.getPackageName().startsWith("java.")) {
			var ct = exchange.getRequest().getHeaders().stream().filter(x -> x.name().equals("content-type"))
					.map(HeaderField::value).findFirst().orElse(null);
			switch (Objects.toString(ct, "").split(";")[0]) {
			case "application/json": {
//		System.out.println("body=" + body);
				var b = body != null ? body.get() : null;
				if (b == null)
					return null;
				var d = new Converter();
				if (resolver != null)
					d.setResolver(resolver.get());
				return d.convert(Json.parse(b), c);
			}
			case "application/x-www-form-urlencoded": {
				var t = createEntryTree();
				t.setTypeResolver(typeResolver);
				entries.forEach(t::add);
				return t.convert(c);
			}
			default:
				if (c.isRecord()) {
					var tt = Arrays.stream(c.getRecordComponents()).collect(
							Collectors.toMap(x -> x.getName(), x -> x.getType(), (v, w) -> v, LinkedHashMap::new));
					var aa = tt.entrySet().stream().map(x -> {
						var n = x.getKey();
						var t = x.getValue();
						return resolveArgument(t, exchange,
								() -> entries != null
										? entries.stream().filter(y -> y.getKey().equals(n)).map(Map.Entry::getValue)
												.toArray(String[]::new)
										: null,
								entries, body, null);
					}).toArray();
					try {
						return c.getConstructors()[0].newInstance(aa);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				} else {
					Constructor<?> d;
					try {
						d = c.getConstructor();
					} catch (NoSuchMethodException e) {
						d = null;
					}
					if (d != null) {
						Object o;
						try {
							o = d.newInstance();
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException(e);
						}
						for (var nn = Reflection.properties(c).iterator(); nn.hasNext();) {
							var n = nn.next();
							var p = Reflection.property(c, n);
							if (p == null)
								continue;
							var v = resolveArgument(p.getType(), exchange,
									() -> entries != null
											? entries.stream().filter(x -> x.getKey().equals(n))
													.map(Map.Entry::getValue).toArray(String[]::new)
											: null,
									entries, body, null);
							p.set(o, v);
						}
						return o;
					}
				}
				break;
			}
		}
		var vv = values.get();
		return parseParameter(vv, type);
	}

	protected EntryTree createEntryTree() {
		return new EntryTree();
	}

	protected Object parseParameter(String[] strings, Type type) {
		var c = (Class<?>) type;
		var i = c.isArray() || Collection.class.isAssignableFrom(c) ? strings
				: (strings != null && strings.length > 0 ? strings[0] : null);
		var d = new Converter();
		d.setResolver(x -> x.map().containsKey("$type")
				? new Converter.MapType(x.map(), typeResolver.apply((String) x.map().get("$type")))
				: null);
		return d.convert(i, c);
	}

	protected void render(Object object, HttpExchange exchange) {
		var h = mainFactory.createHandler(object, exchange);
		if (h != null)
			h.handle(exchange);
	}

	public record Invocable(Object target, Map<Method, MethodHandle> methodHandles) {
	}

	public record Invocation(Object target, Method method, MethodHandle methodHandle, String... regexGroups) {
	}
}

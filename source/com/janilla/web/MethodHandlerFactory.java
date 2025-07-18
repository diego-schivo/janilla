/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.json.MapAndType;
import com.janilla.net.Net;
import com.janilla.reflect.Reflection;
import com.janilla.util.EntryList;
import com.janilla.util.EntryTree;

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

	protected Comparator<Invocation> invocationComparator;

	protected Function<Class<?>, Object> targetResolver;

	protected WebHandlerFactory mainFactory;

	protected RenderableFactory renderableFactory;

	protected Map<String, Invocable> invocables;

	protected Map<Pattern, Invocable> regexInvocables;

	public void setInvocationComparator(Comparator<Invocation> comparator) {
		this.invocationComparator = comparator;
	}

	public void setMainFactory(WebHandlerFactory mainFactory) {
		this.mainFactory = mainFactory;
	}

	public void setRenderableFactory(RenderableFactory renderableFactory) {
		this.renderableFactory = renderableFactory;
	}

	public void initialize(Iterable<Class<?>> types, Function<Class<?>, Object> targetResolver) {
		this.targetResolver = targetResolver;
		invocables = new HashMap<>();
		for (var t : types) {
			if (Modifier.isInterface(t.getModifiers()) || Modifier.isAbstract(t.getModifiers()))
				continue;
//			System.out.println("MethodHandlerFactory.initialize, t=" + t);
			var h0 = t.getAnnotation(Handle.class);
			var p0 = h0 != null ? h0.path() : null;
			Object o = null;
			for (var m : t.getMethods()) {
				var h = m.getAnnotation(Handle.class);
				var p = h != null ? h.path() : null;
				if (p == null)
					continue;
//				System.out.println("MethodHandlerFactory.initialize, m=" + m);
				if (p0 != null && !p0.isEmpty())
					p = !p.isEmpty() ? p0 + '/' + p : p0;
				if (o == null)
					o = resolveTarget(t);
				var o2 = o;
				var i = invocables.computeIfAbsent(p, _ -> new Invocable(o2, new HashMap<>()));
				MethodHandle mh;
				try {
					mh = MethodHandles.publicLookup().unreflect(m);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				i.methodHandles.put(m, mh);
			}
		}
		var kk = invocables.keySet().stream().filter(k -> k.contains("(") && k.contains(")"))
				.collect(Collectors.toSet());
		regexInvocables = kk.stream().sorted(Comparator.comparingInt((String x) -> {
			var i = x.indexOf('(');
			return i >= 0 ? i : x.length();
		}).reversed())
				.collect(Collectors.toMap(k -> Pattern.compile(k), invocables::get, (v, _) -> v, LinkedHashMap::new));
		invocables.keySet().removeAll(kk);
//		System.out.println("m=" + m + "\nx=" + x);
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
		var ii = invocables;
		var jj = regexInvocables;
//		System.out.println(Thread.currentThread().getName() + " AnnotationDrivenToMethodInvocation invocations1=" + i
//				+ ", invocations2=" + j);

		var b = Stream.<Map.Entry<Invocable, String[]>>builder();
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
			return e != null
					? new Invocation(i.target, e.getKey(), e.getValue(),
							getParameterTypes(e.getKey(), i.target.getClass()), x.getValue())
					: null;
		}).filter(Objects::nonNull);
		if (invocationComparator != null)
			jj = jj.sorted(invocationComparator);
		return jj.findFirst().orElse(null);
	}

	public static final ScopedValue<Set<String>> JSON_KEYS = ScopedValue.newInstance();

	protected void handle(Invocation invocation, HttpExchange exchange) {
//		System.out.println("MethodHandlerFactory.handle, invocation=" + invocation);

		var o = ScopedValue.where(JSON_KEYS, new HashSet<>()).call(() -> {
			var aa = resolveArguments(invocation, exchange);
//		System.out.println("MethodHandlerFactory.handle, aa=" + Arrays.toString(aa));
			try {
				var bb = new Object[1 + aa.length];
				bb[0] = invocation.target;
				if (aa.length > 0)
					System.arraycopy(aa, 0, bb, 1, aa.length);
				return invocation.methodHandle.invokeWithArguments(bb);
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
		});

		var rs = exchange.getResponse();
		if (rs.getStatus() != 0)
			;
		else if (invocation.method.getReturnType() == Void.TYPE) {
			rs.setStatus(204);
			rs.setHeaderValue("cache-control", "no-cache");
		} else if (o instanceof Path f && invocation.method.isAnnotationPresent(Attachment.class)) {
			rs.setStatus(200);
			rs.setHeaderValue("cache-control", "max-age=3600");
			rs.setHeaderValue("Content-Disposition", "attachment; filename=\"" + f.getFileName() + "\"");
			try {
				rs.setHeaderValue("content-length", String.valueOf(Files.size(f)));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			try (var in = Files.newInputStream(f);
					var out = Channels.newOutputStream((WritableByteChannel) rs.getBody())) {
				in.transferTo(out);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else if (o instanceof URI v) {
			rs.setStatus(302);
			rs.setHeaderValue("cache-control", "no-cache");
			rs.setHeaderValue("location", v.toString());
		} else {
			rs.setStatus(200);
			if (rs.getHeaderValue("cache-control") == null)
				rs.setHeaderValue("cache-control", "no-cache");
//			if (rs.getHeaderValue("content-type") == null) {
//				var p = exchange.getRequest().getPath();
//				var i = p != null ? p.lastIndexOf('.') : -1;
//				var e = i >= 0 ? p.substring(i + 1) : null;
//				if (e != null)
//					switch (e) {
//					case "html":
//						rs.setHeaderValue("content-type", "text/html");
//						break;
//					case "js":
//						rs.setHeaderValue("content-type", "text/javascript");
//						break;
//					}
//			}
			render(renderableFactory.createRenderable(invocation.method.getAnnotatedReturnType(), o), exchange);
		}
	}

	protected Object[] resolveArguments(Invocation invocation, HttpExchange exchange) {
		var rq = exchange.getRequest();
		var qs = Net.parseQueryString(rq.getQuery());
		Supplier<String> bs = switch (rq.getMethod()) {
		case "PATCH", "POST", "PUT" -> {
			var s = new Supplier<String>() {

				private String[] x;

				@Override
				public String get() {
					if (x == null)
						try {
							var ch = (ReadableByteChannel) rq.getBody();
							x = new String[] {
									ch != null ? new String(Channels.newInputStream(ch).readAllBytes()) : null };
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					return x[0];
				}
			};
			var ct = Objects.toString(rq.getHeaderValue("content-type"), "").split(";")[0];
			switch (ct) {
			case "application/x-www-form-urlencoded":
				var qs2 = Net.parseQueryString(s.get());
				if (qs2 == null)
					;
				else if (qs == null)
					qs = qs2;
				else
					qs.addAll(qs2);
				break;
			case "application/json":
				var s2 = s.get();
				var o = s2 != null ? Json.parse(s2) : null;
				if (o instanceof Map<?, ?> m && !m.isEmpty()) {
					if (qs == null)
						qs = new EntryList<>();
					for (var kv : m.entrySet())
						if (kv.getValue() instanceof Boolean || kv.getValue() instanceof Number
								|| kv.getValue() instanceof String)
							qs.add(kv.getKey().toString(), kv.getValue().toString());
				}
				break;
			}
			yield s;
		}
		default -> null;
		};

		var m = invocation.method;
		var gg = invocation.regexGroups;
		var pp = m.getParameters();
		var ptt = invocation.parameterTypes();
		var paa = m.getParameterAnnotations();
		var bb = Arrays.stream(paa)
				.map(x -> Arrays.stream(x).filter(y -> y.annotationType() == Bind.class).findFirst().orElse(null))
				.toArray(Bind[]::new);
		var aa = new Object[ptt.length];
		var ggl = gg != null ? gg.length : 0;
		for (var i = 0; i < aa.length; i++) {
			var g = i < ggl ? gg[i] : null;
			var b = i < ggl ? null : bb[i];
			var n = Stream.of(b != null ? b.parameter() : null, b != null ? b.value() : null, pp[i].getName())
					.filter(x -> x != null && !x.isEmpty()).findFirst().orElse(null);
			var qs2 = qs;
			var bs2 = i == ggl || (ptt[i] instanceof Class<?> x && x.isRecord()) ? bs : null;
			aa[i] = resolveArgument(ptt[i], exchange,
					i < ggl ? (g != null ? new String[] { g } : null)
							: qs2 != null && n != null ? qs2.stream(n).toArray(String[]::new) : null,
					i >= ggl ? qs : null, bs2,
					b != null && b.resolver() != MapAndType.NullTypeResolver.class ? () -> resolver(b.resolver())
							: null);
		}
		return aa;
	}

	protected Object resolveArgument(Type type, HttpExchange exchange, String[] values,
			EntryList<String, String> entries, Supplier<String> body, Supplier<MapAndType.TypeResolver> resolver) {
		var c = type instanceof Class<?> x ? x : null;
		if (c != null && HttpExchange.class.isAssignableFrom(c))
			return exchange;
		if (c != null && HttpRequest.class.isAssignableFrom(c))
			return exchange.getRequest();
		if (c != null && HttpResponse.class.isAssignableFrom(c))
			return exchange.getResponse();
		if (values != null && values.length > 0)
			return parseParameter(values, type);
		if (c != null) {
			var ct = exchange.getRequest().getHeaderValue("content-type");
			switch (Objects.toString(ct, "").split(";")[0]) {
			case "application/json": {
				if (body == null)
					break;
				var b = body.get();
//				System.out.println("MethodHandlerFactory.resolveArgument, b=" + b);
				if (b == null)
					return null;
				var o = Json.parse(b);
				if (o instanceof Map<?, ?> m) {
					@SuppressWarnings("unchecked")
					var kk = (Collection<String>) m.keySet();
					JSON_KEYS.get().addAll(kk);
				}
				return new Converter(resolver != null ? resolver.get() : null).convert(o, c);
			}
			case "application/x-www-form-urlencoded": {
				if (entries == null)
					break;
				var t = createEntryTree();
//				t.setTypeResolver(typeResolver);
				entries.forEach(t::add);
				return t.convert(c);
			}
			default:
				if (c.isRecord()) {
					var tt = Arrays.stream(c.getRecordComponents()).collect(
							Collectors.toMap(x -> x.getName(), x -> x.getType(), (v, _) -> v, LinkedHashMap::new));
					var aa = tt.entrySet().stream().map(x -> {
						var n = x.getKey();
						var t = x.getValue();
						return resolveArgument(t, exchange,
								entries != null
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
				} else if (!c.getPackageName().startsWith("java.")) {
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
						for (var nn = Reflection.propertyNames(c).iterator(); nn.hasNext();) {
							var n = nn.next();
							var p = Reflection.property(c, n);
							if (p == null)
								continue;
							var v = resolveArgument(p.type(), exchange,
									entries != null
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
		return null;
	}

	protected EntryTree createEntryTree() {
		return new EntryTree();
	}

	protected Object parseParameter(String[] strings, Type type) {
		var c = (Class<?>) type;
		var i = c.isArray() || Collection.class.isAssignableFrom(c) ? strings
				: (strings != null && strings.length > 0 ? strings[0] : null);
//		var d = new Converter(x -> x.map().containsKey("$type")
//				? new Converter.MapType(x.map(), typeResolver.apply((String) x.map().get("$type")))
//				: null);
		var d = new Converter(null);
		return d.convert(i, c);
	}

	protected MapAndType.TypeResolver resolver(Class<? extends MapAndType.TypeResolver> class0) {
		try {
			return class0.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	protected void render(Renderable<?> renderable, HttpExchange exchange) {
//		System.out.println("MethodHandlerFactory.render, renderable=" + renderable);
		var h = mainFactory.createHandler(renderable, exchange);
		if (h != null)
			h.handle(exchange);
	}

	protected static Class<?>[] getParameterTypes(Method method, Class<?> class1) {
		if (class1.getGenericSuperclass() instanceof ParameterizedType pt) {
			var pp = method.getDeclaringClass().getTypeParameters();
			var aa = pt.getActualTypeArguments();
			var m = IntStream.range(0, pp.length).mapToObj(i -> Map.entry(pp[i].getName(), aa[i]))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//		System.out.println("m=" + m);
			var tt = Arrays.stream(method.getGenericParameterTypes())
					.map(x -> x instanceof TypeVariable v ? m.get(v.getName()) : x).toArray(Class[]::new);
//		System.out.println(Arrays.asList(tt));
			return tt;
		}
		return method.getParameterTypes();
	}

	public record Invocable(Object target, Map<Method, MethodHandle> methodHandles) {
	}

	public record Invocation(Object target, Method method, MethodHandle methodHandle, Class<?>[] parameterTypes,
			String... regexGroups) {

//		public Class<?>[] getParameterTypes() {
//			var pp = method.getDeclaringClass().getTypeParameters();
//			var aa = ((ParameterizedType) target.getClass().getGenericSuperclass()).getActualTypeArguments();
//			var m = IntStream.range(0, pp.length).mapToObj(i -> Map.entry(pp[i].getName(), aa[i]))
//					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//			System.out.println("m=" + m);
//			var tt = Arrays.stream(method.getGenericParameterTypes())
//					.map(x -> x instanceof TypeVariable v ? m.get(v.getName()) : x).toArray(Class[]::new);
//			System.out.println(Arrays.asList(tt));
//			return tt;
//		}
	}
}

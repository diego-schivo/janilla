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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.java.Java;
import com.janilla.java.Java.EntryList;
import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.json.NullTypeResolver;
import com.janilla.json.TypeResolver;
import com.janilla.net.Net;
import com.janilla.reflect.ClassAndMethod;
import com.janilla.reflect.Reflection;
import com.janilla.reflect.SimpleParameterizedType;

public class MethodHandlerFactory implements HttpHandlerFactory {

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
//		IO.println(s);
//		assert Objects.equals(s, """
//				HTTP/1.1 200 OK\r
//				Cache-Control: no-cache\r
//				Content-Length: 4\r
//				\r
//				bar
//				""") : s;
//	}

	protected final Comparator<Invocation> invocationComparator;

	protected final Function<Class<?>, Object> targetResolver;

	protected final RenderableFactory renderableFactory;

	protected final HttpHandlerFactory rootFactory;

	protected final Map<String, Invocable> invocables;

	protected final Map<Pattern, Invocable> regexInvocables;

	public MethodHandlerFactory(Collection<ClassAndMethod> methods, Function<Class<?>, Object> targetResolver,
			Comparator<Invocation> invocationComparator, RenderableFactory renderableFactory,
			HttpHandlerFactory rootFactory) {
		this.targetResolver = Objects.requireNonNullElseGet(targetResolver, () -> x -> {
			try {
				return x.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
		this.invocationComparator = invocationComparator;
		this.renderableFactory = Objects.requireNonNullElseGet(renderableFactory, RenderableFactory::new);
		this.rootFactory = rootFactory;

		var oo = new HashMap<Class<?>, Object>();
		invocables = new HashMap<>();
		for (var z : methods) {
			var c = z.class1();
			var m = z.method();
//			IO.println("MethodHandlerFactory, m=" + m + ", c=" + c);
			var h1 = c.getAnnotation(Handle.class);
			var p1 = h1 != null ? h1.path() : null;
			var h2 = m.getAnnotation(Handle.class);
			var p2 = h2 != null ? h2.path() : null;
			if (p2 == null)
				continue;
			else if (p2.startsWith("/"))
				p1 = null;
			var p = Stream.of(p1, p2).filter(x -> x != null && !x.isEmpty()).collect(Collectors.joining("/"));
			var i = invocables.computeIfAbsent(p,
					_ -> new Invocable(oo.computeIfAbsent(c, targetResolver), new HashMap<>()));
			MethodHandle h;
			try {
				h = MethodHandles.publicLookup().unreflect(m);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			i.methodHandles.put(m, h);
		}

		var kk = invocables.keySet().stream().filter(k -> k.contains("(") && k.contains(")")).toList();
		regexInvocables = kk.stream().sorted(Comparator.comparingInt((String x) -> x.indexOf('(')).reversed())
				.collect(Collectors.toMap(k -> Pattern.compile(k), invocables::get, (v, _) -> v, LinkedHashMap::new));
		invocables.keySet().removeAll(kk);
//		IO.println("m=" + m + "\nx=" + x);
	}

	@Override
	public HttpHandler createHandler(Object object) {
		if (object instanceof HttpRequest r) {
			var m1 = Objects.requireNonNullElse(r.getMethod(), "");
			var ii = resolveInvocables(r.getPath()).map(x -> {
				Map.Entry<Method, MethodHandle> kv = null;
				for (var y : x.methodHandles.entrySet()) {
					var m2 = Objects.requireNonNullElse(y.getKey().getAnnotation(Handle.class).method(), "");
					if (m2.equalsIgnoreCase(m1)) {
						kv = y;
						break;
					}
					if (m2.isEmpty())
						kv = y;
				}
				return kv != null
						? new Invocation(x.target, kv.getKey(), kv.getValue(),
								getParameterTypes(kv.getKey(), x.target.getClass()), x.regexGroups)
						: null;
			}).filter(Objects::nonNull);
			if (invocationComparator != null)
				ii = ii.sorted(invocationComparator);
			var i = ii.findFirst();
			if (i.isPresent())
				return x -> handle(i.get(), x);
		}
		return null;
	}

	public Stream<Invocable> resolveInvocables(String path) {
//		IO.println(Thread.currentThread().getName() + " AnnotationDrivenToMethodInvocation invocations1=" + i
//				+ ", invocations2=" + j);
		if (path == null)
			return Stream.empty();
		var i = invocables.get(path);
		var b = Stream.<Invocable>builder();
		if (i != null)
			b.add(i);
		for (var x : regexInvocables.entrySet()) {
			var m = x.getKey().matcher(path);
			if (m.matches()) {
				i = x.getValue();
				var ss = IntStream.range(1, 1 + m.groupCount()).mapToObj(m::group).toArray(String[]::new);
				b.add(ss.length != 0 ? i.withRegexGroups(ss) : i);
			}
		}
		return b.build();
	}

	public static final ScopedValue<Set<String>> JSON_KEYS = ScopedValue.newInstance();

	protected boolean handle(Invocation invocation, HttpExchange exchange) {
//		IO.println("MethodHandlerFactory.handle, invocation=" + invocation);

		var o = ScopedValue.where(JSON_KEYS, new HashSet<>()).call(() -> {
			var aa1 = resolveArguments(invocation, exchange);
//		IO.println("MethodHandlerFactory.handle, aa=" + Arrays.toString(aa));
			try {
				var aa2 = Stream.concat(Stream.of(invocation.target), Arrays.stream(aa1)).toArray();
				return invocation.methodHandle.invokeWithArguments(aa2);
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

		var rs = exchange.response();
		if (rs.getStatus() != 0)
			;
		else if (invocation.method.getReturnType() == Void.TYPE) {
			rs.setStatus(204);
			rs.setHeaderValue("cache-control", "no-cache");
		} else if (o instanceof Path x && invocation.method.isAnnotationPresent(Attachment.class)) {
			rs.setStatus(200);
			rs.setHeaderValue("cache-control", "max-age=3600");
			rs.setHeaderValue("Content-Disposition", "attachment; filename=\"" + x.getFileName() + "\"");
			try {
				rs.setHeaderValue("content-length", String.valueOf(Files.size(x)));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			try (var in = Files.newInputStream(x);
					var out = Channels.newOutputStream((WritableByteChannel) rs.getBody())) {
				in.transferTo(out);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else if (o instanceof URI x) {
			rs.setStatus(302);
			rs.setHeaderValue("cache-control", "no-cache");
			rs.setHeaderValue("location", x.toString());
		} else {
			rs.setStatus(200);
			if (rs.getHeader("cache-control") == null)
				rs.setHeaderValue("cache-control", "no-cache");
			var r = renderableFactory.createRenderable(invocation.method.getAnnotatedReturnType(), o);
			render(r, exchange);
		}
		return true;
	}

	protected Object[] resolveArguments(Invocation invocation, HttpExchange exchange) {
		var rq = exchange.request();
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
			var ct = Objects.requireNonNullElse(rq.getHeaderValue("content-type"), "").split(";")[0];
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
						qs = new Java.EntryList<>();
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
					b != null && b.resolver() != NullTypeResolver.class ? () -> resolver(b.resolver()) : null);
		}
		return aa;
	}

	protected Object resolveArgument(Type type, HttpExchange exchange, String[] values,
			EntryList<String, String> entries, Supplier<String> body, Supplier<TypeResolver> resolver) {
		var c = type instanceof Class<?> x ? x : null;
		if (c != null && HttpExchange.class.isAssignableFrom(c))
			return exchange;
		if (c != null && HttpRequest.class.isAssignableFrom(c))
			return exchange.request();
		if (c != null && HttpResponse.class.isAssignableFrom(c))
			return exchange.response();
		if (values != null && values.length > 0)
			return parseParameter(values, type);
		if (c != null) {
			var ct = exchange.request().getHeaderValue("content-type");
			switch (Objects.requireNonNullElse(ct, "").split(";")[0]) {
			case "application/json": {
				if (body == null)
					break;
				var b = body.get();
//				IO.println("MethodHandlerFactory.resolveArgument, b=" + b);
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
//		IO.println("MethodHandlerFactory.parseParameter, strings=" + Arrays.toString(strings) + ", type=" + type);
		var c = (Class<?>) (type instanceof ParameterizedType x ? x.getRawType() : type);
		var i = c.isArray() || Collection.class.isAssignableFrom(c) ? strings
				: (strings != null && strings.length > 0 ? strings[0] : null);
//		var d = new Converter(x -> x.map().containsKey("$type")
//				? new Converter.MapType(x.map(), typeResolver.apply((String) x.map().get("$type")))
//				: null);
		var d = new Converter(null);
		return d.convert(i, type);
	}

	protected TypeResolver resolver(Class<? extends TypeResolver> class0) {
		try {
			return class0.getConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	protected void render(Renderable<?> renderable, HttpExchange exchange) {
//		IO.println("MethodHandlerFactory.render, renderable=" + renderable);
		var h = rootFactory.createHandler(renderable);
		if (h != null)
			h.handle(exchange);
	}

	protected static Type[] getParameterTypes(Method method, Class<?> class1) {
		if (class1.getGenericSuperclass() instanceof ParameterizedType pt) {
			var pp = method.getDeclaringClass().getTypeParameters();
			var aa = pt.getActualTypeArguments();
			var m = IntStream.range(0, pp.length).mapToObj(i -> Map.entry(pp[i].getName(), aa[i]))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//			IO.println("MethodHandlerFactory.getParameterTypes, m=" + m);
			var tt = Arrays.stream(method.getGenericParameterTypes()).map(x -> {
				switch (x) {
				case TypeVariable<?> x2:
					return m.get(x2.getName());
				case ParameterizedType x2:
					return new SimpleParameterizedType(x2.getRawType(), Arrays.stream(x2.getActualTypeArguments())
							.map(y -> y instanceof TypeVariable y2 ? m.get(y2.getName()) : y).toArray(Type[]::new),
							null);
				default:
					return x;
				}
			}).toArray(Type[]::new);
//			IO.println("MethodHandlerFactory.getParameterTypes, tt=" + Arrays.toString(tt));
			return tt;
		}
		return method.getParameterTypes();
	}

	public record Invocable(Object target, Map<Method, MethodHandle> methodHandles, String... regexGroups) {

		public Invocable withRegexGroups(String... regexGroups) {
			return new Invocable(target, methodHandles, regexGroups);
		}
	}

	public record Invocation(Object target, Method method, MethodHandle methodHandle, Type[] parameterTypes,
			String... regexGroups) {
	}

	public static class EntryTree extends LinkedHashMap<String, Object> {

		private static final long serialVersionUID = 2351446498774467936L;

		public void add(Map.Entry<String, String> entry) {
			Map<String, Object> m = this;
			var ss = entry.getKey().split("\\.");
			for (var i = 0; i < ss.length; i++) {
				if (ss[i].endsWith("]")) {
					var ss2 = ss[i].split("\\[", 2);
					var i2 = Integer.parseInt(ss2[1].substring(0, ss2[1].length() - 1));
					if (i2 < 0 || i2 >= 1000)
						throw new RuntimeException();
					@SuppressWarnings("unchecked")
					var l = (List<Object>) m.computeIfAbsent(ss2[0], _ -> new ArrayList<Object>());
					while (l.size() <= i2)
						l.add(null);
					if (i < ss.length - 1) {
						@SuppressWarnings("unchecked")
						var m2 = (Map<String, Object>) l.get(i2);
						if (m2 == null) {
							m2 = new LinkedHashMap<String, Object>();
							l.set(i2, m2);
						}
						m = m2;
					} else
						l.set(i2, entry.getValue());
				} else if (i < ss.length - 1) {
					@SuppressWarnings("unchecked")
					var o = (Map<String, Object>) m.computeIfAbsent(ss[i], _ -> new LinkedHashMap<String, Object>());
					m = o;
				} else
					m.put(ss[i], entry.getValue());
			}
		}

		public <T> T convert(Class<T> target) {
			return convert(this, target);
		}

		protected <T> T convert(Map<String, Object> map, Class<T> target) {
			if (map.containsKey("$type"))
				try {
					@SuppressWarnings("unchecked")
					var c = (Class<T>) Class.forName(target.getPackageName() + "." + map.get("$type"));
					if (!target.isAssignableFrom(c))
						throw new RuntimeException();
					target = c;
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			BiFunction<String, Type, Object> c = (name, type) -> new Converter(null).convert(map.get(name), type);
			try {
				if (target.isRecord()) {
					var oo = new ArrayList<Object>();
					for (var x : target.getRecordComponents())
						oo.add(c.apply(x.getName(), x.getGenericType()));
					@SuppressWarnings("unchecked")
					var t = (T) target.getConstructors()[0].newInstance(oo.toArray());
					return t;
				}
				var z = target;
				var t = z.getConstructor().newInstance();
				Reflection.properties(z).forEach(x -> {
					var o = c.apply(x.name(), x.genericType());
					if (o != null)
						x.set(t, o);
				});
				return t;
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

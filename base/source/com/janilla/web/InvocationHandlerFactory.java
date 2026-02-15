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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.java.Converter;
import com.janilla.java.Java;
import com.janilla.java.NullTypeResolver;
import com.janilla.java.Property;
import com.janilla.java.Reflection;
import com.janilla.java.TypeResolver;
import com.janilla.java.UriQueryBuilder;
import com.janilla.json.Json;

public class InvocationHandlerFactory implements HttpHandlerFactory {

	protected final InvocationResolver invocationResolver;

	protected final RenderableFactory renderableFactory;

	protected final HttpHandlerFactory rootFactory;

	public InvocationHandlerFactory(InvocationResolver invocationResolver, RenderableFactory renderableFactory,
			HttpHandlerFactory rootFactory) {
		this.invocationResolver = invocationResolver;
		this.renderableFactory = renderableFactory;
		this.rootFactory = rootFactory;
	}

	@Override
	public HttpHandler createHandler(Object object) {
		if (object instanceof HttpRequest r) {
			var ii = invocationResolver.lookup(r.getMethod(), r.getPath()).toList();
			if (!ii.isEmpty())
				return x -> {
					NotFoundException e = null;
					for (var i : ii)
						try {
							return handle(i, x);
						} catch (NotFoundException e2) {
							e = e2;
						}
					throw e;
				};
		}
		return null;
	}

	public static final ScopedValue<Set<String>> JSON_KEYS = ScopedValue.newInstance();

	protected boolean handle(Invocation invocation, HttpExchange exchange) {
//		IO.println("InvocationHandlerFactory.handle, invocation=" + invocation);

		var o = ScopedValue.where(JSON_KEYS, new HashSet<>()).call(() -> {
			var h = Reflection.methodHandle(invocation.method());
			var oo = Stream
					.concat(Stream.of(invocation.object()), Arrays.stream(resolveArguments(invocation, exchange)))
					.toArray();
//		IO.println("InvocationHandlerFactory.handle, aa=" + Arrays.toString(aa));
			try {
				return h.invokeWithArguments(oo);
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
//		IO.println("InvocationHandlerFactory.handle, o=" + o);

		var rs = exchange.response();
		if (rs.getStatus() != 0)
			;
		else if (invocation.method().getReturnType() == Void.TYPE) {
			rs.setStatus(204);
			rs.setHeaderValue("cache-control", "no-cache");
		} else if (o instanceof Path x && invocation.method().isAnnotationPresent(Attachment.class)) {
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
//			rs.setStatus(307);
			rs.setStatus(303);
			rs.setHeaderValue("cache-control", "no-cache");
			rs.setHeaderValue("location", x.toString());
		} else {
			rs.setStatus(200);
			if (rs.getHeader("cache-control") == null)
				rs.setHeaderValue("cache-control", "no-cache");
			var r = renderableFactory != null
					? renderableFactory.createRenderable(invocation.method().getAnnotatedReturnType(), o)
					: new Renderable<>(o, null);
			render(r, exchange);
		}
		return true;
	}

	protected Object[] resolveArguments(Invocation invocation, HttpExchange exchange) {
		var rq = exchange.request();
		var bs = new Supplier<String>() {

			private Optional<String> body;

			@Override
			public String get() {
				if (body == null)
					try {
						body = Optional.ofNullable(rq.getBody() instanceof ReadableByteChannel x
								? new String(Channels.newInputStream(x).readAllBytes())
								: null);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				return body.orElse(null);
			}
		};

		var qs = rq.getQuery();
		if (Set.of("PATCH", "POST", "PUT").contains(rq.getMethod())) {
			var ct = Objects.requireNonNullElse(rq.getHeaderValue("content-type"), "").split(";")[0];
			switch (ct) {
			case "application/x-www-form-urlencoded":
				qs = Stream.of(qs, bs.get()).filter(x -> x != null && !x.isEmpty()).collect(Collectors.joining("&"));
				break;
//			case "application/json":
//				var s = bs.get();
//				var o = s != null ? Json.parse(s) : null;
//				if (o instanceof Map<?, ?> m && !m.isEmpty())
//					qs = Stream.concat(Stream.of(qs), m.entrySet().stream().map(x -> {
//						var y = x.getValue();
//						return y instanceof Boolean || y instanceof Number || y instanceof String ? x.getKey() + "=" + y
//								: null;
//					})).filter(x -> x != null && !x.isEmpty()).collect(Collectors.joining("&"));
//				break;
			}
		}

		var ii = IntStream.iterate(0, x -> x + 1).iterator();
		var ptt = Reflection.actualParameterTypes(invocation.method(), invocation.object().getClass());
		var oo1 = Arrays.stream(invocation.regexGroups()).map(x -> {
			var i = ii.nextInt();
			return resolveArgument(ptt[i], exchange, new String[] { x }, null, () -> converter(null));
		}).toArray();

		var bb = Arrays.stream(invocation.method().getParameterAnnotations())
				.map(x -> Arrays.stream(x).filter(y -> y.annotationType() == Bind.class).findFirst().orElse(null))
				.toArray(Bind[]::new);
		var pp = invocation.method().getParameters();
		var q = qs != null ? new UriQueryBuilder(qs) : null;
		var oo2 = Arrays.stream(ptt).skip(oo1.length).map(pt -> {
			var i = ii.nextInt();
			var n = Stream
					.of(bb[i] != null ? bb[i].parameter() : null, bb[i] != null ? bb[i].value() : null, pp[i].getName())
					.filter(x -> x != null && !x.isEmpty()).findFirst().orElse(null);
			var vv = q != null ? q.values(n).toArray(String[]::new) : null;
			var bs2 = i == oo1.length || (pt instanceof Class<?> c && c.isRecord()) ? bs : null;
			Supplier<Converter> cs = () -> converter(bb[i] != null ? bb[i].resolver() : null);
			return resolveArgument(pt, exchange, vv, bs2, cs);
		}).toArray();
		return Stream.of(oo1, oo2).flatMap(Arrays::stream).toArray();
	}

	protected Object resolveArgument(Type type, HttpExchange exchange, String[] values, Supplier<String> body,
			Supplier<Converter> converter) {
//		IO.println("InvocationHandlerFactory.resolveArgument, type=" + type);
		var c = Java.toClass(type);

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
//				IO.println("InvocationHandlerFactory.resolveArgument, b=" + b);
				if (b == null)
					return null;
				var o = Json.parse(b);
				if (o instanceof Map<?, ?> m) {
					@SuppressWarnings("unchecked")
					var kk = (Collection<String>) m.keySet();
					JSON_KEYS.get().addAll(kk);
				}
				return converter.get().convert(o, type);
			}
			case "application/x-www-form-urlencoded": {
//				if (entries == null)
//					break;
//				var t = createEntryTree();
				////				t.setTypeResolver(typeResolver);
//				entries.forEach(t::add);
//				return t.convert(c);
				if (body == null)
					break;
				var b = body.get();
//				IO.println("InvocationHandlerFactory.resolveArgument, b=" + b);
				if (b == null)
					return null;
				var q = new UriQueryBuilder(b);
				var m = q.names().collect(Collectors.toMap(x -> x, x -> q.values(x).findFirst().orElse(null),
						(x, _) -> x, LinkedHashMap::new));
				return converter.get().convert(m, type);
			}
			default:
				if (c.isRecord()) {
					var tt = Arrays.stream(c.getRecordComponents()).collect(
							Collectors.toMap(x -> x.getName(), x -> x.getType(), (_, x) -> x, LinkedHashMap::new));
					var aa = tt.entrySet().stream().map(x -> {
						var t = x.getValue();
						return resolveArgument(t, exchange, null, body, null);
					}).toArray();
//					IO.println("InvocationHandlerFactory.resolveArgument, aa=" + Arrays.toString(aa));
					try {
						return Arrays.stream(aa).anyMatch(x -> x != null) ? c.getConstructors()[0].newInstance(aa)
								: null;
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				}

				if (!c.getPackageName().startsWith("java.")) {
					Object o;
					try {
						o = c.getConstructor().newInstance();
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
					for (var p : (Iterable<Property>) (() -> Reflection.properties(c).iterator())) {
						var v = resolveArgument(p.type(), exchange,
//									entries != null
//											? entries.stream().filter(x -> x.getKey().equals(n))
//													.map(Map.Entry::getValue).toArray(String[]::new)
//											: null,
//									entries,
								null, body, null);
						p.set(o, v);
					}
					return o;
				}
				break;
			}
		}
		return null;
	}

//	protected EntryTree createEntryTree() {
//		return new EntryTree();
//	}

	protected Object parseParameter(String[] strings, Type type) {
//		IO.println("InvocationHandlerFactory.parseParameter, strings=" + Arrays.toString(strings) + ", type=" + type);
		var c = Java.toClass(type);
		var o = c.isArray() || Collection.class.isAssignableFrom(c) ? strings
				: (strings != null && strings.length > 0 ? strings[0] : null);
		return new Converter(null).convert(o, type);
	}

	protected Converter converter(Class<? extends TypeResolver> type) {
		try {
			return new Converter(
					type != null && type != NullTypeResolver.class ? type.getConstructor().newInstance() : null);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	protected void render(Renderable<?> renderable, HttpExchange exchange) {
//		IO.println("InvocationHandlerFactory.render, renderable=" + renderable);
		var h = rootFactory.createHandler(renderable);
		if (h != null)
			h.handle(exchange);
	}

//	public static class EntryTree extends LinkedHashMap<String, Object> {
//
//		private static final long serialVersionUID = 2351446498774467936L;
//
//		public void add(Map.Entry<String, String> entry) {
//			Map<String, Object> m = this;
//			var ss = entry.getKey().split("\\.");
//			for (var i = 0; i < ss.length; i++) {
//				if (ss[i].endsWith("]")) {
//					var ss2 = ss[i].split("\\[", 2);
//					var i2 = Integer.parseInt(ss2[1].substring(0, ss2[1].length() - 1));
//					if (i2 < 0 || i2 >= 1000)
//						throw new RuntimeException();
//					@SuppressWarnings("unchecked")
//					var l = (List<Object>) m.computeIfAbsent(ss2[0], _ -> new ArrayList<Object>());
//					while (l.size() <= i2)
//						l.add(null);
//					if (i < ss.length - 1) {
//						@SuppressWarnings("unchecked")
//						var m2 = (Map<String, Object>) l.get(i2);
//						if (m2 == null) {
//							m2 = new LinkedHashMap<String, Object>();
//							l.set(i2, m2);
//						}
//						m = m2;
//					} else
//						l.set(i2, entry.getValue());
//				} else if (i < ss.length - 1) {
//					@SuppressWarnings("unchecked")
//					var o = (Map<String, Object>) m.computeIfAbsent(ss[i], _ -> new LinkedHashMap<String, Object>());
//					m = o;
//				} else
//					m.put(ss[i], entry.getValue());
//			}
//		}
//
//		public <T> T convert(Class<T> target) {
//			return convert(this, target);
//		}
//
//		protected <T> T convert(Map<String, Object> map, Class<T> target) {
//			if (map.containsKey("$type"))
//				try {
//					@SuppressWarnings("unchecked")
//					var c = (Class<T>) Class.forName(target.getPackageName() + "." + map.get("$type"));
//					if (!target.isAssignableFrom(c))
//						throw new RuntimeException();
//					target = c;
//				} catch (ClassNotFoundException e) {
//					throw new RuntimeException(e);
//				}
//			BiFunction<String, Type, Object> c = (name, type) -> new Converter(null).convert(map.get(name), type);
//			try {
//				if (target.isRecord()) {
//					var oo = new ArrayList<Object>();
//					for (var x : target.getRecordComponents())
//						oo.add(c.apply(x.getName(), x.getGenericType()));
//					@SuppressWarnings("unchecked")
//					var t = (T) target.getConstructors()[0].newInstance(oo.toArray());
//					return t;
//				}
//				var z = target;
//				var t = z.getConstructor().newInstance();
//				Reflection.properties(z).forEach(x -> {
//					var o = c.apply(x.name(), x.genericType());
//					if (o != null)
//						x.set(t, o);
//				});
//				return t;
//			} catch (ReflectiveOperationException e) {
//				throw new RuntimeException(e);
//			}
//		}
//	}

//	public static void main(String[] args) throws Exception {
//	class C {
//
//		@SuppressWarnings("unused")
//		public String foo() {
//			return "bar";
//		}
//	}
//	var c = new C();
//	var f1 = new InvocationHandlerFactory();
//	f1.setToInvocation(r -> {
//		var u = r.getURI();
//		var p = u.getPath();
//		var n = p.startsWith("/") ? p.substring(1) : null;
//		Method m;
//		try {
//			m = C.class.getMethod(n);
//		} catch (NoSuchMethodException e) {
//			m = null;
//		} catch (SecurityException e) {
//			throw new RuntimeException(e);
//		}
//		return m != null ? new MethodInvocation(m, c, null) : null;
//	});
//	var f2 = new TemplateHandlerFactory();
//	var f = new DelegatingHandlerFactory();
//	{
//		var a = new WebHandlerFactory[] { f1, f2 };
//		f.setToHandler((o, d) -> {
//			if (a != null)
//				for (var g : a) {
//					var h = g.createHandler(o, d);
//					if (h != null)
//						return h;
//				}
//			return null;
//		});
//	}
//	f1.setMainFactory(f);
//
//	var is = new ByteArrayInputStream("""
//			GET /foo HTTP/1.1\r
//			Content-Length: 0\r
//			\r
//			""".getBytes());
//	var os = new ByteArrayOutputStream();
//	try (var rc = new HttpMessageReadableByteChannel(Channels.newChannel(is));
//			var rq = rc.readRequest();
//			var wc = new HttpMessageWritableByteChannel(Channels.newChannel(os));
//			var rs = wc.writeResponse()) {
//		var d = new HttpExchange();
//		d.setRequest(rq);
//		d.setResponse(rs);
//		var h = f.createHandler(rq, d);
//		h.handle(d);
//	}
//
//	var s = os.toString();
//	IO.println(s);
//	assert Objects.equals(s, """
//			HTTP/1.1 200 OK\r
//			Cache-Control: no-cache\r
//			Content-Length: 4\r
//			\r
//			bar
//			""") : s;
//}
}

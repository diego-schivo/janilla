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
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Converter;
import com.janilla.java.DefaultConverter;
import com.janilla.java.DollarTypeResolver;
import com.janilla.java.Java;
import com.janilla.java.TypeResolver;
import com.janilla.java.TypedData;
import com.janilla.json.Json;

public abstract class AbstractWebApp<C extends WebAppConfig> implements WebApp<C> {

	protected static WebAppConfig newConfig(Class<?>[] classes, String path, DiFactory factory) {
//		Stream<Path> ff;
//		ff = Arrays.stream(classes).map(x -> {
//			var r = x.getResource("config.json");
//			try {
//				return r != null ? Path.of(r.toURI()) : null;
//			} catch (URISyntaxException e) {
//				throw new RuntimeException(e);
//			}
//		}).filter(x -> x != null);
		Stream<Map<?, ?>> mm = Arrays.stream(classes).map(x -> toConfigMap(x));

		if (path != null) {
//			var f = Path.of(path.startsWith("~") ? System.getProperty("user.home") + path.substring(1) : path);
			var m = toConfigMap(path);
			mm = Stream.concat(mm, Stream.of(m));
		}

//		var mm = ff.map(f -> {
//			Object o;
//			try {
//				var s = Files.readString(f);
//				o = Json.parse(s);
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
//			return (Map<?, ?>) o;
//		}).toArray(Map<?, ?>[]::new);

		return newConfig(mm.toArray(Map<?, ?>[]::new), factory);
	}

	protected static Map<?, ?> toConfigMap(Class<?> type) {
		var r = type.getResource("config.json");
		Path f;
		try {
			f = r != null ? Path.of(r.toURI()) : null;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return f != null ? toConfigMap(f) : null;
	}

	protected static Map<?, ?> toConfigMap(String path) {
		var f = Path.of(path.startsWith("~") ? System.getProperty("user.home") + path.substring(1) : path);
		return f != null ? toConfigMap(f) : null;
	}

	protected static Map<?, ?> toConfigMap(Path file) {
		Object o;
		try {
			var s = Files.readString(file);
			o = Json.parse(s);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return (Map<?, ?>) o;
	}

	protected static WebAppConfig newConfig(Map<?, ?>[] maps, DiFactory factory) {
		var m = Arrays.stream(maps).flatMap(x -> x.entrySet().stream()).filter(x -> x.getValue() != null)
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue(), (_, x) -> x, LinkedHashMap::new));

		return new DefaultConverter(new TypeResolver() {

			@Override
			public TypedData apply(TypedData t) {
				return t.type() instanceof Class<?> c && c.isInterface() ? t.withType(factory.classFor(c)) : null;
			}

			@Override
			public Class<?> parse(String string) {
				throw new UnsupportedOperationException();
			}

			@Override
			public String format(Class<?> type) {
				throw new UnsupportedOperationException();
			}
		}).convert(m, factory.classFor(WebAppConfig.class));
	}

	protected static void serve(WebApp<?> app) {
		var c = sslContext(app.config());

		HttpServer s;
		{
			var e = new InetSocketAddress(app.config().httpServer().port());
			var h = app.httpHandler();
			s = app.diFactory().newInstance(app.diFactory().classFor(HttpServer.class),
					Java.hashMap("endpoint", e, "sslContext", c, "handler", h));
		}
		s.serve();
	}

	protected static SSLContext sslContext(WebAppConfig config) {
		var k = config.httpServer().keystore();
		if (k != null) {
			var p = k.path();
			var w = k.password();
			if (p.startsWith("~"))
				p = System.getProperty("user.home") + p.substring(1);
			var f = Path.of(p);
			if (!Files.exists(f)) {
				var cn = k.commonName();
				var san = k.subjectAlternativeName();
				Java.generateKeyPair(cn != null ? cn : "localhost", f, w,
						san != null ? san : "dns:localhost,ip:127.0.0.1");
			}
			try (var s = Files.newInputStream(f)) {
				return Java.sslContext(s, w.toCharArray());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return null;
	}

	protected final C config;

	protected final Converter converter;

	protected final DiFactory diFactory;

	protected final HttpHandler httpHandler;

	protected final InvocationResolver invocationResolver;

	protected final RenderableFactory renderableFactory;

	protected final List<Class<?>> resolvables;

	protected final TypeResolver typeResolver;

	protected AbstractWebApp(C config, DiFactory diFactory) {
		this.config = config;
		this.diFactory = diFactory;
		diFactory.context(this);

		{
			Map<String, Class<?>> m = diFactory.types().stream().filter(x -> x.getEnclosingClass() == null)
					.collect(Collectors.toMap(x -> x.getSimpleName(), x -> x, (_, x) -> x, LinkedHashMap::new));
//			IO.println("AbstractBackend.newInvocationResolver, m=" + m);
			resolvables = m.values().stream().toList();
		}
		typeResolver = diFactory.newInstance(diFactory.classFor(DollarTypeResolver.class));
		converter = newConverter();
		invocationResolver = newInvocationResolver();
		renderableFactory = newRenderableFactory();
		httpHandler = newHttpHandler();
	}

	@Override
	public C config() {
		return config;
	}

	public Converter converter() {
		return converter;
	}

	@Override
	public DiFactory diFactory() {
		return diFactory;
	}

	@Override
	public HttpHandler httpHandler() {
		return httpHandler;
	}

	public InvocationResolver invocationResolver() {
		return invocationResolver;
	}

	public RenderableFactory renderableFactory() {
		return renderableFactory;
	}

	public List<Class<?>> resolvables() {
		return resolvables;
	}

	public TypeResolver typeResolver() {
		return typeResolver;
	}

	protected Converter newConverter() {
		return diFactory.newInstance(diFactory.classFor(Converter.class));
	}

	protected HttpHandler newHttpHandler() {
		var f = diFactory.newInstance(diFactory.classFor(ApplicationHandlerFactory.class));
		return x -> {
			var h = f.createHandler(Objects.requireNonNullElse(x.exception(), x.request()));
			if (h == null)
				throw new NotFoundException(
						x.request().getHeaderValue(":method") + " " + x.request().getHeaderValue(":path"));
			return ScopedValue.where(INSTANCE, this).call(() -> h.handle(x));
		};
	}

	protected InvocationResolver newInvocationResolver() {
		return diFactory.newInstance(diFactory.classFor(InvocationResolver.class), Map.of("invocables",
				diFactory.types().stream().filter(x -> !(x.isInterface() || Modifier.isAbstract(x.getModifiers())))
						.flatMap(x -> Arrays.stream(x.getMethods())
								.filter(y -> !Modifier.isStatic(y.getModifiers()) && !y.isBridge())
								.map(y -> new Invocable(x, y)))
						.toList(),
				"instanceResolver", (Function<Class<?>, Object>) x -> {
					var y = diFactory.context();
//							IO.println("x=" + x + ", y=" + y);
					return x.isAssignableFrom(y.getClass()) ? y : diFactory.newInstance(diFactory.classFor(x));
				}));
	}

	protected RenderableFactory newRenderableFactory() {
		return diFactory.newInstance(diFactory.classFor(RenderableFactory.class));
	}
}

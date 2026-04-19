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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.java.Converter;
import com.janilla.java.DollarTypeResolver;
import com.janilla.java.Java;
import com.janilla.java.TypeResolver;

public abstract class AbstractWebApp implements WebApp {

	protected static void serve(DiFactory diFactory, String configurationPath) {
		WebApp a;
		{
			var f = configurationPath != null ? Path.of(
					configurationPath.startsWith("~") ? System.getProperty("user.home") + configurationPath.substring(1)
							: configurationPath)
					: null;
			a = diFactory.newInstance(diFactory.classFor(WebApp.class),
					Java.hashMap("diFactory", diFactory, "configurationFile", f));
		}

		var c = sslContext(a.configuration(), a.configurationKey());

		HttpServer s;
		{
			var p = Integer.parseInt(a.configuration().getProperty(a.configurationKey() + ".server.port"));
			s = diFactory.newInstance(diFactory.classFor(HttpServer.class),
					Java.hashMap("endpoint", new InetSocketAddress(p), "sslContext", c, "handler", a.httpHandler()));
		}
		s.serve();
	}

	protected static SSLContext sslContext(Configuration configuration, String configurationKey) {
		var p = configuration.getProperty(configurationKey + ".server.keystore.path");
		if (p != null) {
			var w = configuration.getProperty(configurationKey + ".server.keystore.password");
			if (p.startsWith("~"))
				p = System.getProperty("user.home") + p.substring(1);
			var f = Path.of(p);
			if (!Files.exists(f)) {
				var cn = configuration.getProperty(configurationKey + ".server.keystore.common-name");
				var san = configuration.getProperty(configurationKey + ".server.keystore.subject-alternative-name");
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

	protected final Configuration configuration;

	protected final Path configurationFile;

	protected final String configurationKey;

	protected final Converter converter;

	protected final DiFactory diFactory;

	protected final HttpHandler httpHandler;

	protected final InvocationResolver invocationResolver;

	protected final RenderableFactory renderableFactory;

	protected final List<Class<?>> resolvables;

	protected final TypeResolver typeResolver;

	protected AbstractWebApp(DiFactory diFactory, Path configurationFile, String configurationKey) {
		this.diFactory = diFactory;
		this.configurationFile = configurationFile;
		this.configurationKey = configurationKey;
		diFactory.context(this);

		configuration = newConfiguration();
		{
			Map<String, Class<?>> m = diFactory.types().stream().filter(x -> x.getEnclosingClass() == null)
					.collect(Collectors.toMap(x -> x.getSimpleName(), x -> x, (_, x) -> x, LinkedHashMap::new));
//			IO.println("AbstractBackend.newInvocationResolver, m=" + m);
			resolvables = m.values().stream().toList();
		}
		typeResolver = diFactory.newInstance(diFactory.classFor(DollarTypeResolver.class));
		converter = diFactory.newInstance(diFactory.classFor(Converter.class));
		invocationResolver = newInvocationResolver();
		renderableFactory = newRenderableFactory();
		httpHandler = newHttpHandler();
	}

	@Override
	public Configuration configuration() {
		return configuration;
	}

	public Path configurationFile() {
		return configurationFile;
	}

	@Override
	public String configurationKey() {
		return configurationKey;
	}

	public Converter converter() {
		return converter;
	}

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

	protected Configuration newConfiguration() {
		return diFactory.newInstance(diFactory.classFor(Configuration.class),
				Collections.singletonMap("path", configurationFile));
	}

	protected HttpHandler newHttpHandler() {
		var f = diFactory.newInstance(diFactory.classFor(ApplicationHandlerFactory.class));
		return x -> {
			var h = f.createHandler(Objects.requireNonNullElse(x.exception(), x.request()));
			if (h == null)
				throw new NotFoundException(
						x.request().getHeaderValue(":method") + " " + x.request().getHeaderValue(":path"));
			return h.handle(x);
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

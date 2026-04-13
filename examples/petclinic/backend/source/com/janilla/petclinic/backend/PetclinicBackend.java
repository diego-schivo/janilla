/*
 * Copyright 2012-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.janilla.petclinic.backend;

import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.persistence.PersistenceBuilder;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.java.Converter;
import com.janilla.java.DollarTypeResolver;
import com.janilla.java.Java;
import com.janilla.java.TypeResolver;
import com.janilla.persistence.Store;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Invocable;
import com.janilla.web.InvocationResolver;
import com.janilla.web.NotFoundException;
import com.janilla.web.RenderableFactory;

/**
 * @author Diego Schivo
 * @author Dave Syer
 */
public class PetclinicBackend {

	public static final String[] DI_PACKAGES = { "com.janilla.http", "com.janilla.web", "com.janilla.petclinic",
			"com.janilla.petclinic.backend" };

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(
				Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageTypes(x)).toList());
		serve(f, args.length > 0 ? args[0] : null);
	}

	protected static void serve(DiFactory diFactory, String configurationPath) {
		PetclinicBackend a;
		{
			var cf = configurationPath != null ? Path.of(
					configurationPath.startsWith("~") ? System.getProperty("user.home") + configurationPath.substring(1)
							: configurationPath)
					: null;
			a = diFactory.newInstance(diFactory.classFor(PetclinicBackend.class),
					Java.hashMap("diFactory", diFactory, "configurationFile", cf));
		}

		HttpServer s;
		{
			var p = Integer.parseInt(a.configuration.getProperty("petclinic.server.port"));
			s = a.diFactory.newInstance(a.diFactory.classFor(HttpServer.class),
					Map.of("endpoint", new InetSocketAddress(p), "handler", a.handler));
		}
		s.serve();
	}

	protected final Configuration configuration;

	protected final Converter converter;

	protected final DiFactory diFactory;

	protected final HttpHandler handler;

	protected final InvocationResolver invocationResolver;

	protected final Persistence persistence;

	protected final RenderableFactory renderableFactory;

	protected final List<Class<?>> resolvables;

	protected final List<Class<?>> storables;

	protected final TypeResolver typeResolver;

	public PetclinicBackend(DiFactory diFactory, Path configurationFile) {
//		IO.println("PetclinicBackend, configurationFile=" + configurationFile);
		this.diFactory = diFactory;
		diFactory.context(this);
		configuration = diFactory.newInstance(diFactory.classFor(Configuration.class),
				Collections.singletonMap("path", configurationFile));

		{
			Map<String, Class<?>> m = diFactory.types().stream()
					.collect(Collectors.toMap(x -> x.getSimpleName(), x -> x, (_, x) -> x, LinkedHashMap::new));
			resolvables = m.values().stream().toList();
		}
		typeResolver = diFactory.newInstance(diFactory.classFor(DollarTypeResolver.class));
		converter = diFactory.newInstance(diFactory.classFor(Converter.class));

		storables = resolvables.stream().filter(x -> x.isAnnotationPresent(Store.class)).toList();
		{
			var f = configuration.getProperty("petclinic.database.file");
			if (f.startsWith("~"))
				f = System.getProperty("user.home") + f.substring(1);
			var b = diFactory.newInstance(diFactory.classFor(PersistenceBuilder.class),
					Map.of("databaseFile", Path.of(f)));
			persistence = b.build(diFactory);
		}

		invocationResolver = diFactory.newInstance(diFactory.classFor(InvocationResolver.class), Map.of("invocables",
				diFactory.types().stream().filter(x -> !(x.isInterface() || Modifier.isAbstract(x.getModifiers())))
						.flatMap(x -> Arrays.stream(x.getMethods())
								.filter(y -> !Modifier.isStatic(y.getModifiers()) && !y.isBridge())
								.map(y -> new Invocable(x, y)))
						.toList(),
				"instanceResolver", (Function<Class<?>, Object>) x -> {
					var y = diFactory.context();
//							IO.println("x=" + x + ", y=" + y);
					return x.isAssignableFrom(y.getClass()) ? diFactory.context()
							: diFactory.newInstance(diFactory.classFor(x));
				}));
		renderableFactory = diFactory.newInstance(diFactory.classFor(RenderableFactory.class));
		{
			var f = diFactory.newInstance(diFactory.classFor(ApplicationHandlerFactory.class));
			handler = x -> {
				var h = f.createHandler(Objects.requireNonNullElse(x.exception(), x.request()));
				if (h == null)
					throw new NotFoundException(x.request().getMethod() + " " + x.request().getTarget());
				return h.handle(x);
			};
		}
	}

	public Configuration configuration() {
		return configuration;
	}

	public Converter converter() {
		return converter;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public HttpHandler handler() {
		return handler;
	}

	public InvocationResolver invocationResolver() {
		return invocationResolver;
	}

	public Persistence persistence() {
		return persistence;
	}

	public RenderableFactory renderableFactory() {
		return renderableFactory;
	}

	public List<Class<?>> resolvables() {
		return resolvables;
	}

	public List<Class<?>> storables() {
		return storables;
	}

	public TypeResolver typeResolver() {
		return typeResolver;
	}
}

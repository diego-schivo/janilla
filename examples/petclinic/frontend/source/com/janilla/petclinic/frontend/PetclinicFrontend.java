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
package com.janilla.petclinic.frontend;

import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.janilla.http.HttpClient;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.java.Java;
import com.janilla.petclinic.OwnerApi;
import com.janilla.petclinic.PetApi;
import com.janilla.petclinic.PetTypeApi;
import com.janilla.petclinic.VetApi;
import com.janilla.petclinic.VisitApi;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Invocable;
import com.janilla.web.InvocationResolver;
import com.janilla.web.NotFoundException;
import com.janilla.web.RenderableFactory;
import com.janilla.web.ResourceMap;

/**
 * @author Diego Schivo
 * @author Dave Syer
 */
public class PetclinicFrontend {

	public static final String[] DI_PACKAGES = { "com.janilla.http", "com.janilla.web",
			"com.janilla.petclinic.frontend" };

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(
				Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageTypes(x, false)).toList());
		serve(f, args.length > 0 ? args[0] : null);
	}

	protected static void serve(DiFactory diFactory, String configurationPath) {
		PetclinicFrontend a;
		{
			var cf = configurationPath != null ? Path.of(
					configurationPath.startsWith("~") ? System.getProperty("user.home") + configurationPath.substring(1)
							: configurationPath)
					: null;
			a = diFactory.newInstance(diFactory.classFor(PetclinicFrontend.class),
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

//	protected static SSLContext sslContext(Configuration configuration) {
//		var p = configuration.getProperty("petclinic.server.keystore.path");
//		if (p == null)
//			return HttpClient.sslContext("TLSv1.3");
//		var w = configuration.getProperty("petclinic.server.keystore.password");
//		if (p.startsWith("~"))
//			p = System.getProperty("user.home") + p.substring(1);
//		var f = Path.of(p);
//		if (!Files.exists(f))
//			Java.generateKeyPair("localhost", f, w, "dns:localhost,ip:127.0.0.1");
//		try (var s = Files.newInputStream(f)) {
//			return Java.sslContext(s, w.toCharArray());
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//	}

	protected final Configuration configuration;

	protected final DiFactory diFactory;

	protected final HttpHandler handler;

	protected final HttpClient httpClient;

	protected final InvocationResolver invocationResolver;

	protected final OwnerApi ownerApi;

	protected final PetApi petApi;

	protected final PetTypeApi petTypeApi;

	protected final RenderableFactory renderableFactory;

	protected final ResourceMap resourceMap;

	protected final VetApi vetApi;

	protected final VisitApi visitApi;

	public PetclinicFrontend(DiFactory diFactory, Path configurationFile) {
		this.diFactory = diFactory;
		diFactory.context(this);
		configuration = diFactory.newInstance(diFactory.classFor(Configuration.class),
				Collections.singletonMap("path", configurationFile));

		httpClient = diFactory.newInstance(diFactory.classFor(HttpClient.class));
		ownerApi = diFactory.newInstance(diFactory.classFor(OwnerApi.class));
		petApi = diFactory.newInstance(diFactory.classFor(PetApi.class));
		petTypeApi = diFactory.newInstance(diFactory.classFor(PetTypeApi.class));
		vetApi = diFactory.newInstance(diFactory.classFor(VetApi.class));
		visitApi = diFactory.newInstance(diFactory.classFor(VisitApi.class));

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
		resourceMap = diFactory.newInstance(diFactory.classFor(ResourceMap.class), Map.of("paths",
				Map.of("/base",
						Java.getPackagePaths("com.janilla.frontend", false).filter(Files::isRegularFile).toList(), "",
						Java.getPackagePaths(PetclinicFrontend.class.getPackageName(), false)
								.filter(Files::isRegularFile).toList())));
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

	public DiFactory diFactory() {
		return diFactory;
	}

	public HttpHandler handler() {
		return handler;
	}

	public HttpClient httpClient() {
		return httpClient;
	}

	public InvocationResolver invocationResolver() {
		return invocationResolver;
	}

	public OwnerApi ownerApi() {
		return ownerApi;
	}

	public PetApi petApi() {
		return petApi;
	}

	public PetTypeApi petTypeApi() {
		return petTypeApi;
	}

	public RenderableFactory renderableFactory() {
		return renderableFactory;
	}

	public ResourceMap resourceMap() {
		return resourceMap;
	}

	public VetApi vetApi() {
		return vetApi;
	}

	public VisitApi visitApi() {
		return visitApi;
	}
}

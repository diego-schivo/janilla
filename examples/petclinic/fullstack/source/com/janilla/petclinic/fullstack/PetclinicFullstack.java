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
package com.janilla.petclinic.fullstack;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.java.Java;
import com.janilla.petclinic.backend.PetclinicBackend;
import com.janilla.petclinic.frontend.PetclinicFrontend;

/**
 * @author Diego Schivo
 * @author Dave Syer
 */
public class PetclinicFullstack {

	public static final String[] DI_PACKAGES = { "com.janilla.http", "com.janilla.web",
			"com.janilla.petclinic.fullstack" };

	public static final ScopedValue<PetclinicFullstack> INSTANCE = ScopedValue.newInstance();

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageTypes(x)).toList(),
				"fullstack");
		serve(f, args.length > 0 ? args[0] : null);
	}

	protected static void serve(DiFactory diFactory, String configurationPath) {
		PetclinicFullstack a;
		{
			var cf = configurationPath != null ? Path.of(
					configurationPath.startsWith("~") ? System.getProperty("user.home") + configurationPath.substring(1)
							: configurationPath)
					: null;
			a = diFactory.newInstance(diFactory.classFor(PetclinicFullstack.class),
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

	protected final PetclinicBackend backend;

	protected final Configuration configuration;

	protected final DiFactory diFactory;

	protected final PetclinicFrontend frontend;

	protected final HttpHandler handler;

	public PetclinicFullstack(DiFactory diFactory, Path configurationFile) {
		this.diFactory = diFactory;
		diFactory.context(this);

		configuration = diFactory.newInstance(diFactory.classFor(Configuration.class),
				Collections.singletonMap("path", configurationFile));

		Path cf;
		try {
			cf = configurationFile != null ? configurationFile
					: Path.of(PetclinicFullstack.class.getResource("configuration.properties").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		backend = ScopedValue.where(INSTANCE, this).call(() -> {
			var f = new DefaultDiFactory(Stream
					.concat(Arrays.stream(PetclinicBackend.DI_PACKAGES), Stream.of("com.janilla.petclinic.fullstack"))
					.flatMap(x -> Java.getPackageTypes(x)).toList(), "backend");
			return diFactory.newInstance(diFactory.classFor(PetclinicBackend.class),
					Java.hashMap("diFactory", f, "configurationFile", cf));
		});

		frontend = ScopedValue.where(INSTANCE, this).call(() -> {
			var f = new DefaultDiFactory(Stream
					.concat(Arrays.stream(PetclinicFrontend.DI_PACKAGES), Stream.of("com.janilla.petclinic.fullstack"))
					.flatMap(x -> Java.getPackageTypes(x)).toList(), "frontend");
			return diFactory.newInstance(diFactory.classFor(PetclinicFrontend.class),
					Java.hashMap("diFactory", f, "configurationFile", cf));
		});

		handler = x -> {
			var h = x.request().getPath().startsWith("/api/") ? backend.handler() : frontend.handler();
			return h.handle(x);
		};
	}

	public PetclinicBackend backend() {
		return backend;
	}

	public Configuration configuration() {
		return configuration;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public PetclinicFrontend frontend() {
		return frontend;
	}

	public HttpHandler handler() {
		return handler;
	}
}

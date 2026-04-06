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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpClient;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.petclinic.backend.BackendExchange;
import com.janilla.petclinic.backend.PetclinicBackend;
import com.janilla.petclinic.frontend.PetclinicFrontend;

/**
 * @author Diego Schivo
 * @author Dave Syer
 */
public class PetclinicFullstack {

	public static final ScopedValue<PetclinicFullstack> INSTANCE = ScopedValue.newInstance();

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DefaultDiFactory(Java.getPackageClasses(PetclinicFullstack.class.getPackageName(), false), "fullstack");
		serve(f, args.length > 0 ? args[0] : null);
	}

	protected static void serve(DiFactory diFactory, String configurationPath) {
		PetclinicFullstack a;
		{
			a = diFactory.newInstance(diFactory.classFor(PetclinicFullstack.class),
					Java.hashMap("diFactory", diFactory, "configurationFile",
							configurationPath != null ? Path.of(configurationPath.startsWith("~")
									? System.getProperty("user.home") + configurationPath.substring(1)
									: configurationPath) : null));
		}

		SSLContext c;
		{
			var p = a.configuration.getProperty("petclinic.server.keystore.path");
			if (p != null) {
				var w = a.configuration.getProperty("petclinic.server.keystore.password");
				if (p.startsWith("~"))
					p = System.getProperty("user.home") + p.substring(1);
				var f = Path.of(p);
				if (!Files.exists(f))
					Java.generateKeyPair("localhost", f, w, "dns:localhost,ip:127.0.0.1");
				try (var s = Files.newInputStream(f)) {
					c = Java.sslContext(s, w.toCharArray());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} else
				c = HttpClient.sslContext("TLSv1.3");
		}

		HttpServer s;
		{
			var p = Integer.parseInt(a.configuration.getProperty("petclinic.server.port"));
			s = a.diFactory.newInstance(a.diFactory.classFor(HttpServer.class),
					Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
		}
		s.serve();
	}

	protected final PetclinicBackend backend;

	protected final Properties configuration;

	protected final DiFactory diFactory;

	protected final PetclinicFrontend frontend;

	protected final HttpHandler handler;

	public PetclinicFullstack(DiFactory diFactory, Path configurationFile) {
		this.diFactory = diFactory;
		diFactory.context(this);
		configuration = diFactory.newInstance(diFactory.classFor(Properties.class),
				Collections.singletonMap("file", configurationFile));

		var cf = Optional.ofNullable(configurationFile).orElseGet(() -> {
			try {
				return Path.of(PetclinicFullstack.class.getResource("configuration.properties").toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		});
		backend = ScopedValue.where(INSTANCE, this)
				.call(() -> diFactory.newInstance(diFactory.classFor(PetclinicBackend.class), Java.hashMap("diFactory",
						new DefaultDiFactory(Stream
								.of("com.janilla.web", "com.janilla.petclinic", "com.janilla.petclinic.backend",
										"com.janilla.petclinic.fullstack")
								.flatMap(x -> Java.getPackageClasses(x, false).stream()).toList(), "backend"),
						"configurationFile", cf)));
		frontend = ScopedValue.where(INSTANCE, this)
				.call(() -> diFactory.newInstance(diFactory.classFor(PetclinicFrontend.class), Java.hashMap("diFactory",
						new DefaultDiFactory(Stream
								.of("com.janilla.http", "com.janilla.web", "com.janilla.petclinic.frontend",
										"com.janilla.petclinic.fullstack")
								.flatMap(x -> Java.getPackageClasses(x, false).stream()).toList(), "frontend"),
						"configurationFile", cf)));

		handler = x -> {
			var h = x instanceof BackendExchange ? backend.handler() : frontend.handler();
			return h.handle(x);
		};
	}

	public PetclinicBackend backend() {
		return backend;
	}

	public Properties configuration() {
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

/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.blanktemplate.fullstack;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.blanktemplate.backend.BlankBackend;
import com.janilla.blanktemplate.frontend.BlankFrontend;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.java.Java;

public class BlankFullstack {

//	public static final String[] DI_BACKEND_PACKAGES = Stream
//			.concat(Arrays.stream(BlankBackend.DI_PACKAGES), Stream.of("com.janilla.blanktemplate.fullstack"))
//			.toArray(String[]::new);
//
//	public static final String[] DI_FRONTEND_PACKAGES = Stream
//			.concat(Arrays.stream(BlankFrontend.DI_PACKAGES), Stream.of("com.janilla.blanktemplate.fullstack"))
//			.toArray(String[]::new);
//
//	public static final String[] DI_PACKAGES = { "com.janilla.blanktemplate.fullstack" };

	public static final ScopedValue<BlankFullstack> INSTANCE = ScopedValue.newInstance();

	public static Stream<Class<?>> diTypes() {
		return Java.getPackageTypes("com.janilla.blanktemplate.fullstack");
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DefaultDiFactory(diTypes().toList(), "fullstack");
		serve(f, BlankFullstack.class, args.length > 0 ? args[0] : null);
	}

	protected static <T extends BlankFullstack> void serve(DiFactory diFactory, Class<T> applicationType,
			String configurationPath) {
		T a;
		{
			var cf = configurationPath != null ? Path.of(
					configurationPath.startsWith("~") ? System.getProperty("user.home") + configurationPath.substring(1)
							: configurationPath)
					: null;
			a = diFactory.newInstance(applicationType, Java.hashMap("diFactory", diFactory, "configurationFile", cf));
		}

//		SSLContext c;
//		{
//			var p = a.configuration.getProperty(a.configurationKey + ".server.keystore.path");
//			if (p != null) {
//				var w = a.configuration.getProperty(a.configurationKey + ".server.keystore.password");
//				if (p.startsWith("~"))
//					p = System.getProperty("user.home") + p.substring(1);
//				var f = Path.of(p);
//				if (!Files.exists(f)) {
//					var cn = a.configuration.getProperty(a.configurationKey + ".server.keystore.common-name");
//					var san = a.configuration
//							.getProperty(a.configurationKey + ".server.keystore.subject-alternative-name");
//					Java.generateKeyPair(cn != null ? cn : "localhost", f, w,
//							san != null ? san : "dns:localhost,ip:127.0.0.1");
//				}
//				try (var s = Files.newInputStream(f)) {
//					c = Java.sslContext(s, w.toCharArray());
//				} catch (IOException e) {
//					throw new UncheckedIOException(e);
//				}
//			} else
//				c = DefaultHttpClient.sslContext("TLSv1.3");
//		}

		HttpServer s;
		{
			var p = Integer.parseInt(a.configuration.getProperty(a.configurationKey + ".server.port"));
			s = a.diFactory.newInstance(a.diFactory.classFor(HttpServer.class),
					Map.of("endpoint", new InetSocketAddress(p), "handler", a.handler));
		}
		s.serve();
	}

	protected final BlankBackend backend;

	protected final Configuration configuration;

	protected final Path configurationFile;

	protected final String configurationKey;

	protected final DiFactory diFactory;

	protected final BlankFrontend frontend;

	protected final HttpHandler handler;

	public BlankFullstack(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "blank-template");
	}

	public BlankFullstack(DiFactory diFactory, Path configurationFile, String configurationKey) {
		this.diFactory = diFactory;
		this.configurationFile = configurationFile;
		this.configurationKey = configurationKey;
		diFactory.context(this);
		configuration = diFactory.newInstance(diFactory.classFor(Configuration.class),
				Collections.singletonMap("path", configurationFile));

		Path cf;
		try {
			cf = configurationFile != null ? configurationFile
					: Path.of(getClass().getResource("configuration.properties").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		backend = ScopedValue.where(INSTANCE, this).call(() -> {
			var f = new DefaultDiFactory(diBackendTypes().toList(), "backend");
			return f.newInstance(f.classFor(BlankBackend.class),
					Java.hashMap("diFactory", f, "configurationFile", cf, "configurationKey", configurationKey));
		});

		frontend = ScopedValue.where(INSTANCE, this).call(() -> {
			var f = new DefaultDiFactory(diFrontendTypes().toList(), "frontend");
			return f.newInstance(f.classFor(BlankFrontend.class),
					Java.hashMap("diFactory", f, "configurationFile", cf, "configurationKey", configurationKey));
		});

		handler = this::handle;
	}

	public BlankBackend backend() {
		return backend;
	}

	public Configuration configuration() {
		return configuration;
	}

	public String configurationKey() {
		return configurationKey;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public BlankFrontend frontend() {
		return frontend;
	}

	public HttpHandler handler() {
		return handler;
	}

	protected Stream<Class<?>> diBackendTypes() {
		return Stream.concat(BlankBackend.diTypes(), Java.getPackageTypes("com.janilla.blanktemplate.fullstack"));
	};

	protected Stream<Class<?>> diFrontendTypes() {
		return Stream.concat(BlankFrontend.diTypes(), Java.getPackageTypes("com.janilla.blanktemplate.fullstack"));
	};

	protected boolean handle(HttpExchange exchange) {
//		IO.println("BlankFullstack.handle, exchange=" + exchange);
		var h = exchange.request().getPath().startsWith("/api/") ? backend.handler() : frontend.handler();
		return h.handle(exchange);
	}
}

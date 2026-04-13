/*
 * MIT License
 *
 * Copyright (c) 2024 Vercel, Inc.
 * Copyright (c) 2024-2026 Diego Schivo
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
package com.janilla.acmedashboard.fullstack;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.acmedashboard.backend.AcmeDashboardBackend;
import com.janilla.acmedashboard.frontend.AcmeDashboardFrontend;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.java.Java;

public class AcmeDashboardFullstack {

	public static final ScopedValue<AcmeDashboardFullstack> INSTANCE = ScopedValue.newInstance();

	public static Stream<Class<?>> diTypes() {
		return Stream.concat(
				Java.getPackageTypes("com.janilla", x -> !x.endsWith(".cms") && !x.equals("com.janilla.acmedashboard")),
				Java.getPackageTypes("com.janilla.acmedashboard.fullstack"));
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList(), "fullstack");
		serve(f, args.length > 0 ? args[0] : null);
	}

	protected static void serve(DiFactory diFactory, String configurationPath) {
		AcmeDashboardFullstack a;
		{
			var cf = configurationPath != null ? Path.of(
					configurationPath.startsWith("~") ? System.getProperty("user.home") + configurationPath.substring(1)
							: configurationPath)
					: null;
			a = diFactory.newInstance(diFactory.classFor(AcmeDashboardFullstack.class),
					Java.hashMap("diFactory", diFactory, "configurationFile", cf));
		}

		HttpServer s;
		{
			var p = Integer.parseInt(a.configuration.getProperty("acme-dashboard.server.port"));
			s = a.diFactory.newInstance(a.diFactory.classFor(HttpServer.class),
					Map.of("endpoint", new InetSocketAddress(p), "handler", a.handler));
		}
		s.serve();
	}

	protected final AcmeDashboardBackend backend;

	protected final Configuration configuration;

	protected final DiFactory diFactory;

	protected final AcmeDashboardFrontend frontend;

	protected final HttpHandler handler;

	public AcmeDashboardFullstack(DiFactory diFactory, Path configurationFile) {
		this.diFactory = diFactory;
		diFactory.context(this);

		configuration = diFactory.newInstance(diFactory.classFor(Configuration.class),
				Collections.singletonMap("path", configurationFile));

		Path cf;
		try {
			cf = configurationFile != null ? configurationFile
					: Path.of(AcmeDashboardFullstack.class.getResource("configuration.properties").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		backend = ScopedValue.where(INSTANCE, this).call(() -> {
			var f = new DefaultDiFactory(Stream
					.concat(AcmeDashboardBackend.diTypes(), Java.getPackageTypes("com.janilla.acmedashboard.fullstack"))
					.toList(), "backend");
			return diFactory.newInstance(diFactory.classFor(AcmeDashboardBackend.class),
					Java.hashMap("diFactory", f, "configurationFile", cf));
		});

		frontend = ScopedValue.where(INSTANCE, this).call(() -> {
			var f = new DefaultDiFactory(Stream.concat(AcmeDashboardFrontend.diTypes(),
					Java.getPackageTypes("com.janilla.acmedashboard.fullstack")).toList(), "frontend");
			return diFactory.newInstance(diFactory.classFor(AcmeDashboardFrontend.class),
					Java.hashMap("diFactory", f, "configurationFile", cf));
		});

		handler = x -> {
			var h = x.request().getPath().startsWith("/api/") ? backend.handler() : frontend.handler();
			return h.handle(x);
		};
	}

	public AcmeDashboardBackend backend() {
		return backend;
	}

	public Configuration configuration() {
		return configuration;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public AcmeDashboardFrontend frontend() {
		return frontend;
	}

	public HttpHandler handler() {
		return handler;
	}
}

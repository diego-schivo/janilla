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
package com.janilla.acmedashboard.test;

import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import com.janilla.acmedashboard.fullstack.AcmeDashboardFullstack;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.java.Java;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Handle;
import com.janilla.web.Invocable;
import com.janilla.web.InvocationResolver;
import com.janilla.web.NotFoundException;
import com.janilla.web.Render;
import com.janilla.web.RenderableFactory;
import com.janilla.web.ResourceMap;

public class AcmeDashboardTest {

	public static Stream<Class<?>> diTypes() {
		return Stream.concat(
				Java.getPackageTypes("com.janilla", x -> !x.endsWith(".cms") && !x.equals("com.janilla.acmedashboard")),
				Java.getPackageTypes("com.janilla.acmedashboard.test"));
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DefaultDiFactory(diTypes().toList());
		serve(f, args.length > 0 ? args[0] : null);
	}

	protected static void serve(DiFactory diFactory, String configurationPath) {
		AcmeDashboardTest a;
		{
			a = diFactory.newInstance(diFactory.classFor(AcmeDashboardTest.class),
					Java.hashMap("diFactory", diFactory, "configurationFile",
							configurationPath != null ? Path.of(configurationPath.startsWith("~")
									? System.getProperty("user.home") + configurationPath.substring(1)
									: configurationPath) : null));
		}

		HttpServer s;
		{
			var p = Integer.parseInt(a.configuration.getProperty("acme-dashboard.server.port"));
			s = a.diFactory.newInstance(a.diFactory.classFor(HttpServer.class),
					Map.of("endpoint", new InetSocketAddress(p), "handler", a.handler));
		}
		s.serve();
	}

	protected final Configuration configuration;

	protected final DiFactory diFactory;

	protected final AcmeDashboardFullstack fullstack;

	protected final HttpHandler handler;

	protected final InvocationResolver invocationResolver;

	protected final RenderableFactory renderableFactory;

	protected final ResourceMap resourceMap;

	public AcmeDashboardTest(DiFactory diFactory, Path configurationFile) {
		this.diFactory = diFactory;
		diFactory.context(this);

		configuration = diFactory.newInstance(diFactory.classFor(Configuration.class),
				Collections.singletonMap("path", configurationFile));

		{
			var f = new DefaultDiFactory(AcmeDashboardFullstack.diTypes().toList(), "fullstack");
			fullstack = diFactory.newInstance(diFactory.classFor(AcmeDashboardFullstack.class),
					Java.hashMap("diFactory", f, "configurationFile", configurationFile));
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
		resourceMap = diFactory.newInstance(diFactory.classFor(ResourceMap.class),
				Map.of("paths", Map.of("", Stream.of("com.janilla.frontend", "com.janilla.acmedashboard.test")
						.flatMap(x -> Java.getPackagePaths(x, false).filter(Files::isRegularFile)).toList())));
		renderableFactory = diFactory.newInstance(diFactory.classFor(RenderableFactory.class));
		{
			var f = diFactory.newInstance(diFactory.classFor(ApplicationHandlerFactory.class));
			handler = ex -> {
//				IO.println("AddressBookTest, " + ex.request().getPath() + ", Test.ONGOING=" + Test.ONGOING.get());
				var h2 = Test.ONGOING.get() && !ex.request().getPath().startsWith("/test/") ? fullstack.handler()
						: (HttpHandler) y -> {
							var h = f.createHandler(Objects.requireNonNullElse(y.exception(), y.request()));
							if (h == null)
								throw new NotFoundException(y.request().getHeaderValue(":method") + " " + y.request().getHeaderValue(":path"));
							return h.handle(y);
						};
				return h2.handle(ex);
			};
		}
	}

	@Handle(method = "GET", path = "/")
	public Index root() {
		return new Index();
	}

	public Configuration configuration() {
		return configuration;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public AcmeDashboardFullstack fullstack() {
		return fullstack;
	}

	public HttpHandler handler() {
		return handler;
	}

	public InvocationResolver invocationResolver() {
		return invocationResolver;
	}

	public RenderableFactory renderableFactory() {
		return renderableFactory;
	}

	public ResourceMap resourceMap() {
		return resourceMap;
	}

	@Render(template = "index", resource = "/index.html")
	public record Index() {
	}
}

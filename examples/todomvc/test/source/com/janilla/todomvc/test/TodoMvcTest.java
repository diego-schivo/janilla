/*
 * MIT License
 *
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
package com.janilla.todomvc.test;

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

import com.janilla.frontend.IndexFactory;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Configuration;
import com.janilla.java.Java;
import com.janilla.todomvc.frontend.TodoMvcFrontend;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Invocable;
import com.janilla.web.InvocationResolver;
import com.janilla.web.NotFoundException;
import com.janilla.web.RenderableFactory;
import com.janilla.web.ResourceMap;

public class TodoMvcTest {

	public static final String[] DI_PACKAGES = Stream
			.concat(Arrays.stream(TodoMvcFrontend.DI_PACKAGES), Stream.of("com.janilla.todomvc.test"))
			.toArray(String[]::new);

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(
				Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageTypes(x)).toList());
		serve(f, args.length > 0 ? args[0] : null);
	}

	protected static void serve(DiFactory diFactory, String configurationPath) {
		TodoMvcTest a;
		{
			var cf = configurationPath != null ? Path.of(
					configurationPath.startsWith("~") ? System.getProperty("user.home") + configurationPath.substring(1)
							: configurationPath)
					: null;
			a = diFactory.newInstance(diFactory.classFor(TodoMvcTest.class),
					Java.hashMap("diFactory", diFactory, "configurationFile", cf));
		}

		HttpServer s;
		{
			var p = Integer.parseInt(a.configuration.getProperty("todomvc.server.port"));
			s = a.diFactory.newInstance(a.diFactory.classFor(HttpServer.class),
					Map.of("endpoint", new InetSocketAddress(p), "handler", a.handler));
		}
		s.serve();
	}

	protected final Configuration configuration;

	protected final DiFactory diFactory;

	protected final TodoMvcFrontend frontend;

	protected final HttpHandler handler;

	protected final IndexFactory indexFactory;

	protected final InvocationResolver invocationResolver;

	protected final RenderableFactory renderableFactory;

	protected final ResourceMap resourceMap;

	public TodoMvcTest(DiFactory diFactory, Path configurationFile) {
		this.diFactory = diFactory;
		diFactory.context(this);

		configuration = diFactory.newInstance(diFactory.classFor(Configuration.class),
				Collections.singletonMap("path", configurationFile));

		{
			var f = new DefaultDiFactory(Arrays.stream(TodoMvcFrontend.DI_PACKAGES)
					.flatMap(x -> Java.getPackageTypes(x)).toList());
			frontend = diFactory.newInstance(diFactory.classFor(TodoMvcFrontend.class),
					Java.hashMap("diFactory", f, "configurationFile", configurationFile));
		}

		resourceMap = diFactory.newInstance(diFactory.classFor(ResourceMap.class), Map.of("paths",
				Map.of("/base", Java.getPackagePaths("com.janilla.frontend").filter(Files::isRegularFile).toList(), "",
						Stream.of("com.janilla.todomvc.frontend", "com.janilla.todomvc.test")
								.flatMap(Java::getPackagePaths).filter(Files::isRegularFile).toList())));
		indexFactory = diFactory.newInstance(diFactory.classFor(IndexFactory.class));
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
			handler = ex -> {
//				IO.println(
//						"TodoMvcTest, " + ex.request().getPath() + ", Test.ongoing=" + Test.ongoing.get());
				var h2 = WebHandling.TEST_ONGOING.get() && !ex.request().getPath().startsWith("/test/")
						? frontend.handler()
						: (HttpHandler) y -> {
							var h = f.createHandler(Objects.requireNonNullElse(y.exception(), y.request()));
							if (h == null)
								throw new NotFoundException(y.request().getMethod() + " " + y.request().getTarget());
							return h.handle(y);
						};
				return h2.handle(ex);
			};
		}
	}

	public Configuration configuration() {
		return configuration;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public TodoMvcFrontend frontend() {
		return frontend;
	}

	public HttpHandler handler() {
		return handler;
	}

	public IndexFactory indexFactory() {
		return indexFactory;
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
}

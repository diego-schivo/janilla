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
package com.janilla.blanktemplate.test;

import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.janilla.blanktemplate.fullstack.BlankFullstack;
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

@Render(template = "index.html")
public class BlankTest {

	public static final String[] DI_PACKAGES = { "com.janilla.web", "com.janilla.blanktemplate.test" };

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DefaultDiFactory(
				Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageTypes(x)).toList());
		serve(f, BlankTest.class, args.length > 0 ? args[0] : null);
	}

	protected static <T extends BlankTest> void serve(DiFactory diFactory, Class<T> applicationType,
			String configurationPath) {
		T a;
		{
			a = diFactory.newInstance(applicationType,
					Java.hashMap("diFactory", diFactory, "configurationFile",
							configurationPath != null ? Path.of(configurationPath.startsWith("~")
									? System.getProperty("user.home") + configurationPath.substring(1)
									: configurationPath) : null));
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

	protected final Configuration configuration;

	protected final Path configurationFile;

	protected final String configurationKey;

	protected final DiFactory diFactory;

	protected final BlankFullstack fullstack;

	protected final HttpHandler handler;

	protected final InvocationResolver invocationResolver;

	protected final RenderableFactory renderableFactory;

	protected final ResourceMap resourceMap;

	public BlankTest(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "blank-template");
	}

	public BlankTest(DiFactory diFactory, Path configurationFile, String configurationKey) {
		this.diFactory = diFactory;
		this.configurationFile = configurationFile;
		this.configurationKey = configurationKey;
		diFactory.context(this);
		configuration = diFactory.newInstance(diFactory.classFor(Configuration.class),
				Collections.singletonMap("path", configurationFile));

		var cf = Optional.ofNullable(configurationFile).orElseGet(() -> {
			try {
				return Path.of(getClass().getResource("configuration.properties").toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		});

		{
			var f = new DefaultDiFactory(
					Arrays.stream(diFullstackPackages()).flatMap(x -> Java.getPackageTypes(x)).toList(),
					"fullstack");
			fullstack = f.newInstance(f.classFor(BlankFullstack.class),
					Java.hashMap("diFactory", f, "configurationFile", cf, "configurationKey", configurationKey));
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
		resourceMap = diFactory.newInstance(diFactory.classFor(ResourceMap.class), Map.of("paths", resourcePaths()));
		renderableFactory = diFactory.newInstance(diFactory.classFor(RenderableFactory.class));
		{
			var f = diFactory.newInstance(diFactory.classFor(ApplicationHandlerFactory.class));
			HttpHandler h0 = ex -> {
				var h = f.createHandler(Objects.requireNonNullElse(ex.exception(), ex.request()));
				if (h == null)
					throw new NotFoundException(ex.request().getMethod() + " " + ex.request().getTarget());
				return h.handle(ex);
			};
			handler = ex -> {
//				IO.println("BlankTest, " + ex.request().getPath() + ", Test.ongoing=" + Test.ONGOING.get());
				var h = Test.ONGOING.get() && !ex.request().getPath().startsWith("/test/") ? fullstack.handler() : h0;
				return h.handle(ex);
			};
		}
	}

	@Handle(method = "GET", path = "/")
	public BlankTest application() {
		return this;
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

	public BlankFullstack fullstack() {
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

	protected String[] diFullstackPackages() {
		return BlankFullstack.DI_PACKAGES;
	}

	protected Map<String, List<Path>> resourcePaths() {
		return Map.of("", Stream.of("com.janilla.frontend", "com.janilla.blanktemplate.test")
				.flatMap(x -> Java.getPackagePaths(x, false).filter(Files::isRegularFile)).toList());
	}
}

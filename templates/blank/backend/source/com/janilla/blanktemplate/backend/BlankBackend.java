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
package com.janilla.blanktemplate.backend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.janilla.backend.cms.CmsResourceHandling;
import com.janilla.backend.cms.CmsSchema;
import com.janilla.backend.web.AbstractBackend;
import com.janilla.blanktemplate.BlankDomain;
import com.janilla.http.HttpExchange;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.web.Handle;
import com.janilla.web.InvocationResolver;
import com.janilla.web.WebApp;

public class BlankBackend<C extends BlankBackendConfig> extends AbstractBackend<C> {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.cms"), Java.getPackageTypes("com.janilla.http"),
				Java.getPackageTypes("com.janilla.java"), Java.getPackageTypes("com.janilla.web"),
				Java.getPackageTypes("com.janilla.backend", _ -> true),
				Java.getPackageTypes("com.janilla.blanktemplate"),
				Java.getPackageTypes("com.janilla.blanktemplate.backend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		var c = newConfig(new Class<?>[] { BlankBackend.class }, args.length != 0 ? args[0] : null, f);
		var a = f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f));
		serve(a);
	}

	protected CmsResourceHandling cmsResourceHandling;

	protected BlankDomain domain;

	protected final Predicate<HttpExchange> drafts = this::testDrafts;

	public BlankBackend(C config, DiFactory diFactory) {
		super(config, diFactory);
	}

//	protected BlankBackend(DiFactory diFactory, Path configurationFile, String configurationKey) {
//		super(diFactory, configurationFile, configurationKey);
//		this.diFactory = diFactory;
//		this.configurationFile = configurationFile;
//		this.configurationKey = configurationKey;
//		diFactory.context(this);
//
//		configuration = diFactory.newInstance(diFactory.classFor(Configuration.class),
//				Collections.singletonMap("path", configurationFile));
//		domain = diFactory.newInstance(diFactory.classFor(BlankDomain.class));
//
//		{
//			Map<String, Class<?>> m = diFactory.types().stream().filter(x -> !x.isAnonymousClass() && !x.isLocalClass())
//					.collect(Collectors.toMap(x -> x.getSimpleName(), x -> x, (_, x) -> x, LinkedHashMap::new));
//			resolvables = m.values().stream().toList();
//		}
//		typeResolver = diFactory.newInstance(diFactory.classFor(DollarTypeResolver.class));
//		converter = diFactory.newInstance(diFactory.classFor(Converter.class));
//
//		storables = resolvables.stream().filter(x -> x.isAnnotationPresent(Store.class)).toList();
//		{
//			var x = configuration.getProperty(configurationKey + ".upload.directory");
//			if (x.startsWith("~"))
//				x = System.getProperty("user.home") + x.substring(1);
//			var d = Path.of(x);
//			if (!Files.exists(d))
//				try {
//					Files.createDirectories(d);
//				} catch (IOException e) {
//					throw new UncheckedIOException(e);
//				}
//			cmsResourceHandling = diFactory.newInstance(CmsResourceHandling.class, Map.of("directory", d));
//		}
//		{
//			var f = configuration.getProperty(configurationKey + ".database.file");
//			if (f.startsWith("~"))
//				f = System.getProperty("user.home") + f.substring(1);
//			var b = diFactory.newInstance(diFactory.classFor(PersistenceBuilder.class),
//					Map.of("databaseFile", Path.of(f)));
//			persistence = b.build(diFactory);
//		}
//
//		includeType = true;
//		invocationResolver = diFactory.newInstance(diFactory.classFor(InvocationResolver.class), Map.of("invocables",
//				diFactory.types().stream().filter(x -> !(x.isInterface() || Modifier.isAbstract(x.getModifiers())))
//						.flatMap(x -> Arrays.stream(x.getMethods())
//								.filter(y -> !Modifier.isStatic(y.getModifiers()) && !y.isBridge())
//								.map(y -> new Invocable(x, y)))
//						.toList(),
//				"instanceResolver", (Function<Class<?>, Object>) x -> {
//					var y = diFactory.context();
	//// IO.println("x=" + x + ", y=" + y);
//					return x.isAssignableFrom(y.getClass()) ? diFactory.context()
//							: diFactory.newInstance(diFactory.classFor(x),
//									Map.of("invocationResolver", DefaultInvocationResolver.INSTANCE.get()));
//				}));
//		renderableFactory = diFactory.newInstance(diFactory.classFor(RenderableFactory.class));
//		handlerFactory = diFactory.newInstance(diFactory.classFor(ApplicationHandlerFactory.class));
//		handler = this::handle;
//	}

	public CmsResourceHandling cmsResourceHandling() {
		return cmsResourceHandling;
	}

	public BlankDomain domain() {
		return domain;
	}

	public Predicate<HttpExchange> drafts() {
		return drafts;
	}

	@Handle(method = "GET", path = "/api/schema")
	public Map<String, Object> schema() {
		class A {
			private static final Map<Class<?>, Map<String, Object>> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(dataType(),
				x -> diFactory.newInstance(diFactory.classFor(CmsSchema.class), Map.of("dataType", x)));
	}

	protected Class<?> dataType() {
		return Data.class;
	}

//	protected boolean handle(HttpExchange exchange) {
	//// IO.println("BlankBackend.handle, exchange=" + exchange);
//		return ScopedValue.where(com.janilla.blanktemplate.Configuration.PROPERTY_GETTER,
//				x -> configuration.getProperty(configurationKey + "." + x)).call(() -> {
//					var h = handlerFactory
//							.createHandler(exchange.exception() != null ? exchange.exception() : exchange.request());
//					if (h == null)
//						throw new NotFoundException(
//								exchange.request().getHeaderValue(":method") + " " + exchange.request().getHeaderValue(":path"));
//					return h.handle(exchange);
//				});
//	}

	@Override
	protected InvocationResolver newInvocationResolver() {
		domain = diFactory.newInstance(diFactory.classFor(BlankDomain.class));

		{
			var x = config.upload().directory();
			if (x.startsWith("~"))
				x = System.getProperty("user.home") + x.substring(1);
			var d = Path.of(x);
			if (!Files.exists(d))
				try {
					Files.createDirectories(d);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			cmsResourceHandling = diFactory.newInstance(CmsResourceHandling.class, Map.of("directory", d));
		}

		return super.newInvocationResolver();
	}

	protected boolean testDrafts(HttpExchange x) {
		var u = x instanceof HttpExchangeImpl y ? y.sessionUser() : null;
		return u != null;
	}
}

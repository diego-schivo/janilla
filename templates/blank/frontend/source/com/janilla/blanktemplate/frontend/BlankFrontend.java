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
package com.janilla.blanktemplate.frontend;

import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;

import com.janilla.blanktemplate.BlankDomain;
import com.janilla.frontend.IndexFactory;
import com.janilla.frontend.cms.CmsDataFetching;
import com.janilla.frontend.web.AbstractFrontend;
import com.janilla.http.HttpClient;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.web.InvocationResolver;

public class BlankFrontend extends AbstractFrontend {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.http"), Java.getPackageTypes("com.janilla.web"),
				Java.getPackageTypes("com.janilla.frontend", _ -> true),
				Java.getPackageTypes("com.janilla.blanktemplate"),
				Java.getPackageTypes("com.janilla.blanktemplate.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		serve(f, args.length > 0 ? args[0] : null);
	}

//	protected final HttpHandlerFactory handlerFactory;

	protected BlankDomain domain;

//	protected Converter converter;

	protected CmsDataFetching dataFetching;

	protected HttpClient httpClient;

//	protected final List<Class<?>> resolvables;

//	protected TypeResolver typeResolver;

	public BlankFrontend(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "blank-template");
	}

	protected BlankFrontend(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey);
//		this.diFactory = diFactory;
//		this.configurationFile = configurationFile;
//		this.configurationKey = configurationKey;
//		diFactory.context(this);
//		configuration = diFactory.newInstance(diFactory.classFor(Configuration.class),
//				Collections.singletonMap("path", configurationFile));
//		domain = diFactory.newInstance(diFactory.classFor(BlankDomain.class));

//		{
//			Map<String, Class<?>> m = diFactory.types().stream()
//					.collect(Collectors.toMap(x -> x.getSimpleName(), x -> x, (_, x) -> x, LinkedHashMap::new));
		//// IO.println("m=" + m);
//			resolvables = m.values().stream().toList();
//		}
//		typeResolver = diFactory.newInstance(diFactory.classFor(DollarTypeResolver.class));
//		converter = diFactory.newInstance(diFactory.classFor(Converter.class));

//		httpClient = diFactory.newInstance(diFactory.classFor(HttpClient.class),
//				Collections.singletonMap("sslContext", sslContext(configuration, configurationKey)));
//		{
//			var c = diFactory.classFor(CmsDataFetching.class);
//			dataFetching = c != null ? diFactory.newInstance(c) : null;
//		}

//		putResourcePrefixes();
//		resourceMap = diFactory.newInstance(diFactory.classFor(ResourceMap.class), Map.of("paths", resourcePaths()));
//		indexFactory = diFactory.newInstance(diFactory.classFor(IndexFactory.class));
//
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
//							: diFactory.newInstance(diFactory.classFor(x));
//				}));
//		renderableFactory = diFactory.newInstance(diFactory.classFor(RenderableFactory.class));
//		handlerFactory = diFactory.newInstance(diFactory.classFor(ApplicationHandlerFactory.class));
//		handler = this::handle;
	}

//	public Configuration configuration() {
//		return configuration;
//	}
//
//	public String configurationKey() {
//		return configurationKey;
//	}

	public BlankDomain domain() {
		return domain;
	}

//	public Converter converter() {
//		return converter;
//	}

	public CmsDataFetching dataFetching() {
		return dataFetching;
	}

//	public DiFactory diFactory() {
//		return diFactory;
//	}
//
//	public HttpHandler handler() {
//		return handler;
//	}

	public HttpClient httpClient() {
		return httpClient;
	}

	public IndexFactory indexFactory() {
		return indexFactory;
	}

//	public InvocationResolver invocationResolver() {
//		return invocationResolver;
//	}
//
//	public RenderableFactory renderableFactory() {
//		return renderableFactory;
//	}

//	public List<Class<?>> resolvables() {
//		return resolvables;
//	}

//	public ResourceMap resourceMap() {
//		return resourceMap;
//	}
//
//	public Map<String, String> resourcePrefixes() {
//		return resourcePrefixes;
//	}

//	public TypeResolver typeResolver() {
//		return typeResolver;
//	}

//	protected boolean handle(HttpExchange exchange) {
//		return ScopedValue.where(com.janilla.blanktemplate.Configuration.PROPERTY_GETTER,
//				x -> configuration.getProperty(configurationKey + "." + x)).call(() -> {
//					var h = handlerFactory
//							.createHandler(exchange.exception() != null ? exchange.exception() : exchange.request());
//					if (h == null)
//						throw new NotFoundException(exchange.request().getHeaderValue(":method") + " "
//								+ exchange.request().getHeaderValue(":path"));
//					return h.handle(exchange);
//				});
//	}

//	protected Map<String, List<Path>> resourcePaths() {
//		return resourcePrefixes().entrySet().stream().reduce(new HashMap<>(), (x, y) -> {
//			x.computeIfAbsent(y.getValue(), _ -> new ArrayList<>())
//					.addAll(Java.getPackagePaths(y.getKey()).filter(Files::isRegularFile).toList());
//			return x;
//		}, (_, x) -> x);
//	}
//
//	protected void putResourcePrefixes() {
//		resourcePrefixes.put("com.janilla.frontend", "/base");
//		resourcePrefixes.put("com.janilla.frontend.cms", "");
//		resourcePrefixes.put("com.janilla.blanktemplate.frontend", "");
//	}

	@Override
	protected InvocationResolver newInvocationResolver() {
		domain = diFactory.newInstance(diFactory.classFor(BlankDomain.class));
		httpClient = diFactory.newInstance(diFactory.classFor(HttpClient.class),
				Collections.singletonMap("sslContext", sslContext(configuration, configurationKey)));
		{
			var c = diFactory.classFor(CmsDataFetching.class);
			dataFetching = c != null ? diFactory.newInstance(c) : null;
		}
		return super.newInvocationResolver();
	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();
		resourcePrefixes.put("com.janilla.frontend.cms", "");
		resourcePrefixes.put("com.janilla.blanktemplate.frontend", "");
	}
}

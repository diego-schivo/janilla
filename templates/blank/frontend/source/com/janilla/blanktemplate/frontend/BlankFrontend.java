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
import com.janilla.web.WebApp;

public class BlankFrontend<C extends BlankFrontendConfig> extends AbstractFrontend<C> {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.http"), Java.getPackageTypes("com.janilla.java"),
				Java.getPackageTypes("com.janilla.web"), Java.getPackageTypes("com.janilla.frontend", _ -> true),
				Java.getPackageTypes("com.janilla.blanktemplate"),
				Java.getPackageTypes("com.janilla.blanktemplate.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		var c = newConfig(new Class<?>[] { BlankFrontend.class }, args.length != 0 ? args[0] : null, f);
		var a = f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f));
		serve(a);
	}

	protected BlankDomain domain;

	protected CmsDataFetching dataFetching;

	protected HttpClient httpClient;

	public BlankFrontend(C config, DiFactory diFactory) {
		super(config, diFactory);
	}

	public BlankDomain domain() {
		return domain;
	}

	public CmsDataFetching dataFetching() {
		return dataFetching;
	}

	public HttpClient httpClient() {
		return httpClient;
	}

	public IndexFactory indexFactory() {
		return indexFactory;
	}

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
				Collections.singletonMap("sslContext", sslContext(config)));
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

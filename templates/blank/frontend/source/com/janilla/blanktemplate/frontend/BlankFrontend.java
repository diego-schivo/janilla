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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.blanktemplate.BlankDomain;
import com.janilla.frontend.IndexFactory;
import com.janilla.frontend.cms.CmsDataFetching;
import com.janilla.frontend.web.AbstractFrontend;
import com.janilla.http.HttpClient;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.web.InvocationResolver;
import com.janilla.web.PackageResourcesProvider;
import com.janilla.web.WebApp;

public class BlankFrontend<C extends BlankFrontendConfig, D extends BlankDomain> extends AbstractFrontend<C, D> {

	protected static final String LUCIDE_ICONS_DOWNLOAD = "https://github.com/lucide-icons/lucide/releases/download/1.14.0/lucide-icons-1.14.0.zip";

	private static final Logger LOGGER = System.getLogger(BlankFrontend.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.http"), Java.getPackageTypes("com.janilla.java"),
				Java.getPackageTypes("com.janilla.web"), Java.getPackageTypes("com.janilla.frontend", _ -> true),
				Java.getPackageTypes("com.janilla.blanktemplate"),
				Java.getPackageTypes("com.janilla.blanktemplate.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0]);
		var c = newConfig(new Class<?>[] { BlankFrontend.class }, args.length != 0 ? args[0] : null, f);
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f, "context",
				(Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x)));
		serve(a[0]);
	}

	protected CmsDataFetching dataFetching;

	protected HttpClient httpClient;

	public BlankFrontend(C config, DiFactory diFactory, Consumer<Object> context, HttpClient httpClient) {
		this.httpClient = httpClient;
		super(config, diFactory, context);
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

	@Override
	protected InvocationResolver newInvocationResolver() {
		if (httpClient == null)
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
		resourcesProviders.put(
				diFactory.newInstance(DownloadResourcesProvider.class, Map.of("url", LUCIDE_ICONS_DOWNLOAD)), "/base");
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.frontend.cms"), "");
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.blanktemplate.frontend"), "");
	}
}

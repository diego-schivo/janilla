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
package com.janilla.websitetemplate.frontend;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.blanktemplate.frontend.BlankFrontend;
import com.janilla.blanktemplate.frontend.DownloadResourcesProvider;
import com.janilla.http.HttpClient;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.web.PackageResourcesProvider;
import com.janilla.web.WebApp;
import com.janilla.websitetemplate.WebsiteDomain;

public class WebsiteFrontend<C extends WebsiteFrontendConfig, D extends WebsiteDomain> extends BlankFrontend<C, D> {

	protected static final String GEIST_FONT_DOWNLOAD = "https://github.com/vercel/geist-font/releases/download/1.8.0/geist-font-1.8.0.zip";

	private static final Logger LOGGER = System.getLogger(WebsiteFrontend.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(BlankFrontend.diTypes(), Java.getPackageTypes("com.janilla.websitetemplate"),
				Java.getPackageTypes("com.janilla.websitetemplate.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0]);
		var c = newConfig(new Class<?>[] { WebsiteFrontend.class }, args.length != 0 ? args[0] : null, f);
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f, "context",
				(Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x)));
		serve(a[0]);
	}

	public WebsiteFrontend(C config, DiFactory diFactory, Consumer<Object> context, HttpClient httpClient) {
		super(config, diFactory, context, httpClient);
	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.blanktemplate.frontend"), "/blank");
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.websitetemplate.frontend"), "");
		resourcesProviders.put(diFactory.newInstance(DownloadResourcesProvider.class, Map.of("url", GEIST_FONT_DOWNLOAD)),
				"");
	}
}

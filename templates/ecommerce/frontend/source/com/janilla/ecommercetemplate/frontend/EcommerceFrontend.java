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
package com.janilla.ecommercetemplate.frontend;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.blanktemplate.frontend.DownloadResourcesProvider;
import com.janilla.ecommercetemplate.EcommerceDomain;
import com.janilla.http.HttpClient;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.web.PackageResourcesProvider;
import com.janilla.web.WebApp;
import com.janilla.websitetemplate.frontend.WebsiteFrontend;

public class EcommerceFrontend<C extends EcommerceFrontendConfig, D extends EcommerceDomain>
		extends WebsiteFrontend<C, D> {

	private static final Logger LOGGER = System.getLogger(EcommerceFrontend.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(WebsiteFrontend.diTypes(), Java.getPackageTypes("com.janilla.ecommercetemplate"),
				Java.getPackageTypes("com.janilla.ecommercetemplate.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0]);
		var c = newConfig(new Class<?>[] { EcommerceFrontend.class }, args.length != 0 ? args[0] : null, f);
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f, "context",
				(Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x)));
		serve(a[0]);
	}

	public EcommerceFrontend(C config, DiFactory diFactory, Consumer<Object> context, HttpClient httpClient) {
		super(config, diFactory, context, httpClient);
	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.websitetemplate.frontend"), "/website");
		resourcesProviders.put(diFactory.newInstance(DownloadResourcesProvider.class, Map.of("url", GEIST_FONT_DOWNLOAD)),
				"/website");
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.ecommercetemplate.frontend"), "");
	}
}

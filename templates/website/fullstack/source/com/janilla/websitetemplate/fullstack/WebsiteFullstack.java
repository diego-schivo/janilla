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
package com.janilla.websitetemplate.fullstack;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.blanktemplate.fullstack.BlankFullstack;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.web.WebApp;
import com.janilla.websitetemplate.WebsiteDomain;
import com.janilla.websitetemplate.backend.WebsiteBackend;
import com.janilla.websitetemplate.frontend.WebsiteFrontend;

public class WebsiteFullstack<C extends WebsiteFullstackConfig, D extends WebsiteDomain> extends BlankFullstack<C, D> {

	private static final Logger LOGGER = System.getLogger(WebsiteFullstack.class.getName());

	public static final Class<?>[] CONFIG_CLASSES = { WebsiteBackend.class, WebsiteFrontend.class,
			WebsiteFullstack.class };

	public static Stream<Class<?>> diTypes() {
		return Stream.of(BlankFullstack.diTypes(), Java.getPackageTypes("com.janilla.websitetemplate.backend"),
				Java.getPackageTypes("com.janilla.websitetemplate.frontend"),
				Java.getPackageTypes("com.janilla.websitetemplate.fullstack")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0], "fullstack");
		var c = newConfig(CONFIG_CLASSES, args.length != 0 ? args[0] : null, f);
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f, "context",
				(Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x)));
		serve(a[0]);
	}

	public WebsiteFullstack(C config, DiFactory diFactory, Consumer<Object> context) {
		this(config, diFactory, context, WebsiteBackend.class, WebsiteFrontend.class);
	}

	@SuppressWarnings("rawtypes")
	protected WebsiteFullstack(C config, DiFactory diFactory, Consumer<Object> context, Class backendClass,
			Class frontendClass) {
		super(config, diFactory, context, backendClass, frontendClass);
	}

	@Override
	protected Stream<Class<?>> diBackendTypes() {
		return Stream.of(diTypes(backendClass), Java.getPackageTypes("com.janilla.blanktemplate.fullstack"),
				Java.getPackageTypes("com.janilla.websitetemplate.fullstack")).flatMap(x -> x);
	}

	@Override
	protected Stream<Class<?>> diFrontendTypes() {
		return Stream.of(diTypes(frontendClass), Java.getPackageTypes("com.janilla.blanktemplate.fullstack"),
				Java.getPackageTypes("com.janilla.websitetemplate.fullstack")).flatMap(x -> x);
	}
}

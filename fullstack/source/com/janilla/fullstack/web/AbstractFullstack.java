/*
 * MIT License
 *
 * Copyright (c) React Training LLC 2015-2019
 * Copyright (c) Remix Software Inc. 2020-2021
 * Copyright (c) Shopify Inc. 2022-2023
 * Copyright (c) Diego Schivo 2024-2026
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
package com.janilla.fullstack.web;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.janilla.backend.web.AbstractBackend;
import com.janilla.frontend.web.AbstractFrontend;
import com.janilla.http.HttpHandler;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.java.JavaInvoke;
import com.janilla.web.AbstractApp;
import com.janilla.web.InvocationResolver;
import com.janilla.web.RenderableFactory;

public abstract class AbstractFullstack extends AbstractApp {

	public static final ScopedValue<AbstractFullstack> INSTANCE = ScopedValue.newInstance();

	protected static Stream<Class<?>> diTypes(Class<?> class1) {
		try {
			return (Stream<Class<?>>) JavaInvoke.methodHandle(class1.getDeclaredMethod("diTypes")).invoke();
		} catch (Throwable e) {
			throw e instanceof RuntimeException x ? x : new RuntimeException(e);
		}
	}

	protected final Class<? extends AbstractBackend> backendClass;

	protected final Class<? extends AbstractFrontend> frontendClass;

	protected AbstractBackend backend;

	protected AbstractFrontend frontend;

	protected AbstractFullstack(DiFactory diFactory, Path configurationFile, String configurationKey,
			Class<? extends AbstractFrontend> frontendClass, Class<? extends AbstractBackend> backendClass) {
		this.frontendClass = frontendClass;
		this.backendClass = backendClass;
		super(diFactory, configurationFile, configurationKey);
	}

	public AbstractBackend backend() {
		return backend;
	}

	public AbstractFrontend frontend() {
		return frontend;
	}

	protected Stream<Class<?>> diBackendTypes() {
		return Stream.concat(diTypes(backendClass), Java.getPackageTypes(getClass().getPackageName()));
	};

	protected Stream<Class<?>> diFrontendTypes() {
		return Stream.concat(diTypes(frontendClass), Java.getPackageTypes(getClass().getPackageName()));
	};

	@Override
	protected HttpHandler newHandler() {
		Path cf;
		try {
			cf = configurationFile != null ? configurationFile
					: Path.of(getClass().getResource("configuration.properties").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		backend = ScopedValue.where(INSTANCE, this).call(() -> {
			var f = new DefaultDiFactory(diBackendTypes().toList(), "backend");
			return f.newInstance(backendClass,
					Java.hashMap("diFactory", f, "configurationFile", cf, "configurationKey", configurationKey));
		});

		frontend = ScopedValue.where(INSTANCE, this).call(() -> {
			var f = new DefaultDiFactory(diFrontendTypes().toList(), "frontend");
			return f.newInstance(frontendClass,
					Java.hashMap("diFactory", f, "configurationFile", cf, "configurationKey", configurationKey));
		});

		return x -> {
			var h = x.request().getPath().startsWith("/api/") ? backend.handler() : frontend.handler();
			return h.handle(x);
		};
	}

	@Override
	protected InvocationResolver newInvocationResolver() {
		return null;
	}

	@Override
	protected RenderableFactory newRenderableFactory() {
		return null;
	}
}

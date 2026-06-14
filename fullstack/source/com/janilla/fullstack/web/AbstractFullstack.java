/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.fullstack.web;

import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.backend.web.AbstractBackend;
import com.janilla.frontend.web.AbstractFrontend;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.java.JavaInvoke;
import com.janilla.web.AbstractWebApp;
import com.janilla.web.Domain;
import com.janilla.web.InvocationResolver;
import com.janilla.web.RenderableFactory;
import com.janilla.web.WebApp;

public abstract class AbstractFullstack<C extends FullstackConfig, D extends Domain> extends AbstractWebApp<C, D>
		implements Fullstack<C, D> {

	protected static Stream<Class<?>> diTypes(Class<?> class1) {
		try {
			return (Stream<Class<?>>) JavaInvoke.methodHandle(class1.getDeclaredMethod("diTypes")).invoke();
		} catch (Throwable e) {
			throw e instanceof RuntimeException x ? x : new RuntimeException(e);
		}
	}

	protected final Class<? extends AbstractBackend<?, ?>> backendClass;

	protected final Class<? extends AbstractFrontend<?, ?>> frontendClass;

	protected AbstractBackend<?, ?> backend;

	protected AbstractFrontend<?, ?> frontend;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected AbstractFullstack(C config, DiFactory diFactory, Consumer<Object> context, Class backendClass,
			Class frontendClass) {
		this.backendClass = backendClass;
		this.frontendClass = frontendClass;
		super(config, diFactory, context);
	}

	public AbstractBackend<?, ?> backend() {
		return backend;
	}

	public AbstractFrontend<?, ?> frontend() {
		return frontend;
	}

	protected Stream<Class<?>> diBackendTypes() {
		return Stream.concat(diTypes(backendClass), Java.getPackageTypes(getClass().getPackageName()));
	};

	protected Stream<Class<?>> diFrontendTypes() {
		return Stream.concat(diTypes(frontendClass), Java.getPackageTypes(getClass().getPackageName()));
	};

	@Override
	protected HttpHandler newHttpHandler() {
		backend = ScopedValue.where(INSTANCE, this).call(() -> {
			var a = new WebApp[1];
			var f = Ioc.diFactory(diBackendTypes().toList(), () -> a[0], "backend");
			return f.newInstance(backendClass, Java.hashMap("config", config, "diFactory", f, "context",
					(Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x)));
		});

		frontend = ScopedValue.where(INSTANCE, this).call(() -> {
			var a = new WebApp[1];
			var f = Ioc.diFactory(diFrontendTypes().toList(), () -> a[0], "frontend");
			return f.newInstance(frontendClass, Java.hashMap("config", config, "diFactory", f, "context",
					(Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x)));
		});

		return x -> {
			var a = webApp(x);
			return ScopedValue.where(INSTANCE, a).call(() -> a.httpHandler().handle(x));
		};
	}

	protected WebApp<?, ?> webApp(HttpExchange exchange) {
		var p1 = exchange.request().getPath();
		var p2 = backend.config().basePath() + "/api/";
		var a = p1.startsWith(p2) ? backend : frontend;
//		IO.println("AbstractFullstack.newHttpHandler, p1=" + p1 + ", p2=" + p2 + ", a=" + a);
		return a;
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

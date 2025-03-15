/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
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
package com.janilla.web;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.reflect.Factory;
import com.janilla.reflect.Reflection;
import com.janilla.util.Util;

public class ApplicationHandlerBuilder {

	protected Factory factory;

	protected List<WebHandlerFactory> factories;

	protected WebHandlerFactory handlerFactory;

	public void setFactory(Factory factory) {
		this.factory = factory;
	}

	public HttpHandler build() {
		factories = buildFactories().toList();
		var f = new DelegatingHandlerFactory();
		for (var g : factories) {
			var s = Reflection.property(g.getClass(), "mainFactory");
			if (s != null)
				s.set(g, f);
		}
		f.setToHandler((o, c) -> {
			var h = createHandler(o, c);
//			System.out.println("h=" + h);
			return h;
		});
//		handlerFactory = f;
		return x -> {
			var ex = (HttpExchange) x;
			var o = ex.getException() != null ? ex.getException() : ex.getRequest();
			var h = f.createHandler(o, ex);
			if (h == null)
				throw new NotFoundException(ex.getRequest().getMethod() + " " + ex.getRequest().getTarget());
			return h.handle(ex);
		};
	}

	protected Stream<WebHandlerFactory> buildFactories() {
		return Stream.of(buildMethodHandlerFactory(), buildTemplateHandlerFactory(), buildResourceHandlerFactory(),
				buildJsonHandlerFactory(), buildExceptionHandlerFactory());
	}

	protected WebHandlerFactory buildMethodHandlerFactory() {
		var f = factory.create(MethodHandlerFactory.class);
		var fcc = Util.getPackageClasses("com.janilla.frontend").toList();
		f.initialize(() -> Stream.concat(StreamSupport.stream(factory.types().spliterator(), false), fcc.stream())
				.iterator(), x -> {
					var s = factory.source();
					return x == s.getClass() ? s : factory.create(x);
				});
		if (f.renderableFactory == null)
			f.renderableFactory = new RenderableFactory();
		return f;
	}

	protected WebHandlerFactory buildTemplateHandlerFactory() {
		return factory.create(TemplateHandlerFactory.class);
	}

	protected WebHandlerFactory buildResourceHandlerFactory() {
		var f = factory.create(ResourceHandlerFactory.class);
		f.initialize(getClass().getPackageName(), "com.janilla.frontend", factory.source().getClass().getPackageName());
		return f;
	}

	protected WebHandlerFactory buildJsonHandlerFactory() {
		return factory.create(JsonHandlerFactory.class);
	}

	protected WebHandlerFactory buildExceptionHandlerFactory() {
		return factory.create(ExceptionHandlerFactory.class);
	}

	protected HttpHandler createHandler(Object object, HttpExchange exchange) {
		for (var f : factories)
			if (f != null) {
				var h = f.createHandler(object, exchange);
				if (h != null) {
//					System.out.println("ApplicationHandlerBuilder.createHandler, f=" + f + ", h=" + h);
					return h;
				}
			}
		return null;
	}
}

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

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.reflect.Factory;

public class ApplicationHandlerBuilder implements WebHandlerFactory {

	protected final Factory factory;

	protected final Set<Method> methods;

	protected final Set<Path> resourceFiles;

	protected List<WebHandlerFactory> handlerFactories;

	public ApplicationHandlerBuilder(Factory factory, Set<Method> methods, Set<Path> resourceFiles) {
		this.factory = factory;
		this.methods = methods;
		this.resourceFiles = resourceFiles;
	}

	public HttpHandler build() {
		handlerFactories = buildFactories();
		return x -> {
			var o = x.getException() != null ? x.getException() : x.getRequest();
			var h = createHandler(o, x);
			if (h == null)
				throw new NotFoundException(x.getRequest().getMethod() + " " + x.getRequest().getTarget());
			return h.handle(x);
		};
	}

	protected List<WebHandlerFactory> buildFactories() {
		return List.of(buildMethodHandlerFactory(), buildTemplateHandlerFactory(), buildResourceHandlerFactory(),
				buildJsonHandlerFactory(), buildExceptionHandlerFactory());
	}

	protected WebHandlerFactory buildMethodHandlerFactory() {
		return factory.create(MethodHandlerFactory.class,
				Map.of("methods", methods, "targetResolver",
						(Function<Class<?>, Object>) x -> x.isAssignableFrom(factory.source().getClass())
								? factory.source()
								: factory.create(x),
						"rootFactory", this));
	}

	protected WebHandlerFactory buildTemplateHandlerFactory() {
		return factory.create(TemplateHandlerFactory.class, Map.of("rootFactory", this));
	}

	protected WebHandlerFactory buildResourceHandlerFactory() {
		return factory.create(ResourceHandlerFactory.class, Map.of("files", resourceFiles, "rootFactory", this));
	}

	protected WebHandlerFactory buildJsonHandlerFactory() {
		return factory.create(JsonHandlerFactory.class, Map.of("rootFactory", this));
	}

	protected WebHandlerFactory buildExceptionHandlerFactory() {
		return factory.create(ExceptionHandlerFactory.class, Map.of("rootFactory", this));
	}

	@Override
	public HttpHandler createHandler(Object object, HttpExchange exchange) {
		for (var f : handlerFactories)
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

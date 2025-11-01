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

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.janilla.http.HttpHandler;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.reflect.ClassAndMethod;
import com.janilla.reflect.Factory;

public class ApplicationHandlerFactory implements HttpHandlerFactory {

	protected final Factory factory;

	protected final Collection<ClassAndMethod> methods;

	protected final Collection<Path> files;

	protected final List<HttpHandlerFactory> handlerFactories;

	public ApplicationHandlerFactory(Factory factory, Collection<ClassAndMethod> methods, Collection<Path> files) {
		this.factory = factory;
		this.methods = methods;
		this.files = files;
		handlerFactories = buildFactories();
	}

	@Override
	public HttpHandler createHandler(Object object) {
		for (var f : handlerFactories)
			if (f != null) {
				var h = f.createHandler(object);
				if (h != null) {
//					IO.println("ApplicationHandlerBuilder.createHandler, f=" + f + ", h=" + h);
					return h;
				}
			}
		return null;
	}

	protected List<HttpHandlerFactory> buildFactories() {
		return List.of(buildMethodHandlerFactory(), buildTemplateHandlerFactory(), buildFileHandlerFactory(),
				buildJsonHandlerFactory(), buildExceptionHandlerFactory());
	}

	protected HttpHandlerFactory buildMethodHandlerFactory() {
		return factory.create(MethodHandlerFactory.class,
				Map.of("methods", methods, "targetResolver", (Function<Class<?>, Object>) x -> {
					var y = factory.source();
//					IO.println("ApplicationHandlerFactory.buildMethodHandlerFactory, x=" + x + ", y=" + y);
					return x.isAssignableFrom(y.getClass()) ? factory.source() : factory.create(x);
				}, "rootFactory", this));
	}

	protected HttpHandlerFactory buildTemplateHandlerFactory() {
		return factory.create(TemplateHandlerFactory.class, Map.of("rootFactory", this));
	}

	protected HttpHandlerFactory buildFileHandlerFactory() {
		return factory.create(FileHandlerFactory.class, Map.of("files", files, "rootFactory", this));
	}

	protected HttpHandlerFactory buildJsonHandlerFactory() {
		return factory.create(JsonHandlerFactory.class, Map.of("rootFactory", this));
	}

	protected HttpHandlerFactory buildExceptionHandlerFactory() {
		return factory.create(ExceptionHandlerFactory.class, Map.of("rootFactory", this));
	}
}

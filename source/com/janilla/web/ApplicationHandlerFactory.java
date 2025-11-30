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
import java.util.Map;
import java.util.function.Function;

import com.janilla.http.HttpHandler;
import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;

public class ApplicationHandlerFactory implements HttpHandlerFactory {

	protected final DiFactory diFactory;

	protected final List<HttpHandlerFactory> handlerFactories;

	public ApplicationHandlerFactory(DiFactory diFactory) {
		this.diFactory = diFactory;
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

	protected HttpHandlerFactory buildExceptionHandlerFactory() {
		return diFactory.create(ExceptionHandlerFactory.class, Map.of("rootFactory", this));
	}

	protected List<HttpHandlerFactory> buildFactories() {
		return List.of(buildInvocationHandlerFactory(), buildTemplateHandlerFactory(), buildJsonHandlerFactory(),
				buildFileHandlerFactory(), buildExceptionHandlerFactory());
	}

	protected FileHandlerFactory buildFileHandlerFactory() {
		return diFactory.create(FileHandlerFactory.class, Map.of("rootFactory", this));
	}

	protected HttpHandlerFactory buildJsonHandlerFactory() {
		return diFactory.create(JsonHandlerFactory.class, Map.of("rootFactory", this));
	}

	protected HttpHandlerFactory buildInvocationHandlerFactory() {
		return diFactory.create(InvocationHandlerFactory.class,
				Java.hashMap("instanceResolver", (Function<Class<?>, Object>) x -> {
					var y = diFactory.context();
//					IO.println("ApplicationHandlerFactory.buildMethodHandlerFactory, x=" + x + ", y=" + y);
					return x.isAssignableFrom(y.getClass()) ? diFactory.context() : diFactory.create(x);
				}, "rootFactory", this));
	}

	protected HttpHandlerFactory buildTemplateHandlerFactory() {
		return diFactory.create(TemplateHandlerFactory.class, Map.of("rootFactory", this));
	}
}

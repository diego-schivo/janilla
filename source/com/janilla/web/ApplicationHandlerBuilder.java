/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.janilla.http.HttpExchange;
import com.janilla.io.IO;
import com.janilla.io.IO.Consumer;
import com.janilla.reflect.Factory;
import com.janilla.reflect.Reflection;
import com.janilla.util.Lazy;
import com.janilla.util.Util;

public class ApplicationHandlerBuilder {

	protected Object application;

	Supplier<Collection<Class<?>>> applicationClasses = Lazy.of(() -> {
		var c = Util.getPackageClasses(application.getClass().getPackageName()).toList();
//		System.out.println("c=" + c);
		return c;
	});

	Supplier<Factory> factory = Lazy.of(() -> {
		var f = new Factory();
		f.setTypes(applicationClasses.get());
		f.setEnclosing(application);
		return f;
	});

	Supplier<Collection<Class<?>>> frontendClasses = Lazy.of(() -> {
		var c = Util.getPackageClasses("com.janilla.frontend").toList();
//		System.out.println("c=" + c);
		return c;
	});

	HandlerFactory[] factories;

	Supplier<HandlerFactory> handlerFactory = Lazy.of(() -> {
		factories = new HandlerFactory[] { buildMethodHandlerFactory(), buildTemplateHandlerFactory(),
				buildResourceHandlerFactory(), buildJsonHandlerFactory(), buildExceptionHandlerFactory() };
		var f = new DelegatingHandlerFactory();
		for (var g : factories) {
			var s = Reflection.property(g.getClass(), "mainFactory");
			if (s != null)
				s.set(g, f);
		}
		f.setToHandler((o, c) -> {
			return createHandler(o, c);
		});
		return f;
	});

	public void setApplication(Object application) {
		this.application = application;
	}

	public HandlerFactory getHandlerFactory() {
		return handlerFactory.get();
	}

	public IO.Consumer<HttpExchange> build() {
		return c -> {
			var o = c.getException() != null ? c.getException() : c.getRequest();
			var h = handlerFactory.get().createHandler(o, c);
			if (h == null)
				throw new NotFoundException();
			h.accept(c);
		};
	}

	protected MethodHandlerFactory buildMethodHandlerFactory() {
		var f = newInstance(MethodHandlerFactory.class);

		var i = new AnnotationDrivenToMethodInvocation() {

			@Override
			protected Object getInstance(Class<?> c) {
				if (c == application.getClass())
					return application;
				var i = super.getInstance(c);
//				try {
//					initialize(i);
//				} catch (ReflectiveOperationException e) {
//					throw new RuntimeException(e);
//				}
				return Reflection.copy(application, i);
			}
		};
		i.setTypes(() -> Stream.concat(applicationClasses.get().stream(), frontendClasses.get().stream()).iterator());
		f.setToInvocation(i);

		return f;
	}

	protected TemplateHandlerFactory buildTemplateHandlerFactory() {
		return newInstance(TemplateHandlerFactory.class);
	}

	protected ResourceHandlerFactory buildResourceHandlerFactory() {
		var f = newInstance(ResourceHandlerFactory.class);
		var s = new ToResourceStream.Simple();
		s.setPaths(() -> Stream.concat(IO.getPackageFiles("com.janilla.frontend"),
				IO.getPackageFiles(application.getClass().getPackageName())).iterator());
		f.setToInputStream(s);
		return f;
	}

	protected JsonHandlerFactory buildJsonHandlerFactory() {
		return newInstance(JsonHandlerFactory.class);
	}

	protected ExceptionHandlerFactory buildExceptionHandlerFactory() {
		return newInstance(ExceptionHandlerFactory.class);
	}

//	protected <T extends HandlerFactory> T newHandlerFactory(Class<T> factoryClass) {
//		Class<?> c = factoryClass;
//		for (var d : applicationClasses.get()) {
//			if (!Modifier.isAbstract(d.getModifiers()) && factoryClass.isAssignableFrom(d)) {
//				c = d;
//				break;
//			}
//		}
//		try {
//			@SuppressWarnings("unchecked")
//			var t = (T) (c.getEnclosingClass() == application.getClass()
//					? c.getConstructors()[0].newInstance(application)
//					: c.getConstructor().newInstance());
//			initialize(t);
//			return t;
//		} catch (ReflectiveOperationException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	protected void initialize(Object object) throws ReflectiveOperationException {
//		for (var j = Reflection.properties(object.getClass()).iterator(); j.hasNext();) {
//			var n = j.next();
//			var s = Reflection.property(object.getClass(), n);
//			if (n.equals("application") && s != null) {
//				s.set(object, application);
//				continue;
//			}
//			var g = s != null ? Reflection.property(application.getClass(), n) : null;
//			var v = g != null ? g.get(application) : null;
//			if (v != null)
//				s.set(object, v);
//		}
//	}

	protected <T> T newInstance(Class<T> type) {
		var t = factory.get().newInstance(type);
		var p = Reflection.property(type, "application");
		if (p != null)
			p.set(t, application);
		return t;
	}

	protected Consumer<HttpExchange> createHandler(Object object, HttpExchange exchange) {
		for (var g : factories)
			if (g != null) {
				var h = g.createHandler(object, exchange);
				if (h != null)
					return h;
			}
		return null;
	}
}

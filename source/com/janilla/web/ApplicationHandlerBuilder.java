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
import com.janilla.reflect.Reflection;
import com.janilla.util.Lazy;
import com.janilla.util.Util;

public class ApplicationHandlerBuilder {

	protected Object application;

	Supplier<Collection<Class<?>>> applicationClasses = Lazy
			.of(() -> Util.getPackageClasses(application.getClass().getPackageName()).toList());

	Supplier<Collection<Class<?>>> frontendClasses = Lazy
			.of(() -> Util.getPackageClasses("com.janilla.frontend").toList());

	Supplier<HandlerFactory> handlerFactory = Lazy.of(() -> {
		var a = new HandlerFactory[] { buildMethodHandlerFactory(), buildTemplateHandlerFactory(),
				buildResourceHandlerFactory(), buildJsonHandlerFactory(), buildExceptionHandlerFactory() };
		var f = new DelegatingHandlerFactory();
		for (var g : a) {
			var s = Reflection.setter(g.getClass(), "mainFactory");
			if (s != null)
				try {
					s.invoke(g, f);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
		}
		f.setToHandler((o, c) -> {
			for (var g : a)
				if (g != null) {
					var h = g.createHandler(o, c);
					if (h != null)
						return h;
				}
			return null;
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
		var f = newHandlerFactory(MethodHandlerFactory.class);

		var i = new AnnotationDrivenToMethodInvocation() {

			@Override
			protected Object getInstance(Class<?> c) {
				if (c == application.getClass())
					return application;
				var i = super.getInstance(c);
				try {
					initialize(i);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
				return i;
			}
		};
		i.setTypes(() -> Stream.concat(applicationClasses.get().stream(), frontendClasses.get().stream()).iterator());
		f.setToInvocation(i);

		return f;
	}

	protected TemplateHandlerFactory buildTemplateHandlerFactory() {
		return newHandlerFactory(TemplateHandlerFactory.class);
	}

	protected ResourceHandlerFactory buildResourceHandlerFactory() {
		var f = newHandlerFactory(ResourceHandlerFactory.class);
		var s = new ToResourceStream.Simple();
		s.setPaths(() -> Stream.concat(IO.getPackageFiles("com.janilla.frontend"),
				IO.getPackageFiles(application.getClass().getPackageName())).iterator());
		f.setToInputStream(s);
		return f;
	}

	protected JsonHandlerFactory buildJsonHandlerFactory() {
		return newHandlerFactory(JsonHandlerFactory.class);
	}

	protected ExceptionHandlerFactory buildExceptionHandlerFactory() {
		return newHandlerFactory(ExceptionHandlerFactory.class);
	}

	protected <T extends HandlerFactory> T newHandlerFactory(Class<T> factoryClass) {
		Class<?> c = factoryClass;
		for (var d : applicationClasses.get()) {
			if (d.getSuperclass() == factoryClass) {
				c = d;
				break;
			}
		}
		try {
			@SuppressWarnings("unchecked")
			var i = (T) c.getConstructor().newInstance();
			initialize(i);
			return i;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	protected void initialize(Object object) throws ReflectiveOperationException {
		for (var j = Reflection.properties(object.getClass()).iterator(); j.hasNext();) {
			var n = j.next();
			var s = Reflection.setter(object.getClass(), n);
			if (n.equals("application") && s != null) {
				s.invoke(object, application);
				continue;
			}
			var g = s != null ? Reflection.getter(application.getClass(), n) : null;
			var v = g != null ? g.invoke(application) : null;
			if (v != null)
				s.invoke(object, v);
		}
	}
}

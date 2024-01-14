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

import java.util.function.Supplier;
import java.util.stream.Stream;

import com.janilla.http.ExchangeContext;
import com.janilla.io.IO;
import com.janilla.util.Lazy;
import com.janilla.util.Util;

public class ApplicationHandlerBuilder {

	protected Object application;

	public void setApplication(Object application) {
		this.application = application;
	}

	Supplier<HandlerFactory> handlerFactory = Lazy.of(() -> {
		var a = new HandlerFactory[] { buildMethodHandlerFactory(), buildTemplateHandlerFactory(),
				buildResourceHandlerFactory(), buildJsonHandlerFactory(), buildExceptionHandlerFactory() };
		var f = new DelegatingHandlerFactory();
		for (var g : a)
			if (g != null)
				completeBuild(g, f);
		f.setToHandler(o -> {
			for (var g : a)
				if (g != null) {
					var h = g.createHandler(o);
					if (h != null)
						return h;
				}
			return null;
		});
		return f;
	});

	public IO.Consumer<ExchangeContext> build() {
		return c -> {
			var o = c.getException() != null ? c.getException() : c.getRequest();
			var h = handlerFactory.get().createHandler(o);
			if (h == null)
				throw new NotFoundException();
			h.accept(c);
		};
	}

	protected MethodHandlerFactory buildMethodHandlerFactory() {
		var f = new MethodHandlerFactory();

		var i = new AnnotationDrivenToMethodInvocation() {

			@Override
			protected Object getInstance(Class<?> c) {
				if (c == application.getClass())
					return application;
				return super.getInstance(c);
			}
		};
		i.setTypes(() -> Util.getPackageClasses(application.getClass().getPackageName()).iterator());
		f.setToInvocation(i);

		return f;
	}

	protected TemplateHandlerFactory buildTemplateHandlerFactory() {
		var f = new TemplateHandlerFactory();

		var r = new AnnotationDrivenToTemplateReader.Simple();
		r.setResourceClass(application.getClass());
		r.setTypes(() -> Util.getPackageClasses(application.getClass().getPackageName()).iterator());
		f.setToReader(r);

		return f;
	}

	protected ResourceHandlerFactory buildResourceHandlerFactory() {
		var f = new ResourceHandlerFactory();
		var s = new ToResourceStream.Simple();
		s.setPaths(() -> Stream.concat(IO.getPackageFiles("com.janilla.frontend"),
				IO.getPackageFiles(application.getClass().getPackageName())).iterator());
		f.setToInputStream(s);
		return f;
	}

	protected JsonHandlerFactory buildJsonHandlerFactory() {
		return new JsonHandlerFactory();
	}

	protected ExceptionHandlerFactory buildExceptionHandlerFactory() {
		return new ExceptionHandlerFactory();
	}

	protected void completeBuild(HandlerFactory factory, HandlerFactory mainFactory) {
		switch (factory) {
		case MethodHandlerFactory m:
			m.setRenderFactory(mainFactory);
			break;
		case TemplateHandlerFactory t:
			t.setIncludeFactory(mainFactory);
			break;
		default:
			break;
		}
	}
}

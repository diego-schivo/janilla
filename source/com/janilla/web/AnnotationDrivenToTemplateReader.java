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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.janilla.util.Lazy;

public abstract class AnnotationDrivenToTemplateReader implements Function<Object, Template> {

	public static void main(String[] args) throws IOException {
		@Render(template = "foo.html")
		class C {
		}
		var f = new AnnotationDrivenToTemplateReader() {

			@Override
			protected Template newReader(String name) {
				var r = name.equals("foo.html") ? new InputStreamReader(new ByteArrayInputStream("""
						<html>
							<head>
								<title>My test page</title>
							</head>
							<body>
								<p>My cat is very grumpy</p>
							</body>
						</html>""".getBytes())) : null;
				if (r == null)
					return null;
				var t = new Template();
				t.setReader(r);
				return t;
			}
		};
		f.setTypes(Collections.singleton(C.class));

		var r = f.apply(new C());

		String s;
		try (var t = new BufferedReader(r.getReader())) {
			s = t.lines().collect(Collectors.joining("\n"));
		}
		System.out.println(s);
		assert s.equals("""
				<html>
					<head>
						<title>My test page</title>
					</head>
					<body>
						<p>My cat is very grumpy</p>
					</body>
				</html>""") : s;
	}

	private Iterable<Class<?>> types;

	Supplier<Map<Class<?>, String>> templates = Lazy.of(() -> {
		var m = new HashMap<Class<?>, String>();
		for (var t : types) {
			var r = t.getAnnotation(Render.class);
			if (r != null)
				m.put(t, r.template());
		}

//		System.out.println(Thread.currentThread().getName() + " ToTemplateReader templates " + templates);

		return m;
	});

	public void setTypes(Iterable<Class<?>> types) {
		this.types = types;
	}

	@Override
	public Template apply(Object t) {
		var n = templates.get().get(t.getClass());
		return n != null ? newReader(n) : null;
	}

	protected abstract Template newReader(String name);

	public static class Simple extends AnnotationDrivenToTemplateReader {

		Class<?> resourceClass;

		public Class<?> getResourceClass() {
			return resourceClass;
		}

		public void setResourceClass(Class<?> resourceClass) {
			this.resourceClass = resourceClass;
		}

		@Override
		protected Template newReader(String name) {
			if (name.contains("\n")) {
				var t = new Template();
				t.setReader(new InputStreamReader(new ByteArrayInputStream(name.getBytes())));
				return t;
			}
			var s = resourceClass.getResourceAsStream(name);
			if (s == null)
				throw new NullPointerException(name);
			var t = new Template();
			t.setName(name);
			t.setReader(new InputStreamReader(s));
			return t;
		}
	}
}

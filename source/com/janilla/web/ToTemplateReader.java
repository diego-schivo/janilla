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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.janilla.util.Lazy;

public abstract class ToTemplateReader implements Function<Object, Template> {

	public static void main(String[] args) throws IOException {
		@Render("foo.html")
		class C {
		}
		var f = new ToTemplateReader() {

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
		f.setClasses(C.class);

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

	Class<?>[] classes;

	Supplier<Map<Class<?>, String>> templates = Lazy.of(() -> {
		var m = new HashMap<Class<?>, String>();
		for (var c : classes) {
			var t = c.getAnnotation(Render.class);
			if (t != null)
				m.put(c, t.value());
		}

//		System.out.println(Thread.currentThread().getName() + " ToTemplateReader templates " + templates);

		return m;
	});

	public Class<?>[] getClasses() {
		return classes;
	}

	public void setClasses(Class<?>... classes) {
		this.classes = classes;
	}

	@Override
	public Template apply(Object t) {
		var n = templates.get().get(t.getClass());
		return n != null ? newReader(n) : null;
	}

	protected abstract Template newReader(String name);

	public static class Simple extends ToTemplateReader {

		Class<?> class1;

		public Class<?> getClass1() {
			return class1;
		}

		public void setClass1(Class<?> class1) {
			this.class1 = class1;
		}

		@Override
		protected Template newReader(String name) {
			var s = class1.getResourceAsStream(name);
			if (s == null)
				throw new NullPointerException(name);
			var t = new Template();
			t.setName(name);
			t.setReader(new InputStreamReader(s));
			return t;
		}
	}
}

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
package com.janilla.frontend;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.janilla.io.IO;
//import com.janilla.util.Evaluator;
//import com.janilla.util.Interpolator;
import com.janilla.util.Lazy;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class TemplatesWeb {

	Object application;

	static Pattern expressions = Pattern.compile("(data-)?__([\\w.]*?)__|<!--__([\\w.]*?)__-->");

	Supplier<Template[]> templates = Lazy.of(() -> {
		var z = application.getClass();
		var b = Stream.<Template>builder();
		IO.acceptPackageFiles(z.getPackageName(), f -> {
			var n = f.getFileName().toString();
			if (!n.endsWith(".html"))
				return;
			n = n.substring(0, n.length() - ".html".length());
			if (n.equals("app"))
				return;
			var o = new ByteArrayOutputStream();
			try (var t = z.getResourceAsStream(n + ".html"); var w = new PrintWriter(o)) {
				if (t == null)
					throw new NullPointerException(n);

				try (var r = new BufferedReader(new InputStreamReader(t))) {
					for (var j = r.lines().iterator(); j.hasNext();)
						w.print(expressions.matcher(j.next()).replaceAll(x -> {
							var g1 = x.group(1);
							var g2 = x.group(2);
							var g3 = x.group(3);
							return "\\${await r('" + Objects.toString(g2, g3) + "', " + (g2 != null && g1 == null)
									+ ")}";
						}));
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			var h = o.toString();
			var u = new Template(n, h);
			b.add(u);
		});
		return b.build().toArray(Template[]::new);
	});

	public void setApplication(Object application) {
		this.application = application;
	}

	@Handle(method = "GET", path = "/templates.js")
	public Script getScript() {
		return new Script(Arrays.stream(templates.get()));
	}

	@Render(template = """
			export default {
				<!--__entries__-->
			};""")
	public record Script(Stream<Template> entries) {
	}

	@Render(template = """
				'__name__': async r => `
			<!--__html__-->
				`,""")
	public record Template(String name, String html) {
	}
}

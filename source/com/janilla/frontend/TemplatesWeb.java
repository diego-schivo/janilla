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
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.janilla.io.IO;
import com.janilla.web.Handle;
import com.janilla.web.Render;

public class TemplatesWeb {

	Object application;

	public static Pattern expression = Pattern.compile("(data-)?\\$\\{([\\w\\[\\].-]*?)}|<!--\\$\\{([\\w\\[\\].-]*?)}-->");

	public static Pattern template = Pattern.compile("<template id=\"([\\w-]+)\">(.*?)</template>", Pattern.DOTALL);

	IO.Supplier<Iterable<Template>> templates = IO.Lazy.of(() -> {
		var l = Thread.currentThread().getContextClassLoader();
		var b = Stream.<Template>builder();
		for (var nn = Stream.of(getClass().getPackageName(), application.getClass().getPackageName())
				.flatMap(p -> IO.getPackageFiles(p).filter(f -> f.getFileName().toString().endsWith(".html")).map(f -> {
					return p.replace('.', '/') + "/" + f.getFileName().toString();
				})).iterator();nn.hasNext();) {
			var n = nn.next();
			var o = new ByteArrayOutputStream();
			try (var t = l.getResourceAsStream(n); var w = new PrintWriter(o)) {
				if (t == null)
					throw new NullPointerException(n);
				try (var r = new BufferedReader(new InputStreamReader(t))) {
					for (var j = r.lines().iterator(); j.hasNext();)
						w.print(expression.matcher(j.next()).replaceAll(x -> {
							var g1 = x.group(1);
							var g2 = x.group(2);
							var g3 = x.group(3);
							return "\\${await r('" + Objects.toString(g2, g3) + "', " + (g2 != null && g1 == null)
									+ ")}";
						}));
				}
			}
			var h = o.toString();
			h = template.matcher(h).replaceAll(r -> {
				b.add(new Template(r.group(1), r.group(2)));
				return "";
			});
			b.add(new Template(n.substring(n.lastIndexOf('/') + 1, n.length() - ".html".length()), h));
		}
		return b.build().toList();
	});

	public void setApplication(Object application) {
		this.application = application;
	}

	@Handle(method = "GET", path = "/templates.js")
	public Script getScript() {
		try {
			return new Script(templates.get());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Render(template = """
			export default {
				${entries}
			};""")
	public record Script(Iterable<Template> entries) {
	}

	@Render(template = """
				'${name}': async r => `
			${html}
				`,""")
	public record Template(String name, String html) {
	}
}

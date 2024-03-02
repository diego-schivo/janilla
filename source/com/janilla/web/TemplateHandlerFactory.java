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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;

import com.janilla.frontend.Interpolator;
import com.janilla.frontend.RenderEngine;
import com.janilla.frontend.RenderEngine.ObjectAndType;
import com.janilla.frontend.TemplatesWeb;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpResponse.Status;
import com.janilla.io.IO;
import com.janilla.io.IO.Consumer;

public class TemplateHandlerFactory implements HandlerFactory {

	protected Object application;

	public void setApplication(Object application) {
		this.application = application;
	}

	@Override
	public Consumer<HttpExchange> createHandler(Object object, HttpExchange context) {
		var i = object instanceof ObjectAndType x ? x : null;
		var o = i != null ? i.object() : null;
		var t = i != null ? i.type() : null;
		return (o != null && o.getClass().isAnnotationPresent(Render.class))
				|| (t != null && t.isAnnotationPresent(Render.class)) ? x -> render(i, x) : null;
	}

	protected void render(ObjectAndType input, HttpExchange context) throws IOException {
		{
			var s = context.getResponse();
			if (s.getStatus() == null)
				s.setStatus(new Status(200, "OK"));
			var h = s.getHeaders();
			if (h.get("Cache-Control") == null)
				s.getHeaders().set("Cache-Control", "no-cache");
			if (h.get("Content-Type") == null)
				s.getHeaders().set("Content-Type", "text/html");
		}

		RenderEngine e = new RenderEngine();
		e.setToInterpolator(s -> {
			String t;
			if (s.contains("\n"))
				t = s;
			else {
				t = foo.get().get(s);
				if (t == null)
					throw new NullPointerException(s);
			}
			var l = switch (context.getResponse().getHeaders().get("Content-Type")) {
			case "text/html" -> Interpolator.Language.HTML;
			case "text/javascript" -> Interpolator.Language.JAVASCRIPT;
			default -> throw new RuntimeException();
			};
			return Interpolator.of(t, l);
		});
		var b = e.render(input).toString().getBytes();
		var c = (WritableByteChannel) context.getResponse().getBody();
		IO.write(b, c);
	}

	IO.Supplier<Map<String, String>> foo = IO.Lazy.of(() -> {
		var c = application.getClass();
		var m = new HashMap<String, String>();
		IO.acceptPackageFiles(c.getPackageName(), f -> {
			var n = f.getFileName().toString();
			try (var i = c.getResourceAsStream(n)) {
				if (i == null)
					throw new NullPointerException(n);
				var h = new String(i.readAllBytes());
				h = TemplatesWeb.template.matcher(h).replaceAll(r -> {
					m.put(r.group(1) + ".html", r.group(2));
					return "";
				});
				m.put(n, h);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		return m;
	});
}

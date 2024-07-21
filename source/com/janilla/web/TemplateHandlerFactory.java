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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.janilla.frontend.Interpolator;
import com.janilla.frontend.RenderEngine;
import com.janilla.frontend.TemplatesWeb;
import com.janilla.http.HeaderField;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.io.IO;
import com.janilla.util.Lazy;

public class TemplateHandlerFactory implements WebHandlerFactory {

	protected Object application;

	Supplier<Map<String, String>> templateMap = Lazy.of(() -> {
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

	public void setApplication(Object application) {
		this.application = application;
	}

	@Override
	public HttpHandler createHandler(Object object, HttpExchange exchange) {
		var i = object instanceof RenderEngine.Entry x ? x : null;
		var o = i != null ? i.getValue() : null;
		var t = i != null ? i.getType() : null;
		return (o != null && o.getClass().isAnnotationPresent(Render.class))
				|| (t != null && t.isAnnotationPresent(Render.class)) ? x -> {
					render(i, (HttpExchange) x);
					return true;
				} : null;
	}

	protected void render(RenderEngine.Entry input, HttpExchange exchange) {
		{
			var rs = exchange.getResponse();
			if (rs.getStatus() == 0)
//				rs.setStatus(HttpResponse.Status.of(200));
				rs.setStatus(200);
			var hh = rs.getHeaders();
			if (hh.stream().noneMatch(x -> x.name().equals("cache-control")))
				hh.add(new HeaderField("cache-control", "no-cache"));
			if (hh.stream().noneMatch(x -> x.name().equals("content-type")))
				hh.add(new HeaderField("content-type", "text/html"));
		}

		var e = createRenderEngine(exchange);
		e.setToInterpolator(s -> {
			String t;
			if (s.contains("\n"))
				t = s;
			else {
				t = templateMap.get().get(s);
				if (t == null)
					throw new NullPointerException(s);
			}
			var ct = exchange.getResponse().getHeaders().stream().filter(x -> x.name().equals("content-type"))
					.map(HeaderField::value).findFirst().orElse(null);
			var l = switch (ct) {
			case "text/html" -> Interpolator.Language.HTML;
			case "text/javascript" -> Interpolator.Language.JAVASCRIPT;
			default -> throw new RuntimeException();
			};
			return Interpolator.of(t, l);
		});
		e.getStack().push(RenderEngine.Entry.of(null, exchange, null));
		var o = e.render(input);
		if (o != null) {
			var b = o.toString().getBytes();
//			var c = (WritableByteChannel) exchange.getResponse().getBody();
//			try {
//				IO.write(b, c);
//			} catch (IOException f) {
//				throw new UncheckedIOException(f);
//			}
			exchange.getResponse().getHeaders().add(new HeaderField("content-length", String.valueOf(b.length)));
//			var c = (OutputStream) exchange.getResponse().getBody();
//			try {
//				c.write(b);
//			} catch (IOException f) {
//				throw new UncheckedIOException(f);
//			}
			exchange.getResponse().setBody(b);
		}
	}

	protected RenderEngine createRenderEngine(HttpExchange exchange) {
		return new RenderEngine();
	}
}

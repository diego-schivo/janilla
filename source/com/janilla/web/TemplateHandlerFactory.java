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
import java.nio.channels.WritableByteChannel;

import com.janilla.frontend.Interpolator;
import com.janilla.frontend.RenderEngine;
import com.janilla.frontend.RenderEngine.ObjectAndType;
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
		var s = context.getResponse();
		if (s.getStatus() == null)
			s.setStatus(new Status(200, "OK"));

		var h = s.getHeaders();
		if (h.get("Cache-Control") == null)
			s.getHeaders().set("Cache-Control", "no-cache");

		RenderEngine e = new RenderEngine();
		e.setToInterpolator(t -> {
			String u;
			if (t.contains("\n"))
				u = t;
			else {
				var c = application.getClass();
				var i = c.getResourceAsStream(t);
				if (i == null)
					throw new NullPointerException(c + " " + t);
				u = new String(i.readAllBytes());
			}
			return Interpolator.of(u);
		});
		var b = e.render(input).toString().getBytes();
		var c = (WritableByteChannel) s.getBody();
		IO.write(b, c);
	}
}

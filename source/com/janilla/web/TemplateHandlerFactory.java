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
import com.janilla.http.ExchangeContext;
import com.janilla.http.HttpResponse.Status;
import com.janilla.io.IO;
import com.janilla.io.IO.Consumer;

public class TemplateHandlerFactory implements HandlerFactory {

	@Override
	public Consumer<ExchangeContext> createHandler(Object object, ExchangeContext context) {
//		var c = object.getClass();
//		var r = object != null ? c.getAnnotation(Render.class) : null;
		var i = object instanceof ObjectAndType x ? x : null;
		var o = i != null ? i.object() : null;
		return o != null && o.getClass().isAnnotationPresent(Render.class) ? x -> render(i, x) : null;
	}

	protected void render(ObjectAndType input, ExchangeContext context) throws IOException {
		var s = context.getResponse();
		if (s.getStatus() == null)
			s.setStatus(new Status(200, "OK"));

		var h = s.getHeaders();
		if (h.get("Cache-Control") == null)
			s.getHeaders().set("Cache-Control", "no-cache");
//		if (h.get("Content-Type") == null) {
//			var n = render.template();
//			var i = n != null ? n.lastIndexOf('.') : -1;
//			var e = i >= 0 ? n.substring(i + 1) : null;
//			if (e != null)
//				switch (e) {
//				case "html":
//					h.set("Content-Type", "text/html");
//					break;
//				case "js":
//					h.set("Content-Type", "text/javascript");
//					break;
//				}
//		}

		RenderEngine e = new RenderEngine();
		e.setToInterpolator(t -> {
			String u;
			if (t.contains("\n"))
				u = t;
			else {
				var i = input.object().getClass().getResourceAsStream(t);
				if (i == null)
					throw new NullPointerException(t);
				u = new String(i.readAllBytes());
			}
			return Interpolator.of(u);
		});
		var b = e.render(input).toString().getBytes();
		var c = (WritableByteChannel) s.getBody();
		IO.write(b, c);
	}
}

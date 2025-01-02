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

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;

public class TemplateHandlerFactory implements WebHandlerFactory {

	protected Object application;

	public void setApplication(Object application) {
		this.application = application;
	}

	@Override
	public HttpHandler createHandler(Object object, HttpExchange exchange) {
		return object instanceof Renderable r ? x -> {
			render(r, (HttpExchange) x);
			return true;
		} : null;
	}

	protected void render(Renderable<?> input, HttpExchange exchange) {
		var rs = exchange.getResponse();
		if (rs.getStatus() == 0)
			rs.setStatus(200);
		if (rs.getHeaderValue("cache-control") == null)
			rs.setHeaderValue("cache-control", "no-cache");
		if (rs.getHeaderValue("content-type") == null)
			rs.setHeaderValue("content-type", "text/html");
		var s = input.get();
		if (s != null) {
			var bb = s.getBytes();
			rs.setHeaderValue("content-length", String.valueOf(bb.length));
			rs.setBody(bb);
		}
	}
}

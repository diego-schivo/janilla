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
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

import com.janilla.frontend.RenderEngine.ObjectAndType;
import com.janilla.http.HttpExchange;
import com.janilla.io.IO;
import com.janilla.json.Json;
import com.janilla.json.JsonToken;
import com.janilla.json.ReflectionJsonIterator;

public class JsonHandlerFactory implements HandlerFactory {

	@Override
	public IO.Consumer<HttpExchange> createHandler(Object object, HttpExchange exchange) {
//		if (object instanceof HttpRequest || object instanceof Exception)
//			return null;
//		return c -> render(object, c);
		return object instanceof ObjectAndType i ? x -> render(i.object(), x) : null;
	}

	protected void render(Object object, HttpExchange exchange) throws IOException {
		var s = exchange.getResponse();
		s.getHeaders().set("Content-Type", "application/json");

		var t = Json.format(newJsonIterator(object, exchange));
//		System.out.println("t=" + t);
		var b = ByteBuffer.wrap(t.getBytes());
		var c = (WritableByteChannel) s.getBody();
		IO.repeat(x -> c.write(b), b.remaining());
	}

	protected Iterator<JsonToken<?>> newJsonIterator(Object object, HttpExchange exchange) {
		return new ReflectionJsonIterator(object);
	}
}

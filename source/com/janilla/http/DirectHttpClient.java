/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
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
package com.janilla.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.util.function.Function;

public class DirectHttpClient extends HttpClient {

	protected final HttpServer server;

	public DirectHttpClient(HttpServer server) {
		super(null);
		this.server = server;
	}

	@Override
	public <R> R send(HttpRequest request, Function<HttpResponse, R> function) {
		var rs0 = new HttpResponse();
		var o = new ByteArrayOutputStream();
		try (var rs = rs0) {
			rs.setStatus(0);
			rs.setBody(Channels.newChannel(o));
			var ex = server.createExchange(request, rs);
			ScopedValue.where(HttpServer.HTTP_EXCHANGE, ex).call(() -> server.handleExchange(ex));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		try (var rs = rs0) {
			rs.setBody(Channels.newChannel(new ByteArrayInputStream(o.toByteArray())));
			return function.apply(rs);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

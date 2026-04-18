/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.conduit.fullstack;

import java.net.SocketAddress;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.janilla.conduit.backend.ConduitBackend;
import com.janilla.conduit.frontend.ConduitFrontend;
import com.janilla.http.DefaultHttpServer;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.ioc.Context;

@Context("fullstack")
class HttpServerImpl extends DefaultHttpServer {

	protected final ConduitBackend backend;

	protected final ConduitFrontend frontend;

	public HttpServerImpl(SocketAddress endpoint, SSLContext sslContext, HttpHandler handler, ConduitBackend backend,
			ConduitFrontend frontend) {
		super(endpoint, sslContext, handler);
		this.backend = backend;
		this.frontend = frontend;
	}

	@Override
	public HttpExchange createExchange(HttpRequest request, HttpResponse response) {
		var f = request.getPath().startsWith("/api/") ? backend.diFactory() : frontend.diFactory();
		var c = f.classFor(HttpExchange.class);
		return c != null ? f.newInstance(c, Map.of("request", request, "response", response))
				: super.createExchange(request, response);
	}
}

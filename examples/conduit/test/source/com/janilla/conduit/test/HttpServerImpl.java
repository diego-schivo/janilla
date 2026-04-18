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
package com.janilla.conduit.test;

import java.net.SocketAddress;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.janilla.conduit.fullstack.ConduitFullstack;
import com.janilla.http.DefaultHttpServer;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;

class HttpServerImpl extends DefaultHttpServer {

	protected final ConduitFullstack fullstack;

	public HttpServerImpl(SocketAddress endpoint, SSLContext sslContext, HttpHandler handler,
			ConduitFullstack fullstack) {
		super(endpoint, sslContext, handler);
		this.fullstack = fullstack;
	}

	@Override
	public HttpExchange createExchange(HttpRequest request, HttpResponse response) {
		if (WebHandling.TEST_ONGOING.get()) {
			var f = request.getPath().startsWith("/api/") ? fullstack.backend().diFactory()
					: fullstack.frontend().diFactory();
			var c = f.classFor(HttpExchange.class);
			if (c != null)
				return f.newInstance(c, Map.of("request", request, "response", response));
		}
		return super.createExchange(request, response);
	}
}

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
package com.janilla.janillacom.backend;

import java.net.SocketAddress;
import java.util.Map;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import com.janilla.backend.web.Backend;
import com.janilla.http.DefaultHttpServer;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.janillacom.JanillaDomain;

class HttpServerImpl extends DefaultHttpServer {

	protected final Function<String, Backend<?>> authorityToBackend;

	public HttpServerImpl(SocketAddress endpoint, SSLContext sslContext, HttpHandler handler,
			Function<String, Backend<?>> authorityToBackend) {
		super(endpoint, sslContext, handler);
		this.authorityToBackend = authorityToBackend;
	}

	@Override
	protected void exchange(HttpRequest request, HttpResponse response) {
		var a = request.getHeaderValue(":authority");
		if (a == null)
			a = request.getHeaderValue("Host");
		var b = authorityToBackend.apply(a);
//		IO.println("HttpServerImpl.exchange, a=" + a + ", b=" + b);
		ScopedValue.where(JanillaDomain.WEB_APP, b).run(() -> super.exchange(request, response));
	}

	@Override
	public HttpExchange createExchange(HttpRequest request, HttpResponse response) {
		var b = (Backend<?>) JanillaDomain.WEB_APP.get();
//		IO.println("HttpServerImpl.createExchange, b=" + b);
		var e = b.diFactory().newInstance(b.diFactory().classFor(HttpExchange.class),
				Map.of("request", request, "response", response));
		return e != null ? e : super.createExchange(request, response);
	}
}

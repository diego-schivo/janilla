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

import javax.net.ssl.SSLContext;

import com.janilla.http.DefaultHttpServer;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.ioc.DiFactory;
import com.janilla.janillacom.JanillaDomain;
import com.janilla.java.JavaReflect;

public class CustomHttpServer extends DefaultHttpServer {

	protected final JanillaBackend application;

	public CustomHttpServer(SocketAddress endpoint, SSLContext sslContext, HttpHandler handler,
			JanillaBackend application) {
		super(endpoint, sslContext, handler);
		this.application = application;
	}

	@Override
	protected void exchange(HttpRequest request, HttpResponse response) {
		var a1 = request.getHeaderValue(":authority");
		if (a1 == null)
			a1 = request.getHeaderValue("Host");
		var a2 = application.application(a1);
//		IO.println("CustomHttpServer.exchange, a1=" + a1 + ", a2=" + a2);
		ScopedValue.where(JanillaDomain.APPLICATION, a2).run(() -> super.exchange(request, response));
	}

	@Override
	public HttpExchange createExchange(HttpRequest request, HttpResponse response) {
//		var a = application.application(request.getAuthority());
		var a = JanillaDomain.APPLICATION.get();
//		IO.println("CustomHttpServer.createExchange, a=" + a);
		var f = (DiFactory) JavaReflect.property(a.getClass(), "diFactory").get(a);
		var e = f.newInstance(f.classFor(HttpExchange.class), Map.of("request", request, "response", response));
		return e != null ? e : super.createExchange(request, response);
	}
}

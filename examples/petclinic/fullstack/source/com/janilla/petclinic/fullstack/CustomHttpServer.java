/*
 * Copyright 2012-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.janilla.petclinic.fullstack;

import java.net.SocketAddress;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.http.HttpServer;
import com.janilla.ioc.Context;
import com.janilla.petclinic.backend.PetclinicBackend;
import com.janilla.petclinic.frontend.PetclinicFrontend;

@Context("fullstack")
public class CustomHttpServer extends HttpServer {

	protected final PetclinicBackend backend;

	protected final PetclinicFrontend frontend;

	public CustomHttpServer(SSLContext sslContext, SocketAddress endpoint, HttpHandler handler,
			PetclinicBackend backend, PetclinicFrontend frontend) {
		super(sslContext, endpoint, handler);
		this.backend = backend;
		this.frontend = frontend;
	}

	@Override
	protected HttpExchange createExchange(HttpRequest request, HttpResponse response) {
		var x = request.getPath().startsWith("/api/") ? backend.diFactory() : frontend.diFactory();
		return x.newInstance(x.classFor(HttpExchange.class), Map.of("request", request, "response", response));
	}
}

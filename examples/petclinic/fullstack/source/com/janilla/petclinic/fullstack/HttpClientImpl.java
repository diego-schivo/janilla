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

import java.util.Map;

import com.janilla.http.DirectHttpClient;
import com.janilla.http.HttpServer;
import com.janilla.ioc.Context;
import com.janilla.web.WebApp;

@Context("frontend")
class HttpClientImpl extends DirectHttpClient {

	public HttpClientImpl() {
		var b = ((PetclinicFullstack) WebApp.INSTANCE.get()).backend();
		super(b.diFactory().newInstance(b.diFactory().classFor(HttpServer.class), Map.of("handler", b.httpHandler())));
	}
}

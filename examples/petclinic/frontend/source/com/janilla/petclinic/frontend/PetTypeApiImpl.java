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
package com.janilla.petclinic.frontend;

import java.net.URI;
import java.util.List;

import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpRequest;
import com.janilla.java.DefaultConverter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.petclinic.PetType;
import com.janilla.petclinic.PetTypeApi;

class PetTypeApiImpl implements PetTypeApi {

	protected final FrontendConfig config;

	protected final HttpClient httpClient;

	public PetTypeApiImpl(FrontendConfig config, HttpClient httpClient) {
		this.config = config;
		this.httpClient = httpClient;
	}

	@Override
	public List<PetType> read() {
		var u = URI.create(config.api().url() + "/pet-types");
		var o = httpClient.send(new HttpRequest("GET", u), HttpClient.JSON);
		return new DefaultConverter().convert(o, new SimpleParameterizedType(List.class, List.of(PetType.class)));
	}
}

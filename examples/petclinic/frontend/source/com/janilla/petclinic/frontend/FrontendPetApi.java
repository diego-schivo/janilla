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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.Properties;

import com.janilla.http.HttpClient;
import com.janilla.http.HttpRequest;
import com.janilla.java.Converter;
import com.janilla.java.UriQueryBuilder;
import com.janilla.json.Json;
import com.janilla.petclinic.Pet;
import com.janilla.petclinic.PetApi;

public class FrontendPetApi implements PetApi {

	protected final Properties configuration;

	protected final HttpClient httpClient;

	public FrontendPetApi(Properties configuration, HttpClient httpClient) {
		this.configuration = configuration;
		this.httpClient = httpClient;
	}

	@Override
	public Pet create(Pet pet) {
		var rq = new HttpRequest("POST", URI.create(configuration.getProperty("petclinic.api.url") + "/pets"));
		rq.setHeaderValue("content-type", "application/json");
		rq.setBody(Channels.newChannel(new ByteArrayInputStream(Json.format(pet, true).getBytes())));
		var o = httpClient.send(rq, HttpClient.JSON);
		return new Converter().convert(o, Pet.class);
	}

	@Override
	public Pet read(Long id, Integer depth) {
		var u = URI.create(configuration.getProperty("petclinic.api.url") + "/pets/" + id + "?"
				+ new UriQueryBuilder().append("depth", depth != null ? depth.toString() : null));
		var o = httpClient.send(new HttpRequest("GET", u), HttpClient.JSON);
		return new Converter().convert(o, Pet.class);
	}

	@Override
	public Pet update(Long id, Pet pet) {
//		IO.println("FrontendPetApi.update, id=" + id + ", pet=" + pet);
		var rq = new HttpRequest("PUT", URI.create(configuration.getProperty("petclinic.api.url") + "/pets/" + id));
		rq.setHeaderValue("content-type", "application/json");
		rq.setBody(Channels.newChannel(new ByteArrayInputStream(Json.format(pet, true).getBytes())));
		var o = httpClient.send(rq, HttpClient.JSON);
		return new Converter().convert(o, Pet.class);
	}
}

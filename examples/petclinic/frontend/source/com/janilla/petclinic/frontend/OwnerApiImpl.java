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
import java.util.List;

import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpRequest;
import com.janilla.java.DefaultConverter;
import com.janilla.java.SimpleParameterizedType;
import com.janilla.java.UriQueryBuilder;
import com.janilla.json.Json;
import com.janilla.persistence.ListPortion;
import com.janilla.petclinic.Owner;
import com.janilla.petclinic.OwnerApi;

class OwnerApiImpl implements OwnerApi {

	protected final FrontendConfig config;

	protected final HttpClient httpClient;

	public OwnerApiImpl(FrontendConfig config, HttpClient httpClient) {
		this.config = config;
		this.httpClient = httpClient;
	}

	@Override
	public Owner create(Owner owner) {
		var rq = new HttpRequest("POST", URI.create(config.api().url() + "/owners"));
		rq.setHeaderValue("content-type", "application/json");
		rq.setBody(Channels.newChannel(new ByteArrayInputStream(Json.format(owner, true).getBytes())));
		var o = httpClient.send(rq, HttpClient.JSON);
		return new DefaultConverter().convert(o, Owner.class);
	}

	@Override
	public ListPortion<Owner> read(String lastName, Integer depth, Integer skip, Integer limit) {
		var u = URI.create(config.api().url() + "/owners?"
				+ new UriQueryBuilder().append("lastName", lastName)
						.append("depth", depth != null ? depth.toString() : null)
						.append("skip", skip != null ? skip.toString() : null)
						.append("limit", limit != null ? limit.toString() : null));
		var o = httpClient.send(new HttpRequest("GET", u), HttpClient.JSON);
		return new DefaultConverter().convert(o, new SimpleParameterizedType(ListPortion.class, List.of(Owner.class)));
	}

	@Override
	public Owner read(Long id, Integer depth) {
		var u = URI.create(config.api().url() + "/owners/" + id + "?"
				+ new UriQueryBuilder().append("depth", depth != null ? depth.toString() : null));
		var o = httpClient.send(new HttpRequest("GET", u), HttpClient.JSON);
		return new DefaultConverter().convert(o, Owner.class);
	}

	@Override
	public Owner update(Long id, Owner owner) {
		IO.println("FrontendOwnerApi.update, id=" + id + ", owner=" + owner);
		var rq = new HttpRequest("PUT", URI.create(config.api().url() + "/owners/" + id));
		rq.setHeaderValue("content-type", "application/json");
		rq.setBody(Channels.newChannel(new ByteArrayInputStream(Json.format(owner, true).getBytes())));
		var o = httpClient.send(rq, HttpClient.JSON);
		return new DefaultConverter().convert(o, Owner.class);
	}
}

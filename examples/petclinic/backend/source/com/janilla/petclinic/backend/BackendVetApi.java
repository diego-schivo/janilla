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
package com.janilla.petclinic.backend;

import com.janilla.backend.persistence.Persistence;
import com.janilla.persistence.ListPortion;
import com.janilla.petclinic.Vet;
import com.janilla.petclinic.VetApi;
import com.janilla.web.Handle;

@Handle(path = "/api/vets")
public class BackendVetApi implements VetApi {

	protected final Persistence persistence;

	public BackendVetApi(Persistence persistence) {
		this.persistence = persistence;
	}

	@Override
	@Handle(method = "GET")
	public ListPortion<Vet> read(Integer depth, Integer skip, Integer limit) {
		var c = persistence.crud(Vet.class);
		var lp = c.listAndCount(false, skip != null ? skip : 0, limit != null ? limit : 0);
		return new ListPortion<>(c.read(lp.elements(), depth != null ? depth : 0), lp.totalSize());
	}
}

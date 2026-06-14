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

import java.util.function.Predicate;

import com.janilla.backend.cms.AbstractCollectionApi;
import com.janilla.backend.persistence.Persistence;
import com.janilla.http.HttpExchange;
import com.janilla.java.Copier;
import com.janilla.java.Direction;
import com.janilla.petclinic.Visit;
import com.janilla.web.Handle;

@Handle(path = "/api/visits")
class VisitApi extends AbstractCollectionApi<Long, Visit> {

	public VisitApi(Predicate<HttpExchange> drafts, Persistence persistence, Copier copier) {
		super(Visit.class, drafts, persistence, "title", copier, Direction.FORWARD, 0);
	}

//	@Override
//	@Handle(method = "POST")
//	public Visit create(Visit visit) {
	//// IO.println("BackendVisitApi.create, visit=" + visit);
//		return persistence.crud(Visit.class).create(visit);
//	}
}

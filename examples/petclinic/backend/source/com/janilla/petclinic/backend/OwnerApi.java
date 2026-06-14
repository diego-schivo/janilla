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
import com.janilla.java.Java;
import com.janilla.persistence.ListPortion;
import com.janilla.petclinic.Owner;
import com.janilla.web.Handle;

@Handle(path = "/api/owners")
class OwnerApi extends AbstractCollectionApi<Long, Owner> {

	public OwnerApi(Predicate<HttpExchange> drafts, Persistence persistence, Copier copier) {
		super(Owner.class, drafts, persistence, "title", copier, Direction.FORWARD, 0);
	}

	@Handle(method = "GET")
	public ListPortion<Owner> read(String search, Direction direction, Long skip, Long limit, Integer depth,
			String lastName) {
		var s = lastName != null && !lastName.isBlank() ? lastName.strip() : null;
		if (s != null) {
			var k = skip != null ? skip.longValue() : 0;
			var l = limit != null ? limit.longValue() : -1;
			var d = depth != null ? depth : 0;
			return crud().filterAndCount("lastName", x -> Java.startsWithIgnoreCase((String) x, s), direction, k, l)
					.map(x -> crud().read(x, d));
		}

		return super.read(search, direction, skip, limit, depth);
	}
}

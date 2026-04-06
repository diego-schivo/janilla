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
import com.janilla.java.JavaReflect;
import com.janilla.persistence.ListPortion;
import com.janilla.petclinic.Owner;
import com.janilla.petclinic.OwnerApi;
import com.janilla.web.Handle;

@Handle(path = "/api/owners")
public class BackendOwnerApi implements OwnerApi {

	protected final Persistence persistence;

	public BackendOwnerApi(Persistence persistence) {
		this.persistence = persistence;
	}

	@Override
	@Handle(method = "POST")
	public Owner create(Owner owner) {
//		IO.println("OwnerApi.create, owner=" + owner);
		return persistence.crud(Owner.class).create(owner);
	}

	@Override
	@Handle(method = "GET")
	public ListPortion<Owner> read(String lastName, Integer depth, Integer skip, Integer limit) {
//		IO.println("BackendOwnerApi.read, lastName=" + lastName + ", depth=" + depth + ", skip=" + skip + ", limit="
//				+ limit);
		var c = persistence.crud(Owner.class);
		var lp = lastName != null && !lastName.isBlank()
				? c.filterAndCount("lastName", x -> startsWithIgnoreCase((String) x, lastName), false, skip != null ? skip : 0,
						limit != null ? limit : -1)
				: c.listAndCount(false, skip != null ? skip : 0, limit != null ? limit : -1);
		return new ListPortion<>(c.read(lp.elements(), depth != null ? depth : 0), lp.totalSize());
	}

	@Override
	@Handle(method = "GET", path = "(\\d+)")
	public Owner read(Long id, Integer depth) {
		return persistence.crud(Owner.class).read(id, depth != null ? depth : 0);
	}

	@Override
	@Handle(method = "PUT", path = "(\\d+)")
	public Owner update(Long id, Owner owner) {
//		IO.println("OwnerApi.update, id=" + id + ", owner=" + owner);
		return persistence.crud(Owner.class).update(id, x -> JavaReflect.copy(owner, x, y -> !y.equals("id")));
	}

	protected static boolean startsWithIgnoreCase(String string, String prefix) {
		return string == prefix || (prefix != null && prefix.length() <= string.length()
				&& string.regionMatches(true, 0, prefix, 0, prefix.length()));
	}
}

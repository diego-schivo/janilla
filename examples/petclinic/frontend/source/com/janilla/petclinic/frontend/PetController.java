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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.janilla.petclinic.Owner;
import com.janilla.petclinic.Pet;
import com.janilla.petclinic.PetApi;
import com.janilla.petclinic.PetTypeApi;
import com.janilla.web.Handle;

/**
 * @author Diego Schivo
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Handle(path = "/owners/(\\d+)/pets")
public class PetController {

	protected final PetApi petApi;

	protected final PetTypeApi petTypeApi;

	public PetController(PetApi petApi, PetTypeApi petTypeApi) {
		this.petApi = petApi;
		this.petTypeApi = petTypeApi;
	}

	@Handle(method = "GET", path = "new")
	public Object initCreate(Long ownerId) {
		var p = Pet.EMPTY.withOwner(Owner.EMPTY.withId(ownerId));
		return new PetForm(p, petTypeApi.read(), null);
	}

	@Handle(method = "POST", path = "new")
	public Object create(Long ownerId, Pet pet) {
		var p = pet.withOwner(Owner.EMPTY.withId(ownerId));

		var ee = validate(p);
		if (!ee.isEmpty())
			return new PetForm(p, petTypeApi.read(), ee);

		p = petApi.create(p);
		return URI.create("/owners/" + p.owner().id());
	}

	@Handle(method = "GET", path = "(\\d+)/edit")
	public Object initUpdate(Long ownerId, Long id) {
		var p = petApi.read(id, null);
		return new PetForm(p, petTypeApi.read(), null);
	}

	@Handle(method = "POST", path = "(\\d+)/edit")
	public Object update(Long ownerId, Long id, Pet pet) {
		var p = pet.withOwner(Owner.EMPTY.withId(ownerId));

		var ee = validate(p);
		if (!ee.isEmpty())
			return new PetForm(p, petTypeApi.read(), ee);

		p = petApi.update(id, p);
		return URI.create("/owners/" + p.owner().id());
	}

	protected Map<String, List<String>> validate(Pet pet) {
		var m = new LinkedHashMap<String, List<String>>();
		if (pet.name() == null || pet.name().isBlank())
			m.computeIfAbsent("name", _ -> new ArrayList<>()).add("must not be blank");
		if (pet.birthDate() == null)
			m.computeIfAbsent("birthDate", _ -> new ArrayList<>()).add("must not be blank");
		if (pet.type() == null)
			m.computeIfAbsent("type", _ -> new ArrayList<>()).add("must not be blank");
		return m;
	}
}

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.janilla.petclinic.Pet;
import com.janilla.petclinic.PetApi;
import com.janilla.petclinic.Visit;
import com.janilla.petclinic.VisitApi;
import com.janilla.web.Handle;

/**
 * @author Diego Schivo
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
@Handle(path = "/owners/(\\d+)/pets/(\\d+)/visits")
public class VisitController {

	protected final VisitApi visitApi;

	protected final PetApi petApi;

	public VisitController(VisitApi visitApi, PetApi petApi) {
		this.visitApi = visitApi;
		this.petApi = petApi;
	}

	@Handle(method = "GET", path = "new")
	public Object initCreate(Long ownerId, Long petId) {
		var p = petApi.read(petId, 1);
		var v = Visit.EMPTY.withPet(p).withDate(LocalDate.now());
		return new VisitForm(v, p.visits(), null);
	}

	@Handle(method = "POST", path = "new")
	public Object create(Long ownerId, Long petId, Visit visit) {
		var v = visit.withPet(Pet.EMPTY.withId(petId));

		var ee = validate(v);
		if (!ee.isEmpty())
			return new VisitForm(v, petApi.read(petId, 1).visits(), ee);

		visitApi.create(v);
		return URI.create("/owners/" + ownerId);
	}

	protected Map<String, List<String>> validate(Visit visit) {
		var m = new HashMap<String, List<String>>();
		if (visit.date() == null)
			m.computeIfAbsent("date", _ -> new ArrayList<>()).add("must not be blank");
		if (visit.description() == null || visit.description().isBlank())
			m.computeIfAbsent("description", _ -> new ArrayList<>()).add("must not be blank");
		return m;
	}
}

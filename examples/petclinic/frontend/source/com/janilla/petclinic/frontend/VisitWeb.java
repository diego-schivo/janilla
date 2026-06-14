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

import com.janilla.frontend.web.FrontendConfig;
import com.janilla.java.Copier;
import com.janilla.petclinic.PetclinicDomain;
import com.janilla.petclinic.Visit;
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
class VisitWeb {

	protected final ApiClientImpl apiClient;

	protected final FrontendConfig config;

	protected final Copier copier;

	protected final PetclinicDomain domain;

	public VisitWeb(ApiClientImpl apiClient, PetclinicDomain domain, Copier copier, FrontendConfig config) {
		this.apiClient = apiClient;
		this.domain = domain;
		this.copier = copier;
		this.config = config;
	}

	@Handle(method = "GET", path = "new")
	public Object initCreate(Long ownerId, Long petId) {
		var p = apiClient.pets().read(petId, 1);
		var v = copier.copy(Map.of("pet", p, "date", LocalDate.now()), domain.emptyVisit());
		return new VisitForm(v, p.visits(), null);
	}

	@Handle(method = "POST", path = "new")
	public Object create(Long ownerId, Long petId, Visit visit) {
		var p = copier.copy(Map.of("id", petId), domain.emptyPet());
		var v = copier.copy(Map.of("pet", p), visit);

		var ee = validate(v);
		if (!ee.isEmpty())
			return new VisitForm(v, apiClient.pets().read(petId, 1).visits(), ee);

		apiClient.visits().create(v);
		return URI.create(config.basePath() + "/owners/" + ownerId);
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

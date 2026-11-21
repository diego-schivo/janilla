/*
 * Copyright 2012-2025 the original author or authors.
 * Copyright 2024-2026 Diego Schivo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.janilla.petclinic.frontend;

import com.janilla.blanktemplate.frontend.BlankApiClient;
import com.janilla.ioc.DiFactory;

/**
 * @author Diego Schivo
 */
class ApiClientImpl extends BlankApiClient {

	protected final OwnerApiClient owners;

	protected final PetApiClient pets;

	protected final PetTypeApiClient petTypes;

	protected final VetApiClient vets;

	protected final VisitApiClient visits;

	public ApiClientImpl(DiFactory diFactory) {
		super(diFactory);

		owners = diFactory.newInstance(diFactory.classFor(OwnerApiClient.class));
		pets = diFactory.newInstance(diFactory.classFor(PetApiClient.class));
		petTypes = diFactory.newInstance(diFactory.classFor(PetTypeApiClient.class));
		vets = diFactory.newInstance(diFactory.classFor(VetApiClient.class));
		visits = diFactory.newInstance(diFactory.classFor(VisitApiClient.class));
	}

	public OwnerApiClient owners() {
		return owners;
	}

	public PetApiClient pets() {
		return pets;
	}

	public PetTypeApiClient petTypes() {
		return petTypes;
	}

	public VetApiClient vets() {
		return vets;
	}

	public VisitApiClient visits() {
		return visits;
	}

}

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

import java.util.Set;

import com.janilla.backend.persistence.Persistence;
import com.janilla.java.JavaReflect;
import com.janilla.petclinic.Pet;
import com.janilla.petclinic.PetApi;
import com.janilla.web.Handle;

@Handle(path = "/api/pets")
public class BackendPetApi implements PetApi {

	protected final Persistence persistence;

	public BackendPetApi(Persistence persistence) {
		this.persistence = persistence;
	}

	@Override
	@Handle(method = "POST")
	public Pet create(Pet pet) {
//		IO.println("PetApi.create, pet=" + pet);
		return persistence.crud(Pet.class).create(pet);
	}

	@Override
	@Handle(method = "GET", path = "(\\d+)")
	public Pet read(Long id, Integer depth) {
		return persistence.crud(Pet.class).read(id, depth != null ? depth : 0);
	}

	@Override
	@Handle(method = "PUT", path = "(\\d+)")
	public Pet update(Long id, Pet pet) {
//		IO.println("PetApi.update, id=" + id + ", owner=" + owner);
		return persistence.crud(Pet.class).update(id,
				x -> JavaReflect.copy(pet, x, y -> !Set.of("id", "owner").contains(y)));
	}
}

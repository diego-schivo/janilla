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
package com.janilla.petclinic;

import java.time.LocalDate;
import java.util.List;

import com.janilla.persistence.Entity;
import com.janilla.persistence.Index;
import com.janilla.persistence.Store;

/**
 * @author Diego Schivo
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@Store
public record Pet(Long id, String name, LocalDate birthDate, PetType type, @Index Owner owner, List<Visit> visits)
		implements Entity<Long> {

	public static final Pet EMPTY = new Pet(null, null, null, null, null, null);

	public Pet withId(Long id) {
		return new Pet(id, name, birthDate, type, owner, visits);
	}

	public Pet withOwner(Owner owner) {
		return new Pet(id, name, birthDate, type, owner, visits);
	}
}

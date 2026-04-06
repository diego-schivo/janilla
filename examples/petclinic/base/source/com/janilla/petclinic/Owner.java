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

import java.util.List;

import com.janilla.persistence.Entity;
import com.janilla.persistence.Index;
import com.janilla.persistence.Store;

/**
 * @author Diego Schivo
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Oliver Drotbohm
 */
@Store
public record Owner(Long id, String firstName, @Index String lastName, String address, String city, String telephone,
		List<Pet> pets) implements Entity<Long> {

	public static final Owner EMPTY = new Owner(null, null, null, null, null, null, null);

	public Owner withId(Long id) {
		return new Owner(id, firstName, lastName, address, city, telephone, pets);
	}
}

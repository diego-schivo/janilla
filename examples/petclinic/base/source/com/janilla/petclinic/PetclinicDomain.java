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
package com.janilla.petclinic;

import java.util.Map;

import com.janilla.blanktemplate.BlankDomain;
import com.janilla.java.Converter;
import com.janilla.java.Copier;

/**
 * @author Diego Schivo
 */
public class PetclinicDomain extends BlankDomain {

	protected final Owner emptyOwner;

	protected final Pet emptyPet;

	protected final Visit emptyVisit;

	public PetclinicDomain(Converter converter, Copier copier) {
		super(converter, copier);

		emptyOwner = converter.convert(Map.of(), Owner.class);
		emptyPet = converter.convert(Map.of(), Pet.class);
		emptyVisit = converter.convert(Map.of(), Visit.class);
	}

	public Owner emptyOwner() {
		return emptyOwner;
	}

	public Pet emptyPet() {
		return emptyPet;
	}

	public Visit emptyVisit() {
		return emptyVisit;
	}

}

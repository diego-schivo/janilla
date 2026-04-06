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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.janilla.backend.persistence.DefaultCrud;
import com.janilla.backend.persistence.Persistence;
import com.janilla.petclinic.Specialty;

/**
 * @author Diego Schivo
 */
public class SpecialtyRepository extends DefaultCrud<Long, Specialty> {

	public SpecialtyRepository(Persistence persistence) {
		super(Specialty.class, null, persistence);
	}

	protected Map<Long, Supplier<Specialty>> readCache = new ConcurrentHashMap<>();

	@Override
	public Specialty read(Long id) {
		return readCache.computeIfAbsent(id, _ -> Lazy.of(() -> super.read(id))).get();
	}
}

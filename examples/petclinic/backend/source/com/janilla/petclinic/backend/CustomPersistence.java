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

import java.util.List;

import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.DefaultPersistence;
import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.java.Converter;
import com.janilla.persistence.Entity;
import com.janilla.petclinic.Specialty;
import com.janilla.petclinic.Vet;

/**
 * @author Diego Schivo
 */
class CustomPersistence extends DefaultPersistence {

	public CustomPersistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables, Converter converter) {
		super(database, storables, converter);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		if (type == Vet.class)
			return (Crud<?, E>) new VetCrud(this);

		if (type == Specialty.class)
			return (Crud<?, E>) new SpecialtyCrud(this);

		return super.newCrud(type);
	}
}

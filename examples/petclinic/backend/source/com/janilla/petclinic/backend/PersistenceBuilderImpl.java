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
package com.janilla.petclinic.backend;

import java.nio.file.Files;
import java.nio.file.Path;

import com.janilla.backend.web.BackendConfig;
import com.janilla.blanktemplate.backend.BlankPersistenceBuilder;

/**
 * @author Diego Schivo
 */
class PersistenceBuilderImpl extends BlankPersistenceBuilder {

	PersistenceBuilderImpl(Path databaseFile, BackendConfig config) {
		super(databaseFile, config);
	}

	@Override
	protected boolean seed() {
		return !Files.exists(databaseFile);
	}
}

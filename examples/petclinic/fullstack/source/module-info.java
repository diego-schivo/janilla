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
/**
 * @author Diego Schivo
 */
module com.janilla.petclinic.fullstack {

	exports com.janilla.petclinic.fullstack;

	opens com.janilla.petclinic.fullstack;

	requires transitive com.janilla.blanktemplate.fullstack;
	requires transitive com.janilla.petclinic.backend;
	requires transitive com.janilla.petclinic.frontend;
}

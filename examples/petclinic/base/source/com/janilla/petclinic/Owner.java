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

import java.time.Instant;
import java.util.List;

import com.janilla.cms.Document;
import com.janilla.cms.DocumentStatus;
import com.janilla.persistence.Index;
import com.janilla.persistence.Store;

/**
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Oliver Drotbohm
 * @author Diego Schivo
 */
@Store
public record Owner(Long id, String firstName, @Index String lastName, String address, String city, String telephone,
		List<Pet> pets, Instant createdAt, Instant updatedAt, DocumentStatus documentStatus, Instant publishedAt)
		implements Document<Long> {
}

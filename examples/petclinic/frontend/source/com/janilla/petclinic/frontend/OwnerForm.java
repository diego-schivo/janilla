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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.janilla.java.Flatten;
import com.janilla.java.JavaReflect;
import com.janilla.petclinic.Owner;
import com.janilla.web.Render;

/**
 * @author Diego Schivo
 */
@Render(template = "createOrUpdateOwnerForm", resource = "/createOrUpdateOwnerForm.html")
record OwnerForm(@Flatten Owner owner, Map<String, List<String>> errors) {

	private static final Map<String, String> LABELS = Map.of("firstName", "First Name", "lastName", "Last Name",
			"address", "Address", "city", "City", "telephone", "Telephone");

	public Function<String, FormField<?>> fields() {
		return x -> {
			var l = LABELS.get(x);
			var v = JavaReflect.property(Owner.class, x).get(owner);
			var ee = errors != null ? errors.get(x) : null;
			return new InputField<>(l, x, (String) v, ee, "text");
		};
	}

	public String button() {
		return owner == null || owner.id() == null ? "Add Owner" : "Update Owner";
	}
}

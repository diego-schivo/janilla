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
package com.janilla.petclinic.frontend;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.janilla.java.JavaReflect;
import com.janilla.petclinic.Visit;
import com.janilla.web.Render;

@Render(template = "createOrUpdateVisitForm", resource = "/createOrUpdateVisitForm.html")
record VisitForm(Visit visit, List<@Render(template = "visit") Visit> previousVisits,
		Map<String, List<String>> errors) {

	private static final Map<String, String> LABELS = Map.of("date", "Date", "description", "Description");

	public Function<String, FormField<?>> fields() {
		return x -> {
			var l = LABELS.get(x);
			var v = JavaReflect.property(Visit.class, x).get(visit);
			var ee = errors != null ? errors.get(x) : null;
			return switch (x) {
			case "date" -> new InputField<>(l, x, (LocalDate) v, ee, "date");
			default -> new InputField<>(l, x, (String) v, ee, "text");
			};
		};
	}
}

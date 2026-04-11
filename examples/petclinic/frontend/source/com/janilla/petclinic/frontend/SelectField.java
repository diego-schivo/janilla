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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.janilla.web.Render;

/**
 * @author Diego Schivo
 */
@Render(template = "selectField", resource = "/selectField.html")
record SelectField<T>(String label, String name, T value, List<String> errors, Map<T, String> items)
		implements FormField<T> {

	public Stream<Option<T>> options() {
		return items.entrySet().stream()
				.map(x -> new Option<>(x.getKey(), x.getValue(), Objects.equals(x.getKey(), value)));
	}

	@Render(template = "option")
	public record Option<T>(T value, String text, boolean selected) {
	}
}

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

import com.janilla.petclinic.Specialty;
import com.janilla.petclinic.Vet;
import com.janilla.web.HtmlRenderer;
import com.janilla.web.Render;

@Render(template = "vetList", resource = "/vetList.html")
record VetList(List<Result> results, Paginator paginator) {

	@Render(template = "result")
	public record Result(Vet vet,
			@Render(renderer = SpecialtiesRenderer.class) List<@Render(template = "specialty") Specialty> specialties) {
	}

	public static class SpecialtiesRenderer extends HtmlRenderer<List<Specialty>> {

		@Override
		public String apply(List<Specialty> value) {
			return !value.isEmpty() ? super.apply(value) : "none";
		}
	}
}

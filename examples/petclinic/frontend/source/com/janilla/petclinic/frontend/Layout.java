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
import java.util.regex.Pattern;

import com.janilla.http.HttpServer;
import com.janilla.web.Render;
import com.janilla.web.Renderable;

/**
 * @author Diego Schivo
 */
@Render(template = "layout", resource = "/layout.html")
record Layout(Renderable<?> content) {

	protected static final List<NavItem> NAV_ITEMS = List.of(new NavItem("home", "Home", "/", "home page"),
			new NavItem("search", "Find owners", "/owners/find", "find owners"),
			new NavItem("list", "Veterinarians", "/vets.html", "veterinarians"), new NavItem("exclamation-triangle",
					"Error", "/oups", "trigger a RuntimeException to see how it is handled"));

	private static final Pattern PATH_PREFIX = Pattern.compile("^/[^/]*");

	public List<NavItem> navItems() {
		return NAV_ITEMS;
	}

	@Render(template = "nav-item")
	public record NavItem(String icon, String text, String href, String title) {

		public String active() {
			var m1 = PATH_PREFIX.matcher(href);
			var m2 = PATH_PREFIX.matcher(HttpServer.HTTP_EXCHANGE.get().request().getPath());
			return m1.find() && m2.find() && m1.group().equals(m2.group()) ? "active" : null;
		}
	}
}

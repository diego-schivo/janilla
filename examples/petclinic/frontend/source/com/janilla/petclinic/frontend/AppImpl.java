package com.janilla.petclinic.frontend;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.janilla.frontend.App;
import com.janilla.http.HttpExchange;
import com.janilla.web.Render;
import com.janilla.web.Renderable;

@Render(template = "app2")
public record AppImpl(Map<String, String> env, Map<String, Object> state, Renderable<?> content) implements App {

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
			var m2 = PATH_PREFIX.matcher(HttpExchange.SCOPED.get().request().getPath());
			return m1.find() && m2.find() && m1.group().equals(m2.group()) ? "active" : null;
		}
	}
}

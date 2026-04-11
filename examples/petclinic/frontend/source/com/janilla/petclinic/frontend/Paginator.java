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

import java.net.URI;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.java.UriQueryBuilder;
import com.janilla.web.HtmlRenderer;
import com.janilla.web.Render;

/**
 * @author Diego Schivo
 */
@Render(template = "paginator", resource = "/paginator.html")
record Paginator(int index, int length, URI uri) {

	public Stream<Item> pages() {
		return IntStream.range(0, length).mapToObj(x -> new Item(x != index ? uri(x + 1) : null, null, null, x + 1));
	}

	public Stream<Item> arrows() {
		return IntStream.range(0, 4).mapToObj(x -> {
			var h = switch (x) {
			case 0, 1 -> index != 0;
			default -> index != length - 1;
			};
			return new Item(h ? uri(switch (x) {
			case 0 -> 1;
			case 1 -> index;
			case 2 -> index + 2;
			default -> length;
			}) : null, switch (x) {
			case 0 -> "First";
			case 1 -> "Previous";
			case 2 -> "Next";
			default -> "Last";
			}, switch (x) {
			case 0 -> "fast-backward";
			case 1 -> "step-backward";
			case 2 -> "step-forward";
			default -> "fast-forward";
			}, null);
		});
	}

	private URI uri(int page) {
		return URI.create(uri.getPath() + "?"
				+ new UriQueryBuilder(uri.getRawQuery()).delete("page").append("page", String.valueOf(page)));
	}

	@Render(renderer = ItemRenderer.class)
	public record Item(URI href, String title, String icon, Object text) {

		public String className() {
			return icon != null ? "fa fa-" + icon : null;
		}
	}

	public static class ItemRenderer extends HtmlRenderer<Item> {

		@Override
		protected String template(Item value) {
			return renderableFactory.template(templateKey1, value.href != null ? "item-on" : "item-off");
		}
	}
}

/*
 * MIT License
 *
 * Copyright (c) React Training LLC 2015-2019
 * Copyright (c) Remix Software Inc. 2020-2021
 * Copyright (c) Shopify Inc. 2022-2023
 * Copyright (c) Diego Schivo 2024-2026
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.addressbook.frontend;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import com.janilla.frontend.Index;
import com.janilla.frontend.AbstractIndexFactory;
import com.janilla.frontend.Template;
import com.janilla.http.HttpExchange;
import com.janilla.web.ResourceMap;

public class IndexFactoryImpl extends AbstractIndexFactory {

	protected final Properties configuration;

	public IndexFactoryImpl(ResourceMap resourceMap, Properties configuration) {
		super(resourceMap);
		this.configuration = configuration;
	}

	@Override
	public Index newIndex(HttpExchange exchange) {
		return new IndexImpl("Address Book", imports(), scripts(),
				new AppImpl(configuration.getProperty("address-book.api.url"), state(exchange)), templates());
	}

	@Override
	protected void putImports(Map<String, String> map) {
		super.putImports(map);
		Stream.of("about", "app", "contact", "edit-contact", "home", "sidebar-layout", "toggle-favorite")
				.forEach(x -> map.put(x, "/" + x + ".js"));
	}

	@Override
	protected String baseImportKey(String name) {
		return "base/" + name;
	}

	@Override
	protected void addTemplates(List<Template> list) {
		Stream.of("about", "app", "contact", "edit-contact", "home", "sidebar-layout", "toggle-favorite")
				.map(this::template).forEach(list::add);
	}
}

/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
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
package com.janilla.ecommercetemplate.backend;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.ecommercetemplate.Country;
import com.janilla.ecommercetemplate.EcommerceDomain;
import com.janilla.ecommercetemplate.Title;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.web.Handle;
import com.janilla.websitetemplate.backend.WebsiteBackend;

public class EcommerceBackend extends WebsiteBackend {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(WebsiteBackend.diTypes(), Java.getPackageTypes("com.janilla.ecommercetemplate"),
				Java.getPackageTypes("com.janilla.ecommercetemplate.backend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		serve(f, args.length > 0 ? args[0] : null);
	}

	public EcommerceBackend(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "ecommerce-template");
	}

	protected EcommerceBackend(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey);
	}

	@Override
	protected Class<?> dataType() {
		return Data.class;
	}

	@Handle(method = "GET", path = "/api/enums")
	public Map<String, List<String>> enums() {
		var cc = ((EcommerceDomain) domain);
		return Map.of(Title.class.getSimpleName(), cc.titles().map(x -> x.name()).toList(),
				Country.class.getSimpleName(), cc.countries().map(x -> x.name()).toList());
	}
}

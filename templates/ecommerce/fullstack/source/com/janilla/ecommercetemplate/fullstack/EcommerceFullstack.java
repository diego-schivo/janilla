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
package com.janilla.ecommercetemplate.fullstack;

import java.nio.file.Path;
import java.util.stream.Stream;

import com.janilla.ecommercetemplate.backend.EcommerceBackend;
import com.janilla.ecommercetemplate.frontend.EcommerceFrontend;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.websitetemplate.fullstack.WebsiteFullstack;

public class EcommerceFullstack extends WebsiteFullstack {

	public static Stream<Class<?>> diTypes() {
		return Stream.concat(WebsiteFullstack.diTypes(),
				Java.getPackageTypes("com.janilla.ecommercetemplate.fullstack"));
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList(), "fullstack");
		serve(f, EcommerceFullstack.class, args.length > 0 ? args[0] : null);
	}

	public EcommerceFullstack(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "ecommerce-template");
	}

	public EcommerceFullstack(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey);
	}

	@Override
	protected Stream<Class<?>> diBackendTypes() {
		return Stream.of(EcommerceBackend.diTypes(), Java.getPackageTypes("com.janilla.blanktemplate.fullstack"),
				Java.getPackageTypes("com.janilla.websitetemplate.fullstack"),
				Java.getPackageTypes("com.janilla.ecommercetemplate.fullstack")).flatMap(x -> x);
	}

	@Override
	protected Stream<Class<?>> diFrontendTypes() {
		return Stream.of(EcommerceFrontend.diTypes(), Java.getPackageTypes("com.janilla.blanktemplate.fullstack"),
				Java.getPackageTypes("com.janilla.websitetemplate.fullstack"),
				Java.getPackageTypes("com.janilla.ecommercetemplate.fullstack")).flatMap(x -> x);
	}
}

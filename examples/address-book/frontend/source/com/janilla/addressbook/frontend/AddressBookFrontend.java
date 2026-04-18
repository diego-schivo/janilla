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

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.frontend.web.AbstractFrontend;
import com.janilla.http.HttpClient;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.web.InvocationResolver;

public class AddressBookFrontend extends AbstractFrontend {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.http"), Java.getPackageTypes("com.janilla.web"),
				Java.getPackageTypes("com.janilla.frontend", _ -> true),
				Java.getPackageTypes("com.janilla.addressbook.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		serve(f, args.length > 0 ? args[0] : null, "address-book");
	}

	protected DataFetching dataFetching;

	protected HttpClient httpClient;

	public AddressBookFrontend(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey);
	}

	public DataFetching dataFetching() {
		return dataFetching;
	}

	public HttpClient httpClient() {
		return httpClient;
	}

	@Override
	protected InvocationResolver newInvocationResolver() {
		httpClient = diFactory.newInstance(diFactory.classFor(HttpClient.class));
		dataFetching = diFactory.newInstance(diFactory.classFor(DataFetching.class));
		return super.newInvocationResolver();
	}

	@Override
	protected void putResourcePrefixes(Map<String, String> prefixes) {
		super.putResourcePrefixes(prefixes);
		prefixes.put("com.janilla.addressbook.frontend", "");
	}
}

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
package com.janilla.addressbook.test;

import java.util.Objects;
import java.util.stream.Stream;

import com.janilla.addressbook.backend.AddressBookBackend;
import com.janilla.addressbook.frontend.AddressBookFrontend;
import com.janilla.addressbook.fullstack.AddressBookFullstack;
import com.janilla.frontend.Index;
import com.janilla.frontend.web.AbstractFrontend;
import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.Handle;
import com.janilla.web.NotFoundException;
import com.janilla.web.WebApp;

public class AddressBookTest extends AbstractFrontend<FrontendConfig> {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.http"), Java.getPackageTypes("com.janilla.java"),
				Java.getPackageTypes("com.janilla.web"), Java.getPackageTypes("com.janilla.frontend", _ -> true),
				Java.getPackageTypes("com.janilla.addressbook.test")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		var c = newConfig(new Class<?>[] { AddressBookTest.class }, args.length != 0 ? args[0] : null, f);
		var a = f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f));
		serve(a);
	}

	protected final AddressBookFullstack fullstack;

	public AddressBookTest(FrontendConfig config, DiFactory diFactory) {
		super(config, diFactory);

		{
			var f = new DefaultDiFactory(AddressBookFullstack.diTypes().toList(), "fullstack");
			var c = newConfig(new Class<?>[] { AddressBookBackend.class, AddressBookFrontend.class,
					AddressBookFullstack.class, AddressBookTest.class }, null, f);
			fullstack = f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f));
		}
	}

	public AddressBookFullstack fullstack() {
		return fullstack;
	}

	@Handle(method = "GET", path = "/")
	public Index home(HttpExchange exchange) {
		return indexFactory.newIndex(exchange);
	}

	@Override
	protected HttpHandler newHttpHandler() {
		var f = diFactory.newInstance(diFactory.classFor(ApplicationHandlerFactory.class));
		return x -> {
			var h = Test.ONGOING.get() && !x.request().getPath().startsWith("/test/") ? fullstack.httpHandler()
					: (HttpHandler) x2 -> {
						var h2 = f.createHandler(Objects.requireNonNullElse(x2.exception(), x2.request()));
						if (h2 == null)
							throw new NotFoundException(x2.request().getHeaderValue(":method") + " "
									+ x2.request().getHeaderValue(":path"));
						return h2.handle(x2);
					};
			return h.handle(x);
		};
	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();
		resourcePrefixes.put("com.janilla.addressbook.test", "");
	}
}

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
package com.janilla.addressbook.test;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.blanktemplate.test.BlankTest;
import com.janilla.frontend.web.FrontendConfig;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.addressbook.backend.AddressBookBackend;
import com.janilla.addressbook.frontend.AddressBookFrontend;
import com.janilla.addressbook.fullstack.AddressBookFullstack;
import com.janilla.web.PackageResourcesProvider;
import com.janilla.web.WebApp;

public class AddressBookTest extends BlankTest {

	private static final Logger LOGGER = System.getLogger(AddressBookTest.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.concat(BlankTest.diTypes(), Java.getPackageTypes("com.janilla.addressbook.test"));
	};

	public static void main(String[] args) {
		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0]);
		var c = newConfig(new Class<?>[] { AddressBookBackend.class, AddressBookFrontend.class, AddressBookFullstack.class,
				AddressBookTest.class }, args.length != 0 ? args[0] : null, f);
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f, "context",
				(Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x)));
		serve(a[0]);
	}

	public AddressBookTest(FrontendConfig config, DiFactory diFactory, Consumer<Object> context) {
		super(config, diFactory, context);
	}

	@Override
	protected Stream<Class<?>> diFullstackTypes() {
		return AddressBookFullstack.diTypes();
	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();

		resourcesProviders.put(new PackageResourcesProvider("com.janilla.addressbook.test"), "");
	}

}

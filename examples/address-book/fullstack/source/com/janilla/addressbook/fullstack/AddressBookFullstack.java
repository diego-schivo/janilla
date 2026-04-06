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
package com.janilla.addressbook.fullstack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.addressbook.backend.AddressBookBackend;
import com.janilla.addressbook.backend.BackendExchange;
import com.janilla.addressbook.frontend.AddressBookFrontend;
import com.janilla.http.HttpClient;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;

public class AddressBookFullstack {

	public static final String[] DI_PACKAGES = { "com.janilla.web", "com.janilla.addressbook.fullstack" };

	public static final ScopedValue<AddressBookFullstack> INSTANCE = ScopedValue.newInstance();

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DefaultDiFactory(
				Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageClasses(x, false).stream()).toList(),
				"fullstack");
		serve(f, args.length > 0 ? args[0] : null);
	}

	protected static void serve(DiFactory diFactory, String configurationPath) {
		AddressBookFullstack a;
		{
			a = diFactory.newInstance(diFactory.classFor(AddressBookFullstack.class),
					Java.hashMap("diFactory", diFactory, "configurationFile",
							configurationPath != null ? Path.of(configurationPath.startsWith("~")
									? System.getProperty("user.home") + configurationPath.substring(1)
									: configurationPath) : null));
		}

		SSLContext c;
		{
			var p = a.configuration.getProperty("address-book.server.keystore.path");
			if (p != null) {
				var w = a.configuration.getProperty("address-book.server.keystore.password");
				if (p.startsWith("~"))
					p = System.getProperty("user.home") + p.substring(1);
				var f = Path.of(p);
				if (!Files.exists(f))
					Java.generateKeyPair("localhost", f, w, "dns:localhost,ip:127.0.0.1");
				try (var s = Files.newInputStream(f)) {
					c = Java.sslContext(s, w.toCharArray());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} else
				c = HttpClient.sslContext("TLSv1.3");
		}

		HttpServer s;
		{
			var p = Integer.parseInt(a.configuration.getProperty("address-book.server.port"));
			s = a.diFactory.newInstance(a.diFactory.classFor(HttpServer.class),
					Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
		}
		s.serve();
	}

	protected AddressBookBackend backend;

	protected final Properties configuration;

	protected final DiFactory diFactory;

	protected AddressBookFrontend frontend;

	protected final HttpHandler handler;

	public AddressBookFullstack(DiFactory diFactory, Path configurationFile) {
		this.diFactory = diFactory;
		diFactory.context(this);
		configuration = diFactory.newInstance(diFactory.classFor(Properties.class),
				Collections.singletonMap("file", configurationFile));

		handler = x -> {
			var h = x instanceof BackendExchange ? backend.handler() : frontend.handler();
			return h.handle(x);
		};

		var cf = Optional.ofNullable(configurationFile).orElseGet(() -> {
			try {
				return Path.of(AddressBookFullstack.class.getResource("configuration.properties").toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		});
		backend = ScopedValue.where(INSTANCE, this)
				.call(() -> diFactory.newInstance(diFactory.classFor(AddressBookBackend.class),
						Java.hashMap("diFactory",
								new DefaultDiFactory(Stream
										.concat(Stream.of("com.janilla.web"),
												Stream.of("backend", "fullstack")
														.map(x -> AddressBookBackend.class.getPackageName()
																.replace(".backend", "." + x)))
										.flatMap(x -> Java.getPackageClasses(x, false).stream()).toList(), "backend"),
								"configurationFile", cf)));
		frontend = ScopedValue.where(INSTANCE, this)
				.call(() -> diFactory.newInstance(diFactory.classFor(AddressBookFrontend.class),
						Java.hashMap("diFactory",
								new DefaultDiFactory(Stream
										.concat(Stream.of("com.janilla.web"),
												Stream.of("frontend", "fullstack")
														.map(x -> AddressBookFrontend.class.getPackageName()
																.replace(".frontend", "." + x)))
										.flatMap(x -> Java.getPackageClasses(x, false).stream()).toList(), "frontend"),
								"configurationFile", cf)));
	}

	public AddressBookBackend backend() {
		return backend;
	}

	public Properties configuration() {
		return configuration;
	}

	public DiFactory diFactory() {
		return diFactory;
	}

	public AddressBookFrontend frontend() {
		return frontend;
	}

	public HttpHandler handler() {
		return handler;
	}
}

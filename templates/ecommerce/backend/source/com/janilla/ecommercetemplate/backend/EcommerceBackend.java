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

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.ecommercetemplate.Country;
import com.janilla.ecommercetemplate.EcommerceDomain;
import com.janilla.ecommercetemplate.Title;
import com.janilla.http.HttpServer;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.net.AbstractServer;
import com.janilla.web.Handle;
import com.janilla.websitetemplate.backend.WebsiteBackend;

public class EcommerceBackend extends WebsiteBackend {

	public static final String[] DI_PACKAGES = Stream
			.concat(Arrays.stream(WebsiteBackend.DI_PACKAGES),
					Stream.of("com.janilla.ecommercetemplate", "com.janilla.ecommercetemplate.backend"))
			.toArray(String[]::new);

	public static void main(String[] args) {
		try {
			EcommerceBackend a;
			{
				var f = new DefaultDiFactory(
						Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageTypes(x)).toList());
				a = f.newInstance(f.classFor(EcommerceBackend.class),
						Java.hashMap("diFactory", f, "configurationFile",
								args.length > 0 ? Path.of(
										args[0].startsWith("~") ? System.getProperty("user.home") + args[0].substring(1)
												: args[0])
										: null));
			}

			HttpServer s;
			{
				SSLContext c;
				try (var x = AbstractServer.class.getResourceAsStream("localhost")) {
					c = Java.sslContext(x, "passphrase".toCharArray());
				}
				var p = Integer.parseInt(a.configuration.getProperty(a.configurationKey + ".server.port"));
				s = a.diFactory.newInstance(a.diFactory.classFor(HttpServer.class),
						Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
			}
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public EcommerceBackend(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "ecommerce-template");
	}

	public EcommerceBackend(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey);
	}

	@Override
	protected Class<?> dataType() {
		return Data.class;
	}

	@Handle(method = "GET", path = "/api/enums")
	public Map<String, List<String>> enums() {
//		return Stream.of(Title.class, Country.class).collect(Collectors.toMap(x -> x.getSimpleName(),
//				x -> Arrays.stream(x.getEnumConstants()).map(Enum::name).toList()));
		var cc = ((EcommerceDomain) domain);
		return Map.of(Title.class.getSimpleName(), cc.titles().map(x -> x.name()).toList(),
				Country.class.getSimpleName(), cc.countries().map(x -> x.name()).toList());
	}
}

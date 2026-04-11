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
package com.janilla.ecommercetemplate.frontend;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import com.janilla.http.HttpServer;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.net.AbstractServer;
import com.janilla.websitetemplate.frontend.WebsiteFrontend;

public class EcommerceFrontend extends WebsiteFrontend {

	public static final String[] DI_PACKAGES = Stream
			.concat(Arrays.stream(WebsiteFrontend.DI_PACKAGES),
					Stream.of("com.janilla.ecommercetemplate", "com.janilla.ecommercetemplate.frontend"))
			.toArray(String[]::new);

	public static void main(String[] args) {
		try {
			EcommerceFrontend a;
			{
				var f = new DefaultDiFactory(
						Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageTypes(x, false)).toList());
				a = f.newInstance(f.classFor(EcommerceFrontend.class),
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
				var p = Integer.parseInt(a.configuration.getProperty("ecommerce-template.server.port"));
				s = a.diFactory.newInstance(a.diFactory.classFor(HttpServer.class),
						Map.of("sslContext", c, "endpoint", new InetSocketAddress(p), "handler", a.handler));
			}
			s.serve();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public EcommerceFrontend(DiFactory diFactory, Path configurationFile) {
		this(diFactory, configurationFile, "ecommerce-template");
	}

	public EcommerceFrontend(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey);
	}

//	@Override
//	protected Map<String, List<Path>> resourcePaths() {
//		var pp1 = Java.getPackagePaths("com.janilla.frontend", false).filter(Files::isRegularFile).toList();
//		var pp2 = Java.getPackagePaths("com.janilla.frontend.cms", false).filter(Files::isRegularFile).toList();
//		var pp3 = Java.getPackagePaths(BlankFrontend.class.getPackageName(), false).filter(Files::isRegularFile)
//				.toList();
//		var pp4 = Java.getPackagePaths(WebsiteFrontend.class.getPackageName(), false).filter(Files::isRegularFile)
//				.toList();
//		var pp5 = Java.getPackagePaths(EcommerceFrontend.class.getPackageName(), false).filter(Files::isRegularFile)
//				.toList();
//		return Map.of("/base", pp1, "/cms", pp2, "/blank", pp3, "/website", pp4, "", pp5);
//	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();
		resourcePrefixes.put("com.janilla.websitetemplate.frontend", "/website");
		resourcePrefixes.put("com.janilla.ecommercetemplate.frontend", "");
	}
}

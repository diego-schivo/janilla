/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Diego Schivo
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
package com.janilla.janillacom.frontend;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.janilla.http.HttpExchange;
import com.janilla.http.HttpHandler;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.janillacom.JanillaDomain;
import com.janilla.java.Java;
import com.janilla.java.JavaReflect;
import com.janilla.websitetemplate.frontend.WebsiteFrontend;

public class JanillaFrontend extends WebsiteFrontend {

	public static final String[] DI_PACKAGES = Stream.concat(Arrays.stream(WebsiteFrontend.DI_PACKAGES),
			Stream.of("com.janilla.janillacom", "com.janilla.janillacom.frontend")).toArray(String[]::new);

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DefaultDiFactory(
				Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageClasses(x, false).stream()).toList());
		serve(f, JanillaFrontend.class, args.length > 0 ? args[0] : null);
	}

//	protected final ApplicationApi applicationApi;

	protected final Map<String, Object> applications = new ConcurrentHashMap<>();

	public JanillaFrontend(DiFactory diFactory, Path configurationFile) {
		super(diFactory, configurationFile, "janilla-com");

//		applicationApi = diFactory.newInstance(diFactory.classFor(ApplicationApi.class));
	}

	public JanillaFrontend application() {
		return this;
	}

	public Object application(String authority) {
//		IO.println("JanillaFrontend.application, authority=" + authority);
		var s = "." + configuration.getProperty(configurationKey + ".authority");
		if (!authority.endsWith(s))
			return this;
		return applications.computeIfAbsent(authority.substring(0, authority.length() - s.length()), k -> {
//			IO.println("JanillaFrontend.application, k=" + k);
			var a = ((JanillaDataFetching) dataFetching).applications(k, null, null, null, null, null).elements()
					.getFirst();
//			IO.println("JanillaFrontend.application, a=" + a);
			if (a != null)
				try {
					var c = Class.forName(a.frontend());
					var f = new DefaultDiFactory(Arrays.stream((String[]) c.getDeclaredField("DI_PACKAGES").get(null))
							.flatMap(x -> Java.getPackageClasses(x, false).stream()).toList());
					var cf = configurationFile != null ? configurationFile
							: Path.of(JanillaFrontend.class.getResource("configuration.properties").toURI());
					return f.newInstance(c, Java.hashMap("diFactory", f, "configurationFile", cf));
				} catch (ReflectiveOperationException | URISyntaxException e) {
					throw new RuntimeException(e);
				}
			return this;
		});
	}

	@Override
	protected boolean handle(HttpExchange exchange) {
//		IO.println("JanillaFrontend.handle, exchange=" + exchange);
//		var a = application(exchange.request().getAuthority());
		var a = JanillaDomain.APPLICATION.get();
		return a == this ? super.handle(exchange)
				: ((HttpHandler) JavaReflect.property(a.getClass(), "handler").get(a)).handle(exchange);
	}

//	@Override
//	protected Map<String, List<Path>> resourcePaths() {
//		var pp1 = Java.getPackagePaths("com.janilla.frontend", false).filter(Files::isRegularFile).toList();
//		var pp2 = Java.getPackagePaths("com.janilla.frontend.cms", false).filter(Files::isRegularFile).toList();
//		var pp3 = Java.getPackagePaths(BlankFrontend.class.getPackageName(), false).filter(Files::isRegularFile)
//				.toList();
//		var pp4 = Java.getPackagePaths(WebsiteFrontend.class.getPackageName(), false).filter(Files::isRegularFile)
//				.toList();
//		var pp5 = Java.getPackagePaths(JanillaFrontend.class.getPackageName(), false).filter(Files::isRegularFile)
//				.toList();
//		return Map.of("/base", pp1, "/cms", pp2, "/blank", pp3, "/website", pp4, "", pp5);
//	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();
		resourcePrefixes.put("com.janilla.websitetemplate.frontend", "/website");
		resourcePrefixes.put("com.janilla.janillacom.frontend", "");
	}
}

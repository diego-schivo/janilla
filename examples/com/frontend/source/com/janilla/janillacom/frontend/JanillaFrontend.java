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

	public static Stream<Class<?>> diTypes() {
		return Stream.of(WebsiteFrontend.diTypes(), Java.getPackageTypes("com.janilla.janillacom"),
				Java.getPackageTypes("com.janilla.janillacom.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		serve(f, args.length > 0 ? args[0] : null);
	}

	protected final Map<String, Object> applications = new ConcurrentHashMap<>();

	public JanillaFrontend(DiFactory diFactory, Path configurationFile) {
		super(diFactory, configurationFile, "janilla-com");
	}

	public JanillaFrontend application() {
		return this;
	}

	public Object application(String authority) {
		IO.println("JanillaFrontend.application, authority=" + authority);
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
					@SuppressWarnings("unchecked")
					var tt = ((Stream<Class<?>>) c.getDeclaredMethod("diTypes").invoke(null)).toList();
					var f = new DefaultDiFactory(tt);
					var cf = configurationFile != null ? configurationFile
							: Path.of(JanillaFrontend.class.getResource("configuration.properties").toURI());
					return f.newInstance(c, Java.hashMap("diFactory", f, "configurationFile", cf));
				} catch (ReflectiveOperationException | URISyntaxException e) {
					throw new RuntimeException(e);
				}
			return this;
		});
	}

//	@Override
//	protected boolean handle(HttpExchange exchange) {
////		IO.println("JanillaFrontend.handle, exchange=" + exchange);
//		var a = JanillaDomain.APPLICATION.get();
//		return a == this ? super.handle(exchange)
//				: ((HttpHandler) JavaReflect.property(a.getClass(), "handler").get(a)).handle(exchange);
//	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();
		resourcePrefixes.put("com.janilla.websitetemplate.frontend", "/website");
		resourcePrefixes.put("com.janilla.janillacom.frontend", "");
	}
}

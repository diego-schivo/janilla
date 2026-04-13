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
package com.janilla.janillacom.backend;

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
import com.janilla.janillacom.Application;
import com.janilla.janillacom.JanillaDomain;
import com.janilla.java.Java;
import com.janilla.java.JavaReflect;
import com.janilla.websitetemplate.backend.WebsiteBackend;

public class JanillaBackend extends WebsiteBackend {

	public static final String[] DI_PACKAGES = Stream.concat(Arrays.stream(WebsiteBackend.DI_PACKAGES),
			Stream.of("com.janilla.janillacom", "com.janilla.janillacom.backend")).toArray(String[]::new);

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());
		var f = new DefaultDiFactory(
				Arrays.stream(DI_PACKAGES).flatMap(x -> Java.getPackageTypes(x)).toList());
		serve(f, JanillaBackend.class, args.length > 0 ? args[0] : null);
	}

	protected final Map<String, Object> applications = new ConcurrentHashMap<>();

	public JanillaBackend(DiFactory diFactory, Path configurationFile) {
		super(diFactory, configurationFile, "janilla-com");
	}

	public JanillaBackend application() {
		return this;
	}

	public Object application(String authority) {
//		IO.println("JanillaBackend.application, authority=" + authority);
		var s = "." + configuration.getProperty(configurationKey + ".authority");
//		IO.println("JanillaBackend.application, s=" + s);
		if (!authority.endsWith(s))
			return this;
		return applications.computeIfAbsent(authority.substring(0, authority.length() - s.length()), k -> {
//			IO.println("JanillaBackend.application, k=" + k);
			Application a;
			{
				var c = persistence.crud(Application.class);
				a = c.read(c.find("slug", new Object[] { k }));
			}
//			IO.println("JanillaBackend.application, a=" + a);
			if (a != null)
				try {
					var c = Class.forName(a.backend());
					var pp = (String[]) c.getDeclaredField("DI_PACKAGES").get(null);
//					IO.println("JanillaBackend.application, c=" + c + ", pp=" + Arrays.toString(pp));
					var f = new DefaultDiFactory(
							Arrays.stream(pp).flatMap(x -> Java.getPackageTypes(x)).toList());
					var cf = configurationFile != null ? configurationFile
							: Path.of(JanillaBackend.class.getResource("configuration.properties").toURI());
					return f.newInstance(c, Java.hashMap("diFactory", f, "configurationFile", cf));
				} catch (ReflectiveOperationException | URISyntaxException e) {
					throw new RuntimeException(e);
				}
			return this;
		});
	}

	@Override
	protected Class<?> dataType() {
		return Data.class;
	}

	@Override
	protected boolean handle(HttpExchange exchange) {
//		IO.println("JanillaBackend.handle, exchange=" + exchange);
//		var a = application(exchange.request().getAuthority());
		var a = JanillaDomain.APPLICATION.get();
		return a == this ? super.handle(exchange)
				: ((HttpHandler) JavaReflect.property(a.getClass(), "handler").get(a)).handle(exchange);
	}
}

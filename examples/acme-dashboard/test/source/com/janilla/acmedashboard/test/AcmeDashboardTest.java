/*
 * MIT License
 *
 * Copyright (c) 2024 Vercel, Inc.
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
package com.janilla.acmedashboard.test;

import java.util.Objects;
import java.util.stream.Stream;

import com.janilla.acmedashboard.backend.AcmeDashboardBackend;
import com.janilla.acmedashboard.frontend.AcmeDashboardFrontend;
import com.janilla.acmedashboard.fullstack.AcmeDashboardFullstack;
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

public class AcmeDashboardTest extends AbstractFrontend<FrontendConfig> {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.http"), Java.getPackageTypes("com.janilla.java"),
				Java.getPackageTypes("com.janilla.web"), Java.getPackageTypes("com.janilla.frontend", _ -> true),
				Java.getPackageTypes("com.janilla.acmedashboard.test")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		var c = newConfig(new Class<?>[] { AcmeDashboardTest.class }, args.length != 0 ? args[0] : null, f);
		var a = f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f));
		serve(a);
	}

	protected final AcmeDashboardFullstack fullstack;

	public AcmeDashboardTest(FrontendConfig config, DiFactory diFactory) {
		super(config, diFactory);

		{
			var f = new DefaultDiFactory(AcmeDashboardFullstack.diTypes().toList(), "fullstack");
			var c = newConfig(new Class<?>[] { AcmeDashboardBackend.class, AcmeDashboardFrontend.class,
					AcmeDashboardFullstack.class, AcmeDashboardTest.class }, null, f);
			fullstack = f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f));
		}
	}

	public AcmeDashboardFullstack fullstack() {
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
		resourcePrefixes.put("com.janilla.acmedashboard.test", "");
	}
}

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
package com.janilla.todomvc.test;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.frontend.web.AbstractFrontend;
import com.janilla.frontend.web.FrontendConfig;
import com.janilla.http.HttpHandler;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.todomvc.frontend.TodoMvcFrontend;
import com.janilla.web.Domain;
import com.janilla.web.NotFoundException;
import com.janilla.web.PackageResourcesProvider;
import com.janilla.web.WebApp;
import com.janilla.web.WebAppHandlerFactory;

public class TodoMvcTest extends AbstractFrontend<FrontendConfig, Domain> {

	private static final Logger LOGGER = System.getLogger(TodoMvcTest.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.http"), Java.getPackageTypes("com.janilla.java"),
				Java.getPackageTypes("com.janilla.web"), Java.getPackageTypes("com.janilla.frontend", _ -> true),
				Java.getPackageTypes("com.janilla.todomvc.test")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0]);
		var c = newConfig(new Class<?>[] { TodoMvcFrontend.class, TodoMvcTest.class },
				args.length != 0 ? args[0] : null, f);
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f, "context",
				(Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x)));
		serve(a[0]);
	}

	protected final TodoMvcFrontend frontend;

	public TodoMvcTest(FrontendConfig config, DiFactory diFactory, Consumer<Object> context) {
		super(config, diFactory, context);

		var a = new WebApp[1];
		var f = Ioc.diFactory(TodoMvcFrontend.diTypes().toList(), () -> a[0]);
		var cfg = newConfig(new Class<?>[] { TodoMvcFrontend.class }, null, f);
		Consumer<Object> ctx = x -> a[0] = (WebApp<?, ?>) x;
		frontend = f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", cfg, "diFactory", f, "context", ctx));
	}

	public TodoMvcFrontend frontend() {
		return frontend;
	}

	@Override
	protected HttpHandler newHttpHandler() {
		var f = diFactory.newInstance(diFactory.classFor(WebAppHandlerFactory.class));
		return x -> {
//			IO.println(
//			"TodoMvcTest, " + ex.request().getPath() + ", Test.ongoing=" + Test.ongoing.get());
			var h = WebHandling.TEST_ONGOING.get() && !x.request().getPath().startsWith("/test/")
					? frontend.httpHandler()
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
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.todomvc.test"), "");
	}
}

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

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.backend.web.Backend;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.janillacom.Application;
import com.janilla.janillacom.JanillaDomain;
import com.janilla.java.Java;
import com.janilla.web.NotFoundException;
import com.janilla.web.WebApp;
import com.janilla.web.WebAppHandlerFactory;
import com.janilla.websitetemplate.backend.WebsiteBackend;

public class JanillaBackend extends WebsiteBackend<JanillaBackendConfig> {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(WebsiteBackend.diTypes(), Java.getPackageTypes("com.janilla.janillacom"),
				Java.getPackageTypes("com.janilla.janillacom.backend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		var c = newConfig(new Class<?>[] { JanillaBackend.class }, args.length != 0 ? args[0] : null, f);
		var a = f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f));
		serve(a);
	}

	protected final Map<String, Backend<?>> backends;

	public JanillaBackend(JanillaBackendConfig config, DiFactory diFactory) {
		super(config, diFactory);

		backends = config.backends().keySet().stream()
//			.map(x -> {
//			var c = persistence.crud(Application.class);
//			return c.read(c.find("slug", new Object[] { x }));
//		})
				.map(persistence.crud(Application.class)::read).filter(x -> x.backend() != null)
				.collect(Collectors.toMap(Application::id, a -> {
					try {
						var c = Class.forName(a.backend());
						@SuppressWarnings("unchecked")
						var tt = ((Stream<Class<?>>) c.getDeclaredMethod("diTypes").invoke(null)).toList();
						var f = new DefaultDiFactory(tt);
						var c2 = newConfig(Stream.of(toConfigMap(c), (Map<?, ?>) config.backends().get(a.id()))
								.filter(x -> x != null).toArray(Map<?, ?>[]::new), f);
						return (Backend<?>) f.newInstance(c, Java.hashMap("config", c2, "diFactory", f));
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				}));
	}

//	public Function<String, Backend<?>> authorityToBackend() {
//		return authorityToBackend;
//	}
	public Backend<?> backend(HttpRequest request) {
		var x = backends.get(config.appResolution().id(request));
		return x != null ? x : this;
	}

	@Override
	protected Class<?> dataType() {
		return Data.class;
	}

	@Override
	protected HttpHandler newHttpHandler() {
		var f = diFactory.newInstance(diFactory.classFor(WebAppHandlerFactory.class));
		return x -> {
			var ba = (Backend<?>) JanillaDomain.WEB_APP.get();
//			IO.println("JanillaBackend.newHttpHandler, ba=" + ba);
			var h = ba == this ? f.createHandler(Objects.requireNonNullElse(x.exception(), x.request()))
					: ba.httpHandler();
			if (h == null)
				throw new NotFoundException(
						x.request().getHeaderValue(":method") + " " + x.request().getHeaderValue(":path"));
			return ScopedValue.where(INSTANCE, ba).call(() -> h.handle(x));
		};
	}
}

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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import com.janilla.frontend.web.Frontend;
import com.janilla.http.HttpHandler;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.janillacom.JanillaDomain;
import com.janilla.java.Java;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.NotFoundException;
import com.janilla.web.WebApp;
import com.janilla.websitetemplate.frontend.WebsiteFrontend;

public class JanillaFrontend extends WebsiteFrontend<JanillaFrontendConfig> {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(WebsiteFrontend.diTypes(), Java.getPackageTypes("com.janilla.janillacom"),
				Java.getPackageTypes("com.janilla.janillacom.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		var c = newConfig(new Class<?>[] { JanillaFrontend.class }, args.length != 0 ? args[0] : null, f);
		var a = f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f));
		serve(a);
	}

	protected final Map<String, Frontend<?>> frontends = new ConcurrentHashMap<>();

	protected final Function<String, Frontend<?>> authorityToFrontend = authority -> {
		IO.println("JanillaFrontend.authorityToFrontend, authority=" + authority);
		var s = "." + config.authority();
		var a = authority.endsWith(s)
				? frontends.computeIfAbsent(authority.substring(0, authority.length() - s.length()), k -> {
					IO.println("JanillaFrontend.authorityToFrontend, k=" + k);
					var a2 = ((JanillaDataFetching) dataFetching).applications(k, null, null, null, null, null)
							.elements().getFirst();
					IO.println("JanillaFrontend.authorityToFrontend, a2=" + a2);
					if (a2 != null)
						try {
							var c = Class.forName(a2.frontend());
							@SuppressWarnings("unchecked")
							var tt = ((Stream<Class<?>>) c.getDeclaredMethod("diTypes").invoke(null)).toList();
							var f = new DefaultDiFactory(tt);
							var c2 = newConfig(Stream.of(toConfigMap(c), (Map<?, ?>) config.frontends().get(a2.slug()))
									.filter(x -> x != null).toArray(Map<?, ?>[]::new), f);
							return (Frontend<?>) f.newInstance(c, Java.hashMap("config", c2, "diFactory", f));
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException(e);
						}
					return this;
				})
				: this;
		IO.println("JanillaFrontend.authorityToFrontend, a=" + a);
		return a;
	};

	public JanillaFrontend(JanillaFrontendConfig config, DiFactory diFactory) {
		super(config, diFactory);
	}

	public Function<String, Frontend<?>> authorityToFrontend() {
		return authorityToFrontend;
	}

	@Override
	protected HttpHandler newHttpHandler() {
		var f = diFactory.newInstance(diFactory.classFor(ApplicationHandlerFactory.class));
		return x -> {
			var a = JanillaDomain.WEB_APP.get();
			var h = a == this ? f.createHandler(Objects.requireNonNullElse(x.exception(), x.request()))
					: a.httpHandler();
			if (h == null)
				throw new NotFoundException(
						x.request().getHeaderValue(":method") + " " + x.request().getHeaderValue(":path"));
			return ScopedValue.where(INSTANCE, a).call(() -> h.handle(x));
		};
	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();
		resourcePrefixes.put("com.janilla.websitetemplate.frontend", "/website");
		resourcePrefixes.put("com.janilla.janillacom.frontend", "");
	}
}

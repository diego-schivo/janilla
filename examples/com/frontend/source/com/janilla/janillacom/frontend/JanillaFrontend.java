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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.blanktemplate.frontend.DownloadResourcesProvider;
import com.janilla.frontend.web.Frontend;
import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.janillacom.Application;
import com.janilla.janillacom.JanillaDomain;
import com.janilla.java.Java;
import com.janilla.web.NotFoundException;
import com.janilla.web.PackageResourcesProvider;
import com.janilla.web.WebApp;
import com.janilla.web.WebAppHandlerFactory;
import com.janilla.websitetemplate.frontend.WebsiteFrontend;

public class JanillaFrontend extends WebsiteFrontend<JanillaFrontendConfig, JanillaDomain> {

	private static final Logger LOGGER = System.getLogger(JanillaFrontend.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(WebsiteFrontend.diTypes(), Java.getPackageTypes("com.janilla.janillacom"),
				Java.getPackageTypes("com.janilla.janillacom.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0]);
		var c = newConfig(new Class<?>[] { JanillaFrontend.class }, args.length != 0 ? args[0] : null, f);
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f, "context",
				(Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x)));
		serve(a[0]);
	}

	protected final Map<String, Frontend<?, ?>> frontends;

	public JanillaFrontend(JanillaFrontendConfig config, DiFactory diFactory, Consumer<Object> context) {
		super(config, diFactory, context, null);

		frontends = config
				.frontends().keySet().stream().map(x -> ((JanillaDataFetching) dataFetching)
						.applications(x, null, null, null, null, null).elements().getFirst())
				.collect(Collectors.toMap(Application::id, a -> {
					try {
						var c = Class.forName(a.frontend());
						@SuppressWarnings("unchecked")
						var tt = ((Stream<Class<?>>) c.getDeclaredMethod("diTypes").invoke(null)).toList();
						var a2 = new WebApp[1];
						var f = Ioc.diFactory(tt, () -> a2[0]);
						var cfg = newConfig(Stream.of(toConfigMap(c), (Map<?, ?>) config.frontends().get(a.id()))
								.filter(x -> x != null).toArray(Map<?, ?>[]::new), f);
						Consumer<Object> ctx = x -> a2[0] = (WebApp<?, ?>) x;
						return (Frontend<?, ?>) f.newInstance(c,
								Java.hashMap("config", cfg, "diFactory", f, "context", ctx, "httpClient", httpClient));
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				}));
	}

	public Frontend<?, ?> frontend(HttpRequest request) {
		var x = frontends.get(config.appResolution().id(request));
		return x != null ? x : this;
	}

	@Override
	protected HttpHandler newHttpHandler() {
		var f = diFactory.newInstance(diFactory.classFor(WebAppHandlerFactory.class));
		return x -> {
			var fa = (Frontend<?, ?>) JanillaDomain.WEB_APP.get();
//			IO.println("JanillaFrontend.newHttpHandler, fa=" + fa);
			var h = fa == this ? f.createHandler(Objects.requireNonNullElse(x.exception(), x.request()))
					: fa.httpHandler();
			if (h == null)
				throw new NotFoundException(
						x.request().getHeaderValue(":method") + " " + x.request().getHeaderValue(":path"));
			return ScopedValue.where(INSTANCE, fa).call(() -> h.handle(x));
		};
	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.websitetemplate.frontend"), "/website");
		resourcesProviders.put(diFactory.newInstance(DownloadResourcesProvider.class, Map.of("url", GEIST_FONT_DOWNLOAD)),
				"/website");
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.janillacom.frontend"), "");
	}
}

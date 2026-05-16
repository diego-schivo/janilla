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
package com.janilla.janillacom.fullstack;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.http.HttpExchange;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.janillacom.JanillaDomain;
import com.janilla.janillacom.backend.JanillaBackend;
import com.janilla.janillacom.frontend.JanillaFrontend;
import com.janilla.java.Java;
import com.janilla.java.SimpleLogger;
import com.janilla.web.WebApp;
import com.janilla.websitetemplate.fullstack.WebsiteFullstack;

public class JanillaFullstack extends WebsiteFullstack<JanillaFullstackConfig, JanillaDomain> {

	public static final Class<?>[] CONFIG_CLASSES = { JanillaBackend.class, JanillaFrontend.class,
			JanillaFullstack.class };

	private static final Logger LOGGER = System.getLogger(JanillaFullstack.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(WebsiteFullstack.diTypes(), Java.getPackageTypes("com.janilla.janillacom.backend"),
				Java.getPackageTypes("com.janilla.janillacom.frontend"),
				Java.getPackageTypes("com.janilla.janillacom.fullstack")).flatMap(x -> x);
	};

	public static void main(String[] args) {
//		SimpleLogger.prefix = () -> {
//			class A {
//				static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
//			}
//			var f = A.WALKER.walk(ff -> ff.dropWhile(x -> !x.getDeclaringClass().equals(System.Logger.class))
//					.dropWhile(x -> x.getDeclaringClass().equals(System.Logger.class)).findFirst().get());
//			return f.getClassName().substring(f.getClassName().lastIndexOf('.') + 1) + "." + f.getMethodName();
//		};
		SimpleLogger.prefix = () -> LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();

		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var r = Runtime.getRuntime();
		LOGGER.log(Level.DEBUG, "maxMemory={0}", r.maxMemory());

		Thread.startVirtualThread(() -> {
			for (;;) {
				try {
					TimeUnit.SECONDS.sleep(5);
				} catch (InterruptedException e) {
					break;
				}
//				IO.println("freeMemory=" + r.freeMemory() + ", maxMemory=" + r.maxMemory() + ", totalMemory="
//						+ r.totalMemory());
				var fm = r.freeMemory();
				var tm = r.totalMemory();
				var um = tm - fm;
				var p = BigDecimal.valueOf(um * 100).divide(BigDecimal.valueOf(tm), RoundingMode.HALF_UP);
//				IO.println(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) + " " + um + "/" + tm + " (" + p + "%)");
				LOGGER.log(Level.DEBUG, "{0}/{1} ({2}%)", um, tm, p);
			}
		});

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0], "fullstack");
		var c = newConfig(CONFIG_CLASSES, args.length != 0 ? args[0] : null, f);
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", c, "diFactory", f, "context",
				(Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x)));
		serve(a[0]);
	}

	public JanillaFullstack(JanillaFullstackConfig config, DiFactory diFactory, Consumer<Object> context) {
		super(config, diFactory, context, JanillaFrontend.class, JanillaBackend.class);
	}

	@Override
	protected Stream<Class<?>> diBackendTypes() {
		return Stream.of(diTypes(backendClass), Java.getPackageTypes("com.janilla.blanktemplate.fullstack"),
				Java.getPackageTypes("com.janilla.websitetemplate.fullstack"),
				Java.getPackageTypes("com.janilla.janillacom.fullstack")).flatMap(x -> x);
	}

	@Override
	protected Stream<Class<?>> diFrontendTypes() {
		return Stream.of(diTypes(frontendClass), Java.getPackageTypes("com.janilla.blanktemplate.fullstack"),
				Java.getPackageTypes("com.janilla.websitetemplate.fullstack"),
				Java.getPackageTypes("com.janilla.janillacom.fullstack")).flatMap(x -> x);
	}

	@Override
	protected WebApp<?, ?> webApp(HttpExchange exchange) {
		var p = exchange.request().getPath();
		var a = p.contains("/api/") ? backend : frontend;
//		IO.println("AbstractFullstack.newHttpHandler, p=" + p + ", a=" + a);
		return a;
	}
}

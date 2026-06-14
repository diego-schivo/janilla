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
package com.janilla.conduit.frontend;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.blanktemplate.frontend.BlankFrontend;
import com.janilla.http.HttpClient;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.java.SimpleLogger;
import com.janilla.conduit.ConduitDomain;
import com.janilla.web.PackageResourcesProvider;
import com.janilla.web.WebApp;

public class ConduitFrontend extends BlankFrontend<ConduitFrontendConfig, ConduitDomain> {

	private static final Logger LOGGER = System.getLogger(ConduitFrontend.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(BlankFrontend.diTypes(), Java.getPackageTypes("com.janilla.conduit"),
				Java.getPackageTypes("com.janilla.conduit.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		SimpleLogger.prefix = () -> {
			interface A {
				StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
			}
			var f = A.WALKER.walk(ff -> ff.dropWhile(x -> !x.getDeclaringClass().equals(System.Logger.class))
					.dropWhile(x -> x.getDeclaringClass().equals(System.Logger.class)).findFirst().get());
			return f.getClassName().substring(f.getClassName().lastIndexOf('.') + 1) + "." + f.getMethodName();
		};

		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0]);
		var cfg = newConfig(new Class<?>[] { ConduitFrontend.class }, args.length != 0 ? args[0] : null, f);
		var ctx = (Consumer<Object>) x -> a[0] = (WebApp<?, ?>) x;
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", cfg, "diFactory", f, "context", ctx));
		serve(a[0]);
	}

	public ConduitFrontend(ConduitFrontendConfig config, DiFactory diFactory, Consumer<Object> context,
			HttpClient httpClient) {
		super(config, diFactory, context, httpClient);
	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();

		resourcesProviders.put(new PackageResourcesProvider("com.janilla.blanktemplate.frontend"), "/blank");
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.conduit.frontend"), "");
	}
}

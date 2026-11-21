/*
 * Copyright 2012-2025 the original author or authors.
 * Copyright 2024-2026 Diego Schivo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.janilla.petclinic.frontend;

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
import com.janilla.petclinic.PetclinicDomain;
import com.janilla.web.PackageResourcesProvider;
import com.janilla.web.WebApp;

/**
 * @author Diego Schivo
 */
public class PetclinicFrontend extends BlankFrontend<PetclinicFrontendConfig, PetclinicDomain> {

	private static final Logger LOGGER = System.getLogger(PetclinicFrontend.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(BlankFrontend.diTypes(), Java.getPackageTypes("com.janilla.petclinic"),
				Java.getPackageTypes("com.janilla.petclinic.frontend")).flatMap(x -> x);
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
		var cfg = newConfig(new Class<?>[] { PetclinicFrontend.class }, args.length != 0 ? args[0] : null, f);
		var ctx = (Consumer<Object>) x -> a[0] = (WebApp<?, ?>) x;
		f.newInstance(f.classFor(WebApp.class), Java.hashMap("config", cfg, "diFactory", f, "context", ctx));
		serve(a[0]);
	}

	public PetclinicFrontend(PetclinicFrontendConfig config, DiFactory diFactory, Consumer<Object> context,
			HttpClient httpClient) {
		super(config, diFactory, context, httpClient);
	}

	@Override
	protected void putResourcePrefixes() {
		super.putResourcePrefixes();

		resourcesProviders.put(new PackageResourcesProvider("com.janilla.blanktemplate.frontend"), "/blank");
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.petclinic.frontend"), "");
	}
}

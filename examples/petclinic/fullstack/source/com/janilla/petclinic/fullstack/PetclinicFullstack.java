/*
 * Copyright 2012-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.janilla.petclinic.fullstack;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.janilla.fullstack.web.AbstractFullstack;
import com.janilla.fullstack.web.FullstackConfig;
import com.janilla.ioc.DiFactory;
import com.janilla.ioc.Ioc;
import com.janilla.java.Java;
import com.janilla.petclinic.backend.PetclinicBackend;
import com.janilla.petclinic.frontend.PetclinicFrontend;
import com.janilla.web.Domain;
import com.janilla.web.WebApp;

/**
 * @author Diego Schivo
 * @author Dave Syer
 */
public class PetclinicFullstack extends AbstractFullstack<FullstackConfig, Domain> {

	private static final Logger LOGGER = System.getLogger(PetclinicFullstack.class.getName());

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.java"), Java.getPackageTypes("com.janilla.web"),
				Java.getPackageTypes("com.janilla.backend", x -> !x.endsWith(".cms")),
				Java.getPackageTypes("com.janilla.frontend", _ -> true),
				Java.getPackageTypes("com.janilla.fullstack", _ -> true),
				Java.getPackageTypes("com.janilla.petclinic.fullstack")).flatMap(x -> x);
	}

	public static void main(String[] args) {
		LOGGER.log(Level.DEBUG, "pid={0}", String.valueOf(ProcessHandle.current().pid()));

		var a = new WebApp[1];
		var f = Ioc.diFactory(diTypes().toList(), () -> a[0], "fullstack");
		var c = newConfig(new Class<?>[] { PetclinicBackend.class, PetclinicFrontend.class, PetclinicFullstack.class },
				args.length != 0 ? args[0] : null, f);
		f.newInstance(f.classFor(WebApp.class),
				Java.hashMap("config", c, "diFactory", f, "context", (Consumer<Object>) (x -> a[0] = (WebApp<?, ?>) x)));
		serve(a[0]);
	}

	public PetclinicFullstack(FullstackConfig config, DiFactory diFactory, Consumer<Object> context) {
		super(config, diFactory, context, PetclinicFrontend.class, PetclinicBackend.class);
	}
}

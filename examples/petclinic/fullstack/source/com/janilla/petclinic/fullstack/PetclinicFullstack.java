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

import java.nio.file.Path;
import java.util.stream.Stream;

import com.janilla.fullstack.web.AbstractFullstack;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.petclinic.backend.PetclinicBackend;
import com.janilla.petclinic.frontend.PetclinicFrontend;

/**
 * @author Diego Schivo
 * @author Dave Syer
 */
public class PetclinicFullstack extends AbstractFullstack {

	public static Stream<Class<?>> diTypes() {
		return Java.getPackageTypes("com.janilla.petclinic.fullstack");
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList(), "fullstack");
		serve(f, args.length > 0 ? args[0] : null, "petclinic");
	}

	public PetclinicFullstack(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey, PetclinicFrontend.class, PetclinicBackend.class);
	}
}

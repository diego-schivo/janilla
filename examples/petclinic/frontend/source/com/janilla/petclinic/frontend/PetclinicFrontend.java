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
package com.janilla.petclinic.frontend;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import com.janilla.frontend.web.AbstractFrontend;
import com.janilla.http.HttpClient;
import com.janilla.ioc.DefaultDiFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.petclinic.OwnerApi;
import com.janilla.petclinic.PetApi;
import com.janilla.petclinic.PetTypeApi;
import com.janilla.petclinic.VetApi;
import com.janilla.petclinic.VisitApi;
import com.janilla.web.InvocationResolver;

/**
 * @author Diego Schivo
 * @author Dave Syer
 */
public class PetclinicFrontend extends AbstractFrontend {

	public static Stream<Class<?>> diTypes() {
		return Stream.of(Java.getPackageTypes("com.janilla.http"), Java.getPackageTypes("com.janilla.web"),
				Java.getPackageTypes("com.janilla.frontend", _ -> true), Java.getPackageTypes("com.janilla.petclinic"),
				Java.getPackageTypes("com.janilla.petclinic.frontend")).flatMap(x -> x);
	};

	public static void main(String[] args) {
		IO.println(ProcessHandle.current().pid());

		var f = new DefaultDiFactory(diTypes().toList());
		serve(f, args.length > 0 ? args[0] : null, "petclinic");
	}

	protected HttpClient httpClient;

	protected OwnerApi ownerApi;

	protected PetApi petApi;

	protected PetTypeApi petTypeApi;

	protected VetApi vetApi;

	protected VisitApi visitApi;

	public PetclinicFrontend(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey);
	}

	public HttpClient httpClient() {
		return httpClient;
	}

	public OwnerApi ownerApi() {
		return ownerApi;
	}

	public PetApi petApi() {
		return petApi;
	}

	public PetTypeApi petTypeApi() {
		return petTypeApi;
	}

	public VetApi vetApi() {
		return vetApi;
	}

	public VisitApi visitApi() {
		return visitApi;
	}

	@Override
	protected InvocationResolver newInvocationResolver() {
		httpClient = diFactory.newInstance(diFactory.classFor(HttpClient.class));
		ownerApi = diFactory.newInstance(diFactory.classFor(OwnerApi.class));
		petApi = diFactory.newInstance(diFactory.classFor(PetApi.class));
		petTypeApi = diFactory.newInstance(diFactory.classFor(PetTypeApi.class));
		vetApi = diFactory.newInstance(diFactory.classFor(VetApi.class));
		visitApi = diFactory.newInstance(diFactory.classFor(VisitApi.class));

		return super.newInvocationResolver();
	}

	@Override
	protected void putResourcePrefixes(Map<String, String> prefixes) {
		super.putResourcePrefixes(prefixes);
		prefixes.put("com.janilla.petclinic.frontend", "");
	}
}

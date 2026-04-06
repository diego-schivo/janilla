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

import java.net.URI;
import java.util.List;

import com.janilla.petclinic.Vet;
import com.janilla.petclinic.VetApi;
import com.janilla.web.Bind;
import com.janilla.web.Handle;

/**
 * @author Diego Schivo
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Handle(path = "/vets")
public class VetController {

	protected final VetApi vetApi;

	public VetController(VetApi vetApi) {
		this.vetApi = vetApi;
	}

	@Handle(method = "GET")
	public List<Vet> find() {
		return vetApi.read(1, null, null).elements();
	}

	@Handle(method = "GET", path = "/vets.html")
	public VetList find(@Bind("page") Integer page) {
		var p = page != null ? page : 1;
		var lp = vetApi.read(1, (p - 1) * 5, 5);
		var rr = lp.elements().stream().map(x -> new VetList.Result(x, x.specialties())).toList();
		var p2 = new Paginator(p - 1, (int) Math.ceilDiv(lp.totalSize(), 5), URI.create("/vets.html"));
		return new VetList(rr, p2);
	}
}

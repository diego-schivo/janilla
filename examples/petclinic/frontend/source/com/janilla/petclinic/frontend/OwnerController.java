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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.janilla.petclinic.Owner;
import com.janilla.petclinic.OwnerApi;
import com.janilla.web.Handle;

/**
 * @author Diego Schivo
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@Handle(path = "/owners")
public class OwnerController {

	protected static final Pattern TEN_DIGITS = Pattern.compile("\\d{10}");

	protected final OwnerApi ownerApi;

	public OwnerController(OwnerApi ownerApi) {
		this.ownerApi = ownerApi;
	}

	@Handle(method = "GET", path = "find")
	public Object initFind() {
		return new FindOwners(null, null);
	}

	@Handle(method = "GET")
	public Object find(String lastName, Integer page) {
//		IO.println("OwnerController.find, lastName=" + lastName + ", page=" + page);
		var p = page != null ? page : 1;
		var oo = ownerApi.read(lastName, 1, (p - 1) * 5, 5);
		return switch ((int) oo.totalSize()) {
		case 0 -> new FindOwners(lastName, Map.of("lastName", List.of("has not been found")));
		case 1 -> URI.create("/owners/" + oo.elements().getFirst().id());
		default -> OwnersList.of(oo, p);
		};
	}

	@Handle(method = "GET", path = "(\\d+)")
	public OwnerDetails show(Long id) {
		var o = ownerApi.read(id, 2);
		return OwnerDetails.of(o);
	}

	@Handle(method = "GET", path = "new")
	public OwnerForm initCreate() {
		return new OwnerForm(null, null);
	}

	@Handle(method = "POST", path = "new")
	public Object create(Owner owner) {
		var ee = validate(owner);
		if (!ee.isEmpty())
			return new OwnerForm(owner, ee);

		var o = ownerApi.create(owner);
		return URI.create("/owners/" + o.id());
	}

	@Handle(method = "GET", path = "(\\d+)/edit")
	public OwnerForm initUpdate(Long id) {
		var o = ownerApi.read(id, 0);
		return new OwnerForm(o, null);
	}

	@Handle(method = "POST", path = "(\\d+)/edit")
	public Object update(Long id, Owner owner) {
//		IO.println("OwnerController.update, id=" + id + ", owner=" + owner);
		var ee = validate(owner);
		if (!ee.isEmpty())
			return new OwnerForm(owner, ee);

		var o = ownerApi.update(id, owner);
		return URI.create("/owners/" + o.id());
	}

	protected Map<String, List<String>> validate(Owner owner) {
		var m = new LinkedHashMap<String, List<String>>();
		if (owner.firstName() == null || owner.firstName().isBlank())
			m.computeIfAbsent("firstName", _ -> new ArrayList<>()).add("must not be blank");
		if (owner.lastName() == null || owner.lastName().isBlank())
			m.computeIfAbsent("lastName", _ -> new ArrayList<>()).add("must not be blank");
		if (owner.address() == null || owner.address().isBlank())
			m.computeIfAbsent("address", _ -> new ArrayList<>()).add("must not be blank");
		if (owner.city() == null || owner.city().isBlank())
			m.computeIfAbsent("city", _ -> new ArrayList<>()).add("must not be blank");
		if (owner.telephone() == null || owner.telephone().isBlank())
			m.computeIfAbsent("telephone", _ -> new ArrayList<>()).add("must not be blank");
		if (owner.telephone() == null || !TEN_DIGITS.matcher(owner.telephone()).matches())
			m.computeIfAbsent("telephone", _ -> new ArrayList<>())
					.add("numeric value out of bounds (<10 digits>.<0 digits> expected)");
		return m;
	}

	protected static boolean startsWithIgnoreCase(String string, String prefix) {
		return string == prefix || (prefix != null && prefix.length() <= string.length()
				&& string.regionMatches(true, 0, prefix, 0, prefix.length()));
	}
}

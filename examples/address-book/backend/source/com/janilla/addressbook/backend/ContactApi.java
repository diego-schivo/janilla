/*
 * MIT License
 *
 * Copyright (c) React Training LLC 2015-2019
 * Copyright (c) Remix Software Inc. 2020-2021
 * Copyright (c) Shopify Inc. 2022-2023
 * Copyright (c) Diego Schivo 2024-2026
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
package com.janilla.addressbook.backend;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.janilla.backend.persistence.Persistence;
import com.janilla.java.JavaReflect;
import com.janilla.web.Bind;
import com.janilla.web.Handle;
import com.janilla.web.NotFoundException;

@Handle(path = "/api/contacts")
class ContactApi {

	protected final Persistence persistence;

	public ContactApi(Persistence persistence) {
		this.persistence = persistence;
	}

	@Handle(method = "GET")
	public List<Contact> list(@Bind("query") String query) {
//		IO.println("ContactApi.list, query=" + query);
		var c = persistence.crud(Contact.class);
		var q = query != null && !query.isEmpty() ? query.toLowerCase().toCharArray() : null;
		return c.read(q != null ? c.filter("full", x -> Arrays.stream(((String) x).split(" ")).anyMatch(y -> {
			var s = y.toLowerCase();
			var i = -1;
			for (var z : q) {
				i = s.indexOf(z, i + 1);
				if (i == -1)
					return false;
			}
			return true;
		})) : c.list());
	}

	@Handle(method = "POST")
	public Contact create(Contact contact) {
		var x = contact;
		var l = 1L + Integer.MAX_VALUE + ThreadLocalRandom.current().nextLong(Long.MAX_VALUE - Integer.MAX_VALUE);
		var s = Long.toString(l, 36);
		s = s.length() > 7 ? s.substring(s.length() - 7, s.length()) : s;
		x = x.withId(s).withCreatedAt(Instant.now());
		return persistence.crud(Contact.class).create(x);
	}

	@Handle(method = "GET", path = "([^/]+)")
	public Contact read(String id) {
		var x = persistence.crud(Contact.class).read(id);
		if (x == null)
			throw new NotFoundException("contact " + id);
		return x;
	}

	@Handle(method = "PUT", path = "([^/]+)")
	public Contact update(String id, Contact contact) {
		var x = persistence.crud(Contact.class).update(id,
				y -> JavaReflect.copy(contact, y, z -> !Set.of("id", "createdAt").contains(z)));
		if (x == null)
			throw new NotFoundException("contact " + id);
		return x;
	}

	@Handle(method = "DELETE", path = "([^/]+)")
	public Contact delete(String id) {
		var x = persistence.crud(Contact.class).delete(id);
		if (x == null)
			throw new NotFoundException("contact " + id);
		return x;
	}

	@Handle(method = "PUT", path = "([^/]+)/favorite")
	public Contact favorite(String id, Boolean value) {
		var x = persistence.crud(Contact.class).update(id, y -> y.withFavorite(value));
		if (x == null)
			throw new NotFoundException("contact " + id);
		return x;
	}
}

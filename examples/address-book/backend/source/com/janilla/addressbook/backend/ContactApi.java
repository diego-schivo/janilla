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

import java.util.Arrays;
import java.util.function.Predicate;

import com.janilla.addressbook.Contact;
import com.janilla.backend.cms.AbstractCollectionApi;
import com.janilla.backend.persistence.Persistence;
import com.janilla.http.HttpExchange;
import com.janilla.java.Copier;
import com.janilla.java.Direction;
import com.janilla.persistence.ListPortion;
import com.janilla.web.Handle;

@Handle(path = "/api/contacts")
class ContactApi extends AbstractCollectionApi<String, Contact> {

	public ContactApi(Predicate<HttpExchange> drafts, Persistence persistence, Copier copier) {
		super(Contact.class, drafts, persistence, "full", copier, Direction.FORWARD, 0);
	}

	@Override
	public ListPortion<Contact> read(String search, Direction direction, Long skip, Long limit, Integer depth) {
		var k = skip != null ? skip.longValue() : 0;
		var l = limit != null ? limit.longValue() : -1;
		var d = depth != null ? depth : 0;

		var cc = search != null && !search.isBlank() ? search.strip().toLowerCase().toCharArray() : null;
		var ii = cc != null
				? crud().filterAndCount(searchIndex, x -> Arrays.stream(((String) x).split(" ")).anyMatch(s -> {
					s = s.toLowerCase();
					var i = -1;
					for (var c : cc) {
						i = s.indexOf(c, i + 1);
						if (i == -1)
							return false;
					}
					return true;
				}), direction, k, l)
				: crud().filterAndCount(searchIndex, new Object[0], direction, k, l);
		return ii.map(x -> crud().read(x, d));
	}
}

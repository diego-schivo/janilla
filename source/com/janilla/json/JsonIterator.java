/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.json;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class JsonIterator extends TokenIterationContext implements Iterator<JsonToken<?>> {

	protected Object object;

	int state;

	JsonToken<?> token;

	Iterator<JsonToken<?>> iterator;

	public void setObject(Object object) {
		this.object = object;
	}

	@Override
	public boolean hasNext() {
		while (token == null && (iterator == null || !iterator.hasNext()) && state != 3) {
			var s = state;
			state = switch (s) {
			case 0 -> {
				token = JsonToken.JSON_START;
				yield 1;
			}
			case 1 -> {
				if (iterator == null)
					iterator = buildValueIterator(object);
				if (iterator.hasNext())
					yield 1;
				yield 2;
			}
			case 2 -> {
				token = JsonToken.JSON_END;
				yield 3;
			}
			default -> s;
			};

//			System.out.println("JsonIterator.hasNext " + s + " -> " + state);

		}
		return token != null || (iterator != null && iterator.hasNext());
	}

	@Override
	public JsonToken<?> next() {
		if (hasNext()) {
			var t = token;
			if (t != null) {
				token = null;
				return t;
			}
			t = iterator.next();
			return t;
		} else
			throw new NoSuchElementException();
	}
}

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

	protected final Object object;

	private Boolean hasNext;

	private int state;

	private JsonToken<?> token;

	private Iterator<JsonToken<?>> iterator;

	public JsonIterator(Object object) {
		this.object = object;
	}

	@Override
	public boolean hasNext() {
		if (hasNext == null) {
			while (token == null && (iterator == null || !iterator.hasNext()) && state != 3) {
				var s = state;
				state = switch (s) {
				case 0 -> {
					stack().push(object);
					token = JsonToken.JSON_START;
					yield 1;
				}
				case 1 -> {
					if (iterator == null)
						iterator = newValueIterator(object);
					if (iterator.hasNext())
						yield 1;
					yield 2;
				}
				case 2 -> {
					stack().pop();
					token = JsonToken.JSON_END;
					yield 3;
				}
				default -> s;
				};
//			IO.println("JsonIterator.hasNext, " + s + " -> " + state);
			}
			hasNext = token != null || (iterator != null && iterator.hasNext());
		}
		return hasNext;
	}

	@Override
	public JsonToken<?> next() {
		if (!hasNext())
			throw new NoSuchElementException();
		var x = token;
		if (x != null)
			token = null;
		else
			x = iterator.next();
		hasNext = null;
		return x;
	}
}

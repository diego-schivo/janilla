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

public class ArrayIterator extends TokenIterator {

	protected final Iterator<?> elements;

	private int state;

	private JsonToken<?> token;

	private Object element;

	private Iterator<JsonToken<?>> iterator;

	public ArrayIterator(TokenIterationContext context, Iterator<?> elements) {
		super(context);
		this.elements = elements;
	}

	@Override
	protected boolean computeHasNext() {
		while (token == null && (iterator == null || !iterator.hasNext()) && state != 4) {
			var s = state;
			state = switch (s) {
			case 0 -> {
				token = JsonToken.ARRAY_START;
				yield 1;
			}
			case 1 -> {
				if (elements.hasNext()) {
					element = elements.next();
					context.stack().push(element);
					token = JsonToken.ELEMENT_START;
					yield 2;
				}
				token = JsonToken.ARRAY_END;
				yield 4;
			}
			case 2 -> {
				if (iterator == null)
					iterator = context.newValueIterator(element);
				if (iterator.hasNext())
					yield 2;
				iterator = null;
				yield 3;
			}
			case 3 -> {
				context.stack().pop();
				token = JsonToken.ELEMENT_END;
				yield 1;
			}
			default -> s;
			};
//			System.out.println("ArrayIterator.hasNext, " + s + " -> " + state);
		}
		return token != null || (iterator != null && iterator.hasNext());
	}

	@Override
	protected JsonToken<?> computeNext() {
		var t = token != null ? token : iterator.next();
		token = null;
		return t;
	}
}

/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

class ValueIterator extends TokenIterator {

	Object object;

	int state;

	JsonToken<?> token;

	Iterator<JsonToken<?>> iterator;

	ValueIterator(Object object, TokenIterationContext context) {
		super(context);
		this.object = object;
	}

	@Override
	public boolean hasNext() {
		while (token == null && (iterator == null || !iterator.hasNext()) && state != 3) {
			var s = state;
			state = switch (s) {
			case 0 -> {
				token = JsonToken.VALUE_START;
				yield 1;
			}
			case 1 -> {
				if (iterator == null)
					iterator = newIterator(object);
				if (iterator.hasNext())
					yield 1;
				yield 2;
			}
			case 2 -> {
				token = JsonToken.VALUE_END;
				yield 3;
			}
			default -> s;
			};

//			System.out.println("ValueIterator.hasNext " + s + " -> " + state);

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

	protected Iterator<JsonToken<?>> newIterator(Object object) {
		return object == null ? new NullIterator() : switch (object) {
		case Map<?, ?> m -> {
			@SuppressWarnings("unchecked")
			var n = (Map<String, Object>) m;
			yield new ObjectIterator(n.entrySet().iterator(), context);
		}
		case List<?> l -> {
			@SuppressWarnings("unchecked")
			var m = (List<Object>) l;
			yield new ArrayIterator(m.iterator(), context);
		}
		case Boolean b -> new BooleanIterator(b);
		case Instant i -> new StringIterator(i.toString());
		case Number n -> new NumberIterator(n);
		case String s -> new StringIterator(s);
		default -> null;
		};
	}
}

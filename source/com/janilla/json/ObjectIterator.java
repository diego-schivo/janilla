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
import java.util.Map;
import java.util.Map.Entry;

public class ObjectIterator extends TokenIterator {

	protected final Iterator<Map.Entry<String, Object>> entries;

	private int state;

	private JsonToken<?> token;

	private Map.Entry<String, Object> entry;

	private Iterator<JsonToken<?>> iterator;

	public ObjectIterator(TokenIterationContext context, Iterator<Entry<String, Object>> entries) {
		super(context);
		this.entries = entries;
	}

	@Override
	protected boolean computeHasNext() {
		while (token == null && (iterator == null || !iterator.hasNext()) && state != 5) {
			var s = state;
			state = switch (s) {
			case 0 -> {
				token = JsonToken.OBJECT_START;
				yield 1;
			}
			case 1 -> {
				if (entries.hasNext()) {
					entry = entries.next();
//					System.out.println("ObjectIterator.computeHasNext, entry=" + entry);
					context.stack().push(entry);
					token = JsonToken.MEMBER_START;
					yield 2;
				}
				token = JsonToken.OBJECT_END;
				yield 5;
			}
			case 2 -> {
				if (iterator == null)
					iterator = context.newStringIterator(entry.getKey());
				if (iterator.hasNext())
					yield s;
				iterator = null;
				yield 3;
			}
			case 3 -> {
				if (iterator == null)
					iterator = context.newValueIterator(entry.getValue());
				if (iterator.hasNext())
					yield s;
				iterator = null;
				yield 4;
			}
			case 4 -> {
				context.stack().pop();
				token = JsonToken.MEMBER_END;
				yield 1;
			}
			default -> s;
			};
//			System.out.println("ObjectIterator.hasNext, " + s + " -> " + state);
		}
		return token != null || (iterator != null && iterator.hasNext());
	}

	@Override
	protected JsonToken<?> computeNext() {
		var x = token;
		if (x != null)
			token = null;
		else
			x = iterator.next();
		return x;
	}
}

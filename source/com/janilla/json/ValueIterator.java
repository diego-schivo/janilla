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

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ValueIterator extends TokenIterator {

	protected final Object value;

	private int state;

	private JsonToken<?> token;

	private Iterator<JsonToken<?>> iterator;

	public ValueIterator(TokenIterationContext context, Object value) {
		super(context);
		this.value = value;
	}

	@Override
	protected boolean computeHasNext() {
		while (token == null && (iterator == null || !iterator.hasNext()) && state != 3) {
			var s = state;
			state = switch (s) {
			case 0 -> {
				context.stack().push(value);
				token = JsonToken.VALUE_START;
				yield 1;
			}
			case 1 -> {
				if (iterator == null)
					iterator = newIterator();
				if (iterator.hasNext())
					yield s;
				yield 2;
			}
			case 2 -> {
				context.stack().pop();
				token = JsonToken.VALUE_END;
				yield 3;
			}
			default -> s;
			};
//			IO.println("ValueIterator.hasNext, " + s + " -> " + state);
		}
		return token != null || (iterator != null && iterator.hasNext());
	}

	@Override
	protected JsonToken<?> computeNext() {
		var t = token != null ? token : iterator.next();
		token = null;
		return t;
	}

	protected Iterator<JsonToken<?>> newIterator() {
//		IO.println("ValueIterator.newIterator, value=" + value);
		return value != null ? switch (value) {
		case Boolean x -> context.newBooleanIterator(x);
		case Date x -> context.newStringIterator(x.toString());
		case Instant x -> context.newStringIterator(x.toString());
		case List<?> x -> {
			@SuppressWarnings("unchecked")
			var l = (List<Object>) x;
			yield context.newArrayIterator(l.iterator());
		}
		case Locale x -> context.newStringIterator(x.toLanguageTag());
		case LocalDate x -> context.newStringIterator(x.toString());
		case LocalDateTime x -> context.newStringIterator(x.toString());
		case Map<?, ?> x -> context.newObjectIterator(x.entrySet().stream().map(y -> {
			@SuppressWarnings("unchecked")
			var z = y.getKey() instanceof Locale l
					? new AbstractMap.SimpleImmutableEntry<String, Object>(l.toLanguageTag(), y.getValue())
					: (Map.Entry<String, Object>) y;
			return z;
		}).iterator());
		case Number x -> context.newNumberIterator(x);
		case OffsetDateTime x -> context.newStringIterator(x.toString());
		case Path x -> context.newStringIterator(x.toString().replace(File.separatorChar, '/'));
		case String x -> context.newStringIterator(x);
		case URI x -> context.newStringIterator(x.toString());
		case UUID x -> context.newStringIterator(x.toString());
		case Class<?> x -> context.newStringIterator(x.toString());
		default -> null;
		} : context.newNullIterator();
	}
}

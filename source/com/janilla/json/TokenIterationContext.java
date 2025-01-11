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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

public abstract class TokenIterationContext {

	private Deque<Object> stack = new ArrayDeque<>();

	public Deque<Object> stack() {
		return stack;
	}

	public Iterator<JsonToken<?>> newArrayIterator(Iterator<?> elements) {
		var tt = new ArrayIterator();
		tt.setContext(this);
		tt.setElements(elements);
		return tt;
	}

	public Iterator<JsonToken<?>> newBooleanIterator(Boolean value) {
		var tt = new BooleanIterator();
		tt.setValue(value);
		return tt;
	}

	public Iterator<JsonToken<?>> newNullIterator() {
		return new NullIterator();
	}

	public Iterator<JsonToken<?>> newNumberIterator(Number number) {
		var tt = new NumberIterator();
		tt.setNumber(number);
		return tt;
	}

	public Iterator<JsonToken<?>> newObjectIterator(Iterator<Map.Entry<String, Object>> entries) {
		var tt = new ObjectIterator();
		tt.setContext(this);
		tt.setEntries(entries);
		return tt;
	}

	public Iterator<JsonToken<?>> newStringIterator(String string) {
		var tt = new StringIterator();
		tt.setString(string);
		return tt;
	}

	public Iterator<JsonToken<?>> newValueIterator(Object object) {
		var tt = new ValueIterator();
		tt.setContext(this);
		tt.setObject(object);
		return tt;
	}
}

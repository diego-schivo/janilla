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

	public Deque<Object> getStack() {
		return stack;
	}

	public Iterator<JsonToken<?>> buildArrayIterator(Iterator<?> elements) {
		var i = new ArrayIterator();
		i.setContext(this);
		i.setElements(elements);
		return i;
	}

	public Iterator<JsonToken<?>> buildBooleanIterator(Boolean value) {
		var i = new BooleanIterator();
		i.setValue(value);
		return i;
	}

	public Iterator<JsonToken<?>> buildNullIterator() {
		return new NullIterator();
	}

	public Iterator<JsonToken<?>> buildNumberIterator(Number number) {
		var i = new NumberIterator();
		i.setNumber(number);
		return i;
	}

	public Iterator<JsonToken<?>> buildObjectIterator(Iterator<Map.Entry<String, Object>> entries) {
		var i = new ObjectIterator();
		i.setContext(this);
		i.setEntries(entries);
		return i;
	}

	public Iterator<JsonToken<?>> buildStringIterator(String string) {
		var i = new StringIterator();
		i.setString(string);
		return i;
	}

	public Iterator<JsonToken<?>> buildValueIterator(Object object) {
		var i = new ValueIterator();
		i.setContext(this);
		i.setObject(object);
		return i;
	}
}

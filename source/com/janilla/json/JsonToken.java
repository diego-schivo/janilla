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

public record JsonToken<T>(Type type, T data) {

	final static JsonToken<Boundary> JSON_START = new JsonToken<>(Type.JSON, Boundary.START);
	final static JsonToken<Boundary> JSON_END = new JsonToken<>(Type.JSON, Boundary.END);
	final static JsonToken<Boundary> OBJECT_START = new JsonToken<>(Type.OBJECT, Boundary.START);
	final static JsonToken<Boundary> OBJECT_END = new JsonToken<>(Type.OBJECT, Boundary.END);
	final static JsonToken<Boundary> ARRAY_START = new JsonToken<>(Type.ARRAY, Boundary.START);
	final static JsonToken<Boundary> ARRAY_END = new JsonToken<>(Type.ARRAY, Boundary.END);
	final static JsonToken<Boundary> MEMBER_START = new JsonToken<>(Type.MEMBER, Boundary.START);
	final static JsonToken<Boundary> MEMBER_END = new JsonToken<>(Type.MEMBER, Boundary.END);
	final static JsonToken<Boundary> ELEMENT_START = new JsonToken<>(Type.ELEMENT, Boundary.START);
	final static JsonToken<Boundary> ELEMENT_END = new JsonToken<>(Type.ELEMENT, Boundary.END);
	final static JsonToken<Boundary> VALUE_START = new JsonToken<>(Type.VALUE, Boundary.START);
	final static JsonToken<Boundary> VALUE_END = new JsonToken<>(Type.VALUE, Boundary.END);
	final static JsonToken<Object> NULL = new JsonToken<>(Type.NULL, null);

	public enum Type {
		JSON, OBJECT, ARRAY, MEMBER, ELEMENT, VALUE, STRING, NUMBER, BOOLEAN, NULL
	}

	public enum Boundary {
		START, END
	}
}

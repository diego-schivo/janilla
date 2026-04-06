/*
 * MIT License
 *
 * Copyright (c) 2024 Vercel, Inc.
 * Copyright (c) 2024-2026 Diego Schivo
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
package com.janilla.acmedashboard.backend;

import java.util.Iterator;
import java.util.Map;

import com.janilla.java.TypeResolver;
import com.janilla.json.JsonToken;
import com.janilla.json.ReflectionJsonIterator;

public class CustomReflectionJsonIterator extends ReflectionJsonIterator {

	public CustomReflectionJsonIterator(Object object) {
		super(object);
	}

//	public CustomReflectionJsonIterator(Object object, boolean includeType) {
//		super(object, includeType);
//	}
	public CustomReflectionJsonIterator(Object object, TypeResolver typeResolver) {
		super(object, typeResolver);
	}

	@Override
	public Iterator<JsonToken<?>> newValueIterator(Object object) {
		if (stack().peek() instanceof Map.Entry x && x.getKey().equals("password"))
			object = "**********";
		return super.newValueIterator(object);
	}
}

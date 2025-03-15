/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
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
package com.janilla.cms;

import java.util.Set;

import com.janilla.json.MapAndType;
import com.janilla.persistence.Crud;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;
import com.janilla.web.Bind;
import com.janilla.web.Handle;
import com.janilla.web.NotFoundException;

public abstract class GlobalApi<E> {

	protected final Class<E> type;

	public Persistence persistence;

	protected GlobalApi(Class<E> type) {
		this.type = type;
	}

	@Handle(method = "GET")
	public E read() {
		var e = crud().read(1);
		if (e == null)
			throw new NotFoundException("entity " + 1);
		return e;
	}

	@Handle(method = "PUT")
	public E update(@Bind(resolver = MapAndType.DollarTypeResolver.class) E entity) {
		var e = crud().update(1, x -> Reflection.copy(entity, x, y -> !Set.of("id").contains(y)));
		if (e == null)
			throw new NotFoundException("entity " + 1);
		return e;
	}

	protected Crud<E> crud() {
		return persistence.crud(type);
	}
}

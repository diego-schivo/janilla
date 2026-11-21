/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
package com.janilla.persistence;

import java.util.List;
import java.util.function.Function;

public record ListPortion<E>(List<E> elements, long totalSize) {

	private static ListPortion<?> EMPTY = new ListPortion<>(List.of(), 0);

	@SuppressWarnings("unchecked")
	public static <E> ListPortion<E> empty() {
		return (ListPortion<E>) EMPTY;
	}

	public static <E> ListPortion<E> of(List<E> list) {
		return of(list, 0, -1);
	}

	public static <E> ListPortion<E> of(List<E> list, long skip, long limit) {
		var s = list.stream();
		if (skip > 0)
			s = s.skip(skip);
		if (limit >= 0)
			s = s.limit(limit);
		return new ListPortion<>(s.toList(), list.size());
	}

	public <F> ListPortion<F> map(Function<E, F> mapper) {
		return new ListPortion<>(elements.stream().map(mapper).toList(), totalSize);
	}
}

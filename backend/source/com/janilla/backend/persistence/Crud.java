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
package com.janilla.backend.persistence;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import com.janilla.persistence.Entity;
import com.janilla.persistence.ListPortion;

public interface Crud<ID extends Comparable<ID>, E extends Entity<ID>> {

	Class<E> type();

	List<CrudObserver<E>> observers();

	E create(E entity);

	default E read(ID id) {
		return read(id, 0);
	}

	E read(ID id, int depth);

	default List<E> read(List<ID> ids) {
		return read(ids, 0);
	}

	List<E> read(List<ID> ids, int depth);

	E update(ID id, UnaryOperator<E> operator);

	E delete(ID id);

	List<E> delete(List<ID> ids);

	default List<ID> list() {
		return list(false);
	}

	List<ID> list(boolean reverse);

	long count();

	ListPortion<ID> listAndCount(boolean reverse, long skip, long limit);

	default ID find(String index, Object[] keys) {
		return find(index, keys, false);
	}

	default ID find(String index, Object[] keys, boolean reverse) {
		var ii = filter(index, keys, reverse, 0, 1);
		return !ii.isEmpty() ? ii.getFirst() : null;
	}

	default List<ID> filter(String index, Object[] keys) {
		return filter(index, keys, false);
	}

	default List<ID> filter(String index, Object[] keys, boolean reverse) {
		return filter(index, keys, reverse, 0, -1);
	}

	List<ID> filter(String index, Object[] keys, boolean reverse, long skip, long limit);

	long count(String index, Object[] keys);

	ListPortion<ID> filterAndCount(String index, Object[] keys, boolean reverse, long skip, long limit);

	default List<ID> filter(String index, Predicate<Object> operation) {
		return filter(index, operation, false);
	}

	List<ID> filter(String index, Predicate<Object> operation, boolean reverse);

	ListPortion<ID> filterAndCount(String index, Predicate<Object> operation, boolean reverse, long skip, long limit);

	ListPortion<ID> filterAndCount(Map<String, Object[]> keys, boolean reverse, long skip, long limit);
}

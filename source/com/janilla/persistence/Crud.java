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
package com.janilla.persistence;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.LongStream;

import com.janilla.database.Database;
import com.janilla.io.IO;
import com.janilla.persistence.Persistence.IndexEntryGetter;
import com.janilla.reflect.Reflection;

public class Crud<E> {

	protected Class<E> type;

	protected Database database;

	protected Function<E, Object> formatter;

	protected Function<Object, E> parser;

	protected Map<String, IndexEntryGetter> indexEntryGetters = new HashMap<>();

	public long create(E entity) throws IOException {
		var i = new long[1];
		database.performOnStore(type.getSimpleName(), s -> i[0] = s.create(j -> {
			try {
				Reflection.setter(type, "id").invoke(entity, j);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			var t = formatter.apply(entity);
//			System.out.println("Crud.create t=" + t);
			return t;
		}));
		for (var e : getIndexMap(entity, i[0]).entrySet()) {
			var n = e.getKey();
			var f = e.getValue();
			var m = toMap(f);
			updateIndex(n, null, m);
		}
		return i[0];
	}

	public E read(long id) throws IOException {
		var o = new Object[1];
		database.performOnStore(type.getSimpleName(), s -> o[0] = s.read(id));
		return parser.apply(o[0]);
	}

	public E update(long id, Consumer<E> consumer) throws IOException {
		class A {

			E e;

			Map<String, Entry<Object, Object>> m;
		}
		var a = new A();
		database.performOnStore(type.getSimpleName(), s -> s.update(id, t -> {
			a.e = parser.apply(t);
			a.m = getIndexMap(a.e, id);
			consumer.accept(a.e);
			return formatter.apply(a.e);
		}));
		if (a.e != null)
			for (var e : getIndexMap(a.e, id).entrySet()) {
				var f1 = a.m.get(e.getKey());
				var f2 = e.getValue();
				var k1 = f1 != null ? f1.getKey() : null;
				var k2 = f2 != null ? f2.getKey() : null;
				if (!Objects.equals(k1, k2)) {
					var m1 = k2 instanceof Collection<?> c ? toMap(f1, k -> !c.contains(k)) : toMap(f1);
					var m2 = k1 instanceof Collection<?> c ? toMap(f2, k -> !c.contains(k)) : toMap(f2);
					updateIndex(e.getKey(), m1, m2);
				}
			}
		return a.e;
	}

	public E delete(long id) throws IOException {
		var o = new Object[1];
		database.performOnStore(type.getSimpleName(), s -> o[0] = s.delete(id));
		var e = parser.apply(o[0]);
		for (var f : getIndexMap(e, id).entrySet()) {
			var m = toMap(f.getValue());
			updateIndex(f.getKey(), m, null);
		}
		return e;
	}

	public long count() throws IOException {
		class A {

			long c;
		}
		var a = new A();
		database.performOnStore(type.getSimpleName(), s -> a.c = s.count());
		return a.c;
	}

	public long indexCount(String name, Object value) throws IOException {
		var n = type.getSimpleName() + (name != null && !name.isEmpty() ? "." + name : "");
		var c = new long[1];
		database.performOnIndex(n, i -> c[0] = i.count(value));
		return c[0];
	}

	public void indexAccept(String name, Object value, IO.Consumer<LongStream> operation) throws IOException {
		var n = type.getSimpleName() + (name != null && !name.isEmpty() ? "." + name : "");
		database.performOnIndex(n, i -> operation
				.accept(i.list(value).mapToLong(o -> (Long) (o instanceof Object[] a ? a[a.length - 1] : o))));
	}

	protected Map<String, Entry<Object, Object>> getIndexMap(E entity, long id) {
		var m = new HashMap<String, Entry<Object, Object>>();
		for (var e : indexEntryGetters.entrySet()) {
			var n = e.getKey();
			var g = e.getValue();
			try {
				var f = g.getIndexEntry(entity, id);
				m.put(n, f);
			} catch (ReflectiveOperationException f) {
				throw new RuntimeException(f);
			}
		}
		return m;
	}

	protected Map<Object, Object> toMap(Entry<Object, Object> entry) {
		return toMap(entry, null);
	}

	protected Map<Object, Object> toMap(Entry<Object, Object> entry, Predicate<Object> predicate) {
		if (entry == null)
			return null;
		var k = entry.getKey();
		var v = entry.getValue();
		var m = new LinkedHashMap<Object, Object>();
		if (k == null) {
			if (predicate == null || predicate.test(null))
				m.put(null, v);
		} else
			switch (k) {
			case Collection<?> c:
				for (var l : c)
					if (predicate == null || predicate.test(l))
						m.put(l, v);
				break;
			default:
				if (predicate == null || predicate.test(k))
					m.put(k, v);
				break;
			}
		return m;
	}

	protected void updateIndex(String name, Map<Object, Object> remove, Map<Object, Object> add) throws IOException {
		var n = type.getSimpleName() + (name != null && !name.isEmpty() ? "." + name : "");
		database.performOnIndex(n, i -> {
			if (remove != null)
				for (var e : remove.entrySet())
					i.remove(e.getKey(), e.getValue());
			if (add != null)
				for (var e : add.entrySet())
					i.add(e.getKey(), e.getValue());
		});
	}
}

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
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

	public Long create(E entity) throws IOException {
		var d = database;
		var n = type.getSimpleName();
		var i = d.storeApply(n, s -> s.create(j -> {
			try {
				Reflection.setter(type, "id").invoke(entity, j);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			return formatter.apply(entity);
		}));
		for (var e : getIndexMap(entity, i).entrySet()) {
			var m = toMap(e.getValue(), null);
			updateIndex(e.getKey(), null, m);
		}
		return i;
	}

	public E read(Long id) throws IOException {
		var n = type.getSimpleName();
		var t = database.storeApply(n, s -> s.read(id));
		return parser.apply(t);
	}

	public E update(Long id, Consumer<E> consumer) throws IOException {
		class A {

			E e;

			Map<String, Entry<Object, Object>> m;
		}
		var n = type.getSimpleName();
		var a = new A();
		database.storeAccept(n, s -> s.update(id, t -> {
			a.e = parser.apply(t);
			a.m = getIndexMap(a.e, id);
			consumer.accept(a.e);
			return formatter.apply(a.e);
		}));
		if (a.e != null)
			for (var e : getIndexMap(a.e, id).entrySet()) {
				var f1 = a.m.get(e.getKey());
				var f2 = e.getValue();
				if (!Objects.equals(f1.getKey(), f2.getKey())) {
					var m1 = toMap(f1, f2.getKey() instanceof Collection<?> c ? k -> !c.contains(k) : null);
					var m2 = toMap(f2, f1.getKey() instanceof Collection<?> c ? k -> !c.contains(k) : null);
					updateIndex(e.getKey(), m1, m2);
				}
			}
		return a.e;
	}

	public E delete(Long id) throws IOException {
		var n = type.getSimpleName();
		var t = database.storeApply(n, s -> s.delete(id));
		var e = parser.apply(t);
		for (var f : getIndexMap(e, id).entrySet()) {
			var m = toMap(f.getValue(), null);
			updateIndex(f.getKey(), m, null);
		}
		return e;
	}

	public long count() throws IOException {
		return database.storeApply(type.getSimpleName(), s -> s.count());
	}

	public long indexCount(String name, Object value) throws IOException {
		var n = type.getSimpleName() + (name != null && !name.isEmpty() ? "." + name : "");
		return database.indexApply(n, i -> i.count(value));
	}

	public void indexAccept(String name, Object value, IO.Consumer<Long> consumer) throws IOException {
		var n = type.getSimpleName() + (name != null && !name.isEmpty() ? "." + name : "");
		var l = database.indexApply(n, i -> i.list(value));
		l.forEach(o -> {
			var i = (Long) (o instanceof Object[] a ? a[a.length - 1] : o);
			try {
				consumer.accept(i);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	public <R> Stream<R> indexApply(String name, Object value, IO.Function<Long, R> function) throws IOException {
		var n = type.getSimpleName() + (name != null && !name.isEmpty() ? "." + name : "");
		var l = database.indexApply(n, i -> i.list(value));
		return l.map(o -> {
			var i = (Long) (o instanceof Object[] a ? a[a.length - 1] : o);
			try {
				return function.apply(i);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	public <K> Stream<K> getIndexKeys(String name) throws IOException {
		var n = type.getSimpleName() + (name != null && !name.isEmpty() ? "." + name : "");
		return database.indexApply(n, i -> {
			@SuppressWarnings("unchecked")
			var j = (com.janilla.database.Index<K, ?>) i;
			return j.keys();
		});
	}

	public <V> Stream<V> getIndexValues(String name) throws IOException {
		var n = type.getSimpleName() + (name != null && !name.isEmpty() ? "." + name : "");
		return database.indexApply(n, i -> {
			@SuppressWarnings("unchecked")
			var j = (com.janilla.database.Index<?, V>) i;
			return j.values();
		});
	}

	protected Map<String, Entry<Object, Object>> getIndexMap(E entity, Long id) {
		var m = new HashMap<String, Entry<Object, Object>>();
		for (var e : indexEntryGetters.entrySet())
			try {
				m.put(e.getKey(), e.getValue().getIndexEntry(entity, id));
			} catch (ReflectiveOperationException f) {
				throw new RuntimeException(f);
			}
		return m;
	}

	protected Map<Object, Object> toMap(Entry<Object, Object> entry, Predicate<Object> predicate) {
		var m = new LinkedHashMap<Object, Object>();
		if (entry.getKey() == null) {
			if (predicate == null || predicate.test(null))
				m.put(null, entry.getValue());
		} else
			switch (entry.getKey()) {
			case Collection<?> c:
				for (var k : c)
					if (predicate == null || predicate.test(k))
						m.put(k, entry.getValue());
				break;
			default:
				if (predicate == null || predicate.test(entry.getKey()))
					m.put(entry.getKey(), entry.getValue());
				break;
			}
		return m;
	}

	protected void updateIndex(String name, Map<Object, Object> remove, Map<Object, Object> add) throws IOException {
		var n = type.getSimpleName() + (name != null && !name.isEmpty() ? "." + name : "");
		database.indexAccept(n, i -> {
			if (remove != null)
				for (var e : remove.entrySet())
					i.remove(e.getKey(), e.getValue());
			if (add != null)
				for (var e : add.entrySet())
					i.add(e.getKey(), e.getValue());
		});
	}
}

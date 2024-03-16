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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.janilla.database.Database;
import com.janilla.persistence.Persistence.IndexEntryGetter;
import com.janilla.reflect.Reflection;

public class Crud<E> {

	protected Class<E> type;

	protected Database database;

	protected Function<E, Object> formatter;

	protected Function<Object, E> parser;

	protected Map<String, IndexEntryGetter> indexEntryGetters = new HashMap<>();

	protected boolean indexPresent;

	public E create(E entity) throws IOException {
		database.perform((ss, ii) -> {
			var i = ss.perform(type.getSimpleName(), s -> s.create(x -> {
				try {
					Reflection.setter(type, "id").invoke(entity, x);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
				var t = formatter.apply(entity);
//			System.out.println("Crud.create t=" + t);
				return t;
			}));
			for (var e : getIndexMap(entity, i).entrySet()) {
				var n = e.getKey();
				var f = e.getValue();
				var m = toMap(f);
				updateIndex(n, null, m);
			}
			return i;
		}, true);
		return entity;
	}

	public E read(long id) throws IOException {
		if (id <= 0)
			return null;
		var o = database.perform((ss, ii) -> ss.perform(type.getSimpleName(), s -> s.read(id)), false);
		return o != null ? parser.apply(o) : null;
	}

	public Stream<E> read(long[] ids) throws IOException {
		if (ids.length == 0)
			return Stream.empty();
		return database.perform((ss, ii) -> {
			var b = Stream.<E>builder();
			for (var i : ids) {
				var o = ss.perform(type.getSimpleName(), s -> s.read(i));
				b.add(o != null ? parser.apply(o) : null);
			}
			return b.build();
		}, false);
	}

	public E update(long id, Consumer<E> consumer) throws IOException {
		if (id <= 0)
			return null;
		return database.perform((ss, ii) -> {
			class A {

				E e;

				Map<String, Entry<Object, Object>> m;
			}
			var a = new A();
			ss.perform(type.getSimpleName(), s -> s.update(id, x -> {
				a.e = parser.apply(x);
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
		}, true);
	}

	public E delete(long id) throws IOException {
		if (id <= 0)
			return null;
		return database.perform((ss, ii) -> {
			var o = ss.perform(type.getSimpleName(), s -> s.delete(id));
			var e = parser.apply(o);
			for (var f : getIndexMap(e, id).entrySet()) {
				var m = toMap(f.getValue());
				updateIndex(f.getKey(), m, null);
			}
			return e;
		}, true);
	}

	public long[] list() throws IOException {
		return database.perform(
				(ss, ii) -> indexPresent ? ii.perform(type.getSimpleName(), i -> getIndexIds(i.values()).toArray())
						: ss.perform(type.getSimpleName(), s -> s.ids().toArray()),
				false);
	}

	public Page list(long skip, long limit) throws IOException {
		return database.perform((ss, ii) -> indexPresent ? ii.perform(type.getSimpleName(), i -> {
			var j = getIndexIds(i.values().skip(skip).limit(limit)).toArray();
			var c = i.count();
			return new Page(j, c);
		}) : ss.perform(type.getSimpleName(), s -> {
			var i = s.ids().skip(skip).limit(limit).toArray();
			var c = s.count();
			return new Page(i, c);
		}), false);
	}

	public long count() throws IOException {
		return database.perform((ss, ii) -> ss.perform(type.getSimpleName(), s -> s.count()), false);
	}

	public long count(String index, Object key) throws IOException {
		var n = type.getSimpleName() + (index != null && !index.isEmpty() ? "." + index : "");
		return database.perform((ss, ii) -> ii.perform(n, i -> i.count(key)), false);
	}

	public long find(String index, Object key) throws IOException {
		var n = type.getSimpleName() + (index != null && !index.isEmpty() ? "." + index : "");
		return database.perform((ss, ii) -> ii.perform(n, i -> i.list(key)
				.mapToLong(o -> (Long) (o instanceof Object[] oo ? oo[oo.length - 1] : o)).findFirst().orElse(0)),
				false);
	}

	public long[] filter(String index, Object... keys) throws IOException {
		var n = type.getSimpleName() + (index != null && !index.isEmpty() ? "." + index : "");
		switch (keys.length) {
		case 0:
			return database.perform((ss, ii) -> ii.perform(n, i -> getIndexIds(i.values()).toArray()), false);
		case 1:
			return database.perform((ss, ii) -> ii.perform(n, i -> getIndexIds(i.list(keys[0])).toArray()), false);
		}
		class A {

			Iterator<Object> vv;

			Object v;
		}
		List<A> aa;
		try {
			aa = database.perform((ss, ii) -> ii.perform(n, i -> (List<A>) Arrays.stream(keys).map(k -> {
				try {
					var a = new A();
					a.vv = i.list(k).iterator();
					a.v = a.vv.hasNext() ? a.vv.next() : null;
					return a;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).toList()), false);
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		return LongStream.iterate(0, l -> {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			var a = aa.stream().max((a1, a2) -> {
				var c1 = a1.v != null ? (Comparable) ((Object[]) a1.v)[0] : null;
				var c2 = a2.v != null ? (Comparable) ((Object[]) a2.v)[0] : null;
				return c1 != null ? (c2 != null ? c1.compareTo(c2) : 1) : (c2 != null ? -1 : 0);
			}).orElse(null);
			if (a == null || a.v == null)
				return 0;
			var v = (Object[]) a.v;
			var i = (Long) v[v.length - 1];
			a.v = a.vv.hasNext() ? a.vv.next() : null;
			return i;
		}).skip(1).takeWhile(x -> x > 0).toArray();
	}

	public Page filter(String index, long skip, long limit, Object... keys) throws IOException {
		var n = type.getSimpleName() + (index != null && !index.isEmpty() ? "." + index : "");
		switch (keys.length) {
		case 0:
			return database.perform(
					(ss, ii) -> ii.perform(n,
							i -> new Page(getIndexIds(i.values()).skip(skip).limit(limit).toArray(), i.count())),
					false);
		case 1:
			return database.perform((ss, ii) -> ii.perform(n,
					i -> new Page(getIndexIds(i.list(keys[0])).skip(skip).limit(limit).toArray(), i.count(keys[0]))),
					false);
		}
		class A {

			Iterator<Object> vv;

			Object v;

			long l;
		}
		List<A> aa;
		try {
			aa = database.perform((ss, ii) -> ii.perform(n, i -> (List<A>) Arrays.stream(keys).map(k -> {
				try {
					var a = new A();
					a.vv = i.list(k).iterator();
					a.v = a.vv.hasNext() ? a.vv.next() : null;
					a.l = i.count(k);
					return a;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).toList()), false);
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}

		return new Page(LongStream.iterate(0, l -> {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			var a = aa.stream().max((a1, a2) -> {
				var c1 = a1.v != null ? (Comparable) ((Object[]) a1.v)[0] : null;
				var c2 = a2.v != null ? (Comparable) ((Object[]) a2.v)[0] : null;
				return c1 != null ? (c2 != null ? c1.compareTo(c2) : 1) : (c2 != null ? -1 : 0);
			}).orElse(null);
			if (a == null || a.v == null)
				return 0;
			var v = (Object[]) a.v;
			var i = (Long) v[v.length - 1];
			a.v = a.vv.hasNext() ? a.vv.next() : null;
			return i;
		}).skip(1 + skip).takeWhile(x -> x > 0).limit(limit).toArray(), aa.stream().mapToLong(x -> x.l).sum());
	}

	public Page filter2(String index, Predicate<Object> operation, long skip, long limit) throws IOException {
		var n = type.getSimpleName() + (index != null && !index.isEmpty() ? "." + index : "");
		return database.perform((ss, ii) -> ii.perform(n,
				i -> new Page(getIndexIds(i.valuesIf(operation)).skip(skip).limit(limit).toArray(),
						i.countIf(operation))),
				false);
	}

	public Page filter(Map<String, Object[]> keys, long skip, long limit) throws IOException {
		var ee = keys.entrySet().stream().filter(e -> e.getValue() != null && e.getValue().length > 0).toList();
		switch (ee.size()) {
		case 0:
			return list(skip, limit);
		case 1: {
			var e = ee.get(0);
			return filter(e.getKey(), skip, limit, e.getValue());
		}
		}
		return database.perform((ss, ii) -> {
			class A {

				PrimitiveIterator.OfLong ii;

				long i;
			}
			var aa = new ArrayList<A>();
			for (var e : ee) {
				var n = type.getSimpleName() + (e.getKey() != null && !e.getKey().isEmpty() ? "." + e.getKey() : "");
				class B {

					Iterator<Object[]> vv;

					Object[] v;
				}
				var bb = new ArrayList<B>();
				ii.perform(n, i -> {
					for (var k : e.getValue()) {
						var b = new B();
						b.vv = i.list(k).map(x -> (Object[]) x).iterator();
						b.v = b.vv.hasNext() ? b.vv.next() : null;
						bb.add(b);
					}
					return null;
				});
				var a = new A();
				a.ii = LongStream.iterate(0, l -> {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					var b = bb.stream().max((b1, b2) -> {
						var c1 = b1.v != null ? (Comparable) b1.v[0] : null;
						var c2 = b2.v != null ? (Comparable) b2.v[0] : null;
						return c1 != null ? (c2 != null ? c1.compareTo(c2) : 1) : (c2 != null ? -1 : 0);
					}).orElse(null);
					if (b == null || b.v == null)
						return 0;
					var i = (Long) b.v[b.v.length - 1];
					b.v = b.vv.hasNext() ? b.vv.next() : null;
					return i;
				}).skip(1).takeWhile(x -> x > 0).iterator();
				a.i = a.ii.hasNext() ? a.ii.nextLong() : 0;
				aa.add(a);
			}
			var i = LongStream.builder();
			var j = new long[1];
			var t = LongStream.iterate(0, l -> {
				for (;;) {
					var z = aa.stream().mapToLong(a -> a.i).max().getAsLong();
					if (z == 0)
						return 0;
					var e = true;
					for (var a : aa) {
						while (a.i > 0 && a.i < z)
							a.i = a.ii.hasNext() ? a.ii.nextLong() : 0;
						if (a.i != z)
							e = false;
					}
					if (e) {
						for (var a : aa)
							a.i = a.ii.hasNext() ? a.ii.nextLong() : 0;
						return z;
					}
				}
			}).skip(1).takeWhile(x -> x > 0).peek(x -> {
				var d = j[0]++ - skip;
				if (d >= 0 && d < limit)
					i.add(x);
			}).count();
			return new Page(i.build().toArray(), t);
		}, false);
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
			case long[] kk:
				for (var l : kk)
					if (predicate == null || predicate.test(l))
						m.put(l, v);
				break;
			case Collection<?> kk:
				for (var l : kk)
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
		database.perform((ss, ii) -> ii.perform(n, i -> {
			if (remove != null)
				for (var e : remove.entrySet())
					i.remove(e.getKey(), e.getValue());
			if (add != null)
				for (var e : add.entrySet())
					i.add(e.getKey(), e.getValue());
			return null;
		}), true);
	}

	protected LongStream getIndexIds(Stream<Object> results) {
		return results.mapToLong(x -> {
			return switch (x) {
			case Object[] oo -> (Long) oo[oo.length - 1];
			default -> (Long) x;
			};
		});
	}

	public record Page(long[] ids, long total) {

		static Page EMPTY = new Page(new long[0], 0);

		public static Page empty() {
			return EMPTY;
		}
	}
}

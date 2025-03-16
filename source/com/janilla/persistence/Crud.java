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
package com.janilla.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.reflect.Reflection;

public class Crud<E> {

	protected final Class<E> type;

	protected final Persistence persistence;

	protected final Map<String, Persistence.IndexEntryGetter> indexEntryGetters = new HashMap<>();

	public Crud(Class<E> type, Persistence persistence) {
		this.type = type;
		this.persistence = persistence;
	}

	public E create(E entity) {
//		System.out.println("Crud.create entity=" + entity);

		return persistence.database.perform((ss, _) -> {
			class A {

				E e;
			}
			var a = new A();
			var i = ss.perform(type.getSimpleName(), s -> s.create(x -> {
				a.e = Reflection.copy(new Entity(x), entity);
				var t = format(a.e);
//			System.out.println("Crud.create t=" + t);
				return t;
			}));
			updateIndexes(null, a.e, i);
			return a.e;
		}, true);
	}

	public E read(long id) {
		if (id <= 0)
			return null;
		var o = persistence.database.perform((ss, _) -> ss.perform(type.getSimpleName(), s -> s.read(id)), false);
		return o != null ? parse(o) : null;
	}

	public Stream<E> read(long[] ids) {
		if (ids == null || ids.length == 0)
			return Stream.empty();
		return persistence.database
				.perform((ss, _) -> ss.perform(type.getSimpleName(), s -> Arrays.stream(ids).mapToObj(x -> {
					var o = s.read(x);
					return o != null ? parse(o) : null;
				})), false);
	}

	public E update(long id, UnaryOperator<E> operator) {
		if (id <= 0)
			return null;
		return persistence.database.perform((ss, _) -> {
			class A {

				E e1;

				E e2;
			}
			var a = new A();
			ss.perform(type.getSimpleName(), s -> s.update(id, x -> {
				a.e1 = parse(x);
				a.e2 = operator.apply(a.e1);
				return format(a.e2);
			}));
			if (a.e1 != null)
				updateIndexes(a.e1, a.e2, id);
			return a.e2;
		}, true);
	}

	public E delete(long id) {
		if (id <= 0)
			return null;
		return persistence.database.perform((ss, _) -> {
			var o = ss.perform(type.getSimpleName(), s -> s.delete(id));
			var e = parse(o);
			updateIndexes(e, null, id);
			return e;
		}, true);
	}

	public long[] list() {
		return persistence.database.perform((ss, ii) -> type.isAnnotationPresent(Index.class)
				? ii.perform(type.getSimpleName(), i -> getIndexIds(i.values()).toArray())
				: ss.perform(type.getSimpleName(), s -> s.ids().toArray()), false);
	}

	public IdPage list(long skip, long limit) {
//		System.out.println("Crud.list, skip=" + skip + ", limit=" + limit);
		return persistence.database
				.perform((ss, ii) -> type.isAnnotationPresent(Index.class) ? ii.perform(type.getSimpleName(), i -> {
					var vv = i.values();
					if (skip > 0)
						vv = vv.skip(skip);
					if (limit >= 0)
						vv = vv.limit(limit);
					var j = getIndexIds(vv).toArray();
					var c = i.count();
					return new IdPage(j, c);
				}) : ss.perform(type.getSimpleName(), s -> {
					var jj = s.ids();
					if (skip > 0)
						jj = jj.skip(skip);
					if (limit >= 0)
						jj = jj.limit(limit);
					var i = jj.toArray();
					var c = s.count();
					return new IdPage(i, c);
				}), false);
	}

	public long count() {
		return persistence.database.perform(
				(ss, ii) -> type.isAnnotationPresent(Index.class) ? ii.perform(type.getSimpleName(), i -> i.count())
						: ss.perform(type.getSimpleName(), s -> s.count()),
				false);
	}

	public long count(String index, Object key) {
		var n = Stream.of(type.getSimpleName(), index).filter(x -> x != null && !x.isEmpty())
				.collect(Collectors.joining("."));
		return persistence.database.perform((_, ii) -> ii.perform(n, i -> i.count(key)), false);
	}

	public long find(String index, Object key) {
		var n = Stream.of(type.getSimpleName(), index).filter(x -> x != null && !x.isEmpty())
				.collect(Collectors.joining("."));
		return persistence.database.perform((_, ii) -> ii.perform(n, i -> i.list(key)
				.mapToLong(o -> (Long) (o instanceof Object[] oo ? oo[oo.length - 1] : o)).findFirst().orElse(0)),
				false);
	}

	public long[] filter(String index, Object... keys) {
		var n = Stream.of(type.getSimpleName(), index).filter(x -> x != null && !x.isEmpty())
				.collect(Collectors.joining("."));
//		System.out.println("n=" + n + ", keys=" + Arrays.toString(keys));
		switch (keys.length) {
		case 0:
			return persistence.database.perform((_, ii) -> ii.perform(n, i -> getIndexIds(i.values()).toArray()),
					false);
		case 1:
			return persistence.database.perform((_, ii) -> ii.perform(n, i -> getIndexIds(i.list(keys[0])).toArray()),
					false);
		}
		class A {

			Iterator<Object> vv;

			Object v;
		}
		List<A> aa = persistence.database.perform((_, ii) -> ii.perform(n, i -> (List<A>) Arrays.stream(keys).map(k -> {
			var a = new A();
			a.vv = i.list(k).iterator();
			a.v = a.vv.hasNext() ? a.vv.next() : null;
			return a;
		}).toList()), false);
		return LongStream.iterate(0, _ -> {
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

	public IdPage filter(String index, long skip, long limit, Object... keys) {
		var n = Stream.of(type.getSimpleName(), index).filter(x -> x != null && !x.isEmpty())
				.collect(Collectors.joining("."));
		switch (keys.length) {
		case 0:
			return persistence.database.perform((_, ii) -> ii.perform(n, i -> {
				var jj = getIndexIds(i.values());
				if (skip > 0)
					jj = jj.skip(skip);
				if (limit >= 0)
					jj = jj.limit(limit);
				return new IdPage(jj.toArray(), i.count());
			}), false);
		case 1:
			return persistence.database.perform((_, ii) -> ii.perform(n, i -> {
				var jj = getIndexIds(i.list(keys[0]));
				if (skip > 0)
					jj = jj.skip(skip);
				if (limit >= 0)
					jj = jj.limit(limit);
				return new IdPage(jj.toArray(), i.count(keys[0]));
			}), false);
		}
		class A {

			Iterator<Object> vv;

			Object v;

			long l;
		}
		List<A> aa;
		aa = persistence.database.perform((_, ii) -> ii.perform(n, i -> (List<A>) Arrays.stream(keys).map(k -> {
			var a = new A();
			a.vv = i.list(k).iterator();
			a.v = a.vv.hasNext() ? a.vv.next() : null;
			a.l = i.count(k);
			return a;
		}).toList()), false);

		return new IdPage(LongStream.iterate(0, _ -> {
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

	public long[] filter(String index, Predicate<Object> operation) {
		var n = Stream.of(type.getSimpleName(), index).filter(x -> x != null && !x.isEmpty())
				.collect(Collectors.joining("."));
		return persistence.database.perform((_, ii) -> ii.perform(n, i -> getIndexIds(i.valuesIf(operation)).toArray()),
				false);
	}

	public IdPage filter(String index, Predicate<Object> operation, long skip, long limit) {
		var n = Stream.of(type.getSimpleName(), index).filter(x -> x != null && !x.isEmpty())
				.collect(Collectors.joining("."));
		return persistence.database.perform((_, ii) -> ii.perform(n,
				i -> new IdPage(getIndexIds(i.valuesIf(operation)).skip(skip).limit(limit).toArray(),
						i.countIf(operation))),
				false);
	}

	public IdPage filter(Map<String, Object[]> keys, long skip, long limit) {
		var ee = keys.entrySet().stream().filter(x -> x.getValue() != null && x.getValue().length > 0).toList();
		if (ee.isEmpty())
			return list(skip, limit);
		if (ee.size() == 1) {
			var e = ee.get(0);
			return filter(e.getKey(), skip, limit, e.getValue());
		}
		return persistence.database.perform((_, ii) -> {
			class A {

				PrimitiveIterator.OfLong lli;

				long l;
			}
			var aa = new ArrayList<A>();
			for (var e : ee) {
				var n = Stream.of(type.getSimpleName(), e.getKey()).filter(x -> x != null && !x.isEmpty())
						.collect(Collectors.joining("."));
				class B {

					Iterator<Object[]> vvi;

					Object[] v;
				}
				var bb = new ArrayList<B>();
				ii.perform(n, i -> {
					for (var k : e.getValue()) {
						var b = new B();
						b.vvi = i.list(k).map(x -> (Object[]) x).iterator();
						b.v = b.vvi.hasNext() ? b.vvi.next() : null;
						bb.add(b);
					}
					return null;
				});
				var a = new A();
				a.lli = LongStream.iterate(0, _ -> {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					var b = bb.stream().max((b1, b2) -> {
						var v1 = b1.v != null ? (Comparable) b1.v[0] : null;
						var v2 = b2.v != null ? (Comparable) b2.v[0] : null;
						return v1 != null ? (v2 != null ? v1.compareTo(v2) : 1) : (v2 != null ? -1 : 0);
					}).orElse(null);
					if (b == null || b.v == null)
						return 0;
					var l = (long) b.v[b.v.length - 1];
					b.v = b.vvi.hasNext() ? b.vvi.next() : null;
					return l;
				}).skip(1).takeWhile(x -> x > 0).iterator();
				a.l = a.lli.hasNext() ? a.lli.nextLong() : 0;
				aa.add(a);
			}
			var llb = LongStream.builder();
			class C {

				long l;
			}
			var c = new C();
			var t = LongStream.iterate(0, _ -> {
				for (;;) {
					var ll = aa.stream().mapToLong(a -> a.l).toArray();
//					System.out.println("ll=" + Arrays.toString(ll));
					var l = Arrays.stream(ll).allMatch(y -> y != 0) ? Arrays.stream(ll).max().getAsLong() : 0;
					if (l == 0)
						return 0;
					var e = true;
					for (var a : aa) {
						while (a.l != 0 && a.l < l)
							a.l = a.lli.hasNext() ? a.lli.nextLong() : 0;
						if (a.l != l)
							e = false;
					}
					if (e) {
						for (var a : aa)
							a.l = a.lli.hasNext() ? a.lli.nextLong() : 0;
						return l;
					}
				}
			}).skip(1).takeWhile(x -> x != 0).peek(x -> {
				var o = c.l++ - skip;
				if (o >= 0 && (limit < 0 || o < limit)) {
//					System.out.println("x=" + x);
					llb.add(x);
				}
			}).count();
			return new IdPage(llb.build().toArray(), t);
		}, false);
	}

	protected String format(Object object) {
		var tt = new ReflectionJsonIterator();
		tt.setObject(object);
		tt.setIncludeType(true);
		return Json.format(tt);
	}

	protected E parse(Object object) {
		var c = new Converter(persistence.typeResolver);
		@SuppressWarnings("unchecked")
		var e = (E) c.convert(Json.parse((String) object), type);
		return e;
	}

	protected void updateIndexes(E entity1, E entity2, long id) {
		var im1 = entity1 != null ? getIndexMap(entity1, id) : null;
		var im2 = entity2 != null ? getIndexMap(entity2, id) : null;
		for (var k : (im1 != null ? im1 : im2).keySet()) {
			var v1 = im1 != null ? im1.get(k) : null;
			var v2 = im2 != null ? im2.get(k) : null;
			if (v1 == null || v2 == null || !Objects.equals(v1.getKey(), v2.getKey())
					|| !Arrays.equals((Object[]) v1.getValue(), (Object[]) v2.getValue())) {
				var k1 = v1 != null ? v1.getKey() : null;
				var k2 = v2 != null ? v2.getKey() : null;
				var m1 = v1 == null ? null : k2 instanceof Collection<?> c ? toMap(v1, x -> !c.contains(x)) : toMap(v1);
				var m2 = v2 == null ? null : k1 instanceof Collection<?> c ? toMap(v2, x -> !c.contains(x)) : toMap(v2);
				updateIndex(k, m1, m2);
			}
		}
	}

	protected Map<String, Map.Entry<Object, Object>> getIndexMap(E entity, long id) {
		var m = new HashMap<String, Map.Entry<Object, Object>>();
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

	protected Map<Object, Object> toMap(Map.Entry<Object, Object> entry) {
		return toMap(entry, null);
	}

	protected Map<Object, Object> toMap(Map.Entry<Object, Object> entry, Predicate<Object> predicate) {
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

	protected void updateIndex(String name, Map<Object, Object> remove, Map<Object, Object> add) {
		var n = Stream.of(type.getSimpleName(), name).filter(x -> x != null && !x.isEmpty())
				.collect(Collectors.joining("."));
//		System.out.println("Crud.updateIndex, n=" + n + ", remove=" + remove + ", add=" + add);
		persistence.database.perform((_, ii) -> ii.perform(n, i -> {
			if (remove != null)
				for (var e : remove.entrySet())
					i.remove(e.getKey(), new Object[] { e.getValue() });
			if (add != null)
				for (var e : add.entrySet())
					i.add(e.getKey(), new Object[] { e.getValue() });
			return null;
		}), true);
	}

	protected LongStream getIndexIds(Stream<Object> results) {
		return results.mapToLong(x -> {
			var y = switch (x) {
			case Object[] oo -> (Long) oo[oo.length - 1];
			default -> (Long) x;
			};
//			System.out.println("y=" + y);
			return y;
		});
	}

	public record Entity(long id) {
	}

	public record IdPage(long[] ids, long total) {

		static IdPage EMPTY = new IdPage(new long[0], 0);

		public static IdPage empty() {
			return EMPTY;
		}
	}
}

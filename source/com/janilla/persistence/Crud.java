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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.json.JsonToken;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.json.ReflectionValueIterator;
import com.janilla.json.TokenIterationContext;
import com.janilla.reflect.Reflection;

public class Crud<ID extends Comparable<ID>, E extends Entity<ID>> {

	protected final Class<E> type;

	protected final Function<E, ID> nextId;

	protected final Persistence persistence;

	protected final Map<String, Persistence.IndexEntryGetter> indexEntryGetters = new HashMap<>();

	protected final List<Observer> observers = new ArrayList<>();

	public Crud(Class<E> type, Function<E, ID> nextId, Persistence persistence) {
		this.type = type;
		this.nextId = nextId;
		this.persistence = persistence;
	}

	public List<Observer> observers() {
		return observers;
	}

	public E create(E entity) {
//		IO.println("Crud.create entity=" + entity);

		return persistence.database.perform(() -> {
			class A {

				E e;

				ID i;
			}
			var a = new A();
			a.e = entity;
			if (nextId != null) {
				@SuppressWarnings("unchecked")
				var x = (ID) Reflection.property(type, "id").get(a.e);
				if (x == null)
					x = nextId.apply(a.e);
				a.i = x;
				persistence.database.indexBTree(type.getSimpleName()).insert(a.i.toString(), format(a.e));
			} else {
				@SuppressWarnings("unchecked")
				var x = (ID) Long.valueOf(persistence.database.tableBTree(type.getSimpleName()).insert(k -> {
					a.e = Reflection.copy(Map.of("id", k), a.e);
					for (var y : observers)
						a.e = y.beforeCreate(a.e);
					return new Object[] { format(a.e) };
				}));
				a.i = x;
			}
			updateIndexes(null, entity, a.i);
			for (var x : observers)
				x.afterCreate(a.e);
			return a.e;
		}, true);
	}

	public E read(ID id) {
		if (id == null)
			return null;
		return persistence.database.perform(() -> persistence.database.performOnTable(type.getSimpleName(), t -> {
			var oo = t.select((Long) id);
//					IO.println("Crud.read, x=" + x + ", o=" + o);
			return oo != null ? parse((String) oo[0]) : null;
		}), false);
	}

	public List<E> read(List<ID> ids) {
		if (ids == null || ids.isEmpty())
			return List.of();
		return persistence.database
				.perform(() -> persistence.database.performOnTable(type.getSimpleName(), t -> ids.stream().map(x -> {
					var oo = t.select((Long) x);
//					IO.println("Crud.read, x=" + x + ", o=" + o);
					return oo != null ? parse((String) oo[0]) : null;
				}).toList()), false);
	}

	public E update(ID id, UnaryOperator<E> operator) {
		if (id == null)
			return null;
		return persistence.database.perform(() -> {
			class A {

				E e1;

				E e2;
			}
			var a = new A();
			persistence.database.performOnTable(type.getSimpleName(), t -> {
				t.update((Long) id, x -> {
					a.e1 = parse((String) x[0]);
					a.e2 = operator.apply(a.e1);
					for (var y : observers)
						a.e2 = y.beforeUpdate(a.e2);
					return new Object[] { format(a.e2) };
				});
				return null;
			});
			updateIndexes(a.e1, a.e2, id);
			for (var x : observers)
				x.afterUpdate(a.e1, a.e2);
			return a.e2;
		}, true);
	}

	public E delete(ID id) {
		if (id == null)
			return null;
		return persistence.database.perform(() -> {
			var oo = persistence.database.performOnTable(type.getSimpleName(), t -> t.delete((Long) id));
			var e = parse((String) oo[0]);
			updateIndexes(e, null, id);
			for (var x : observers)
				x.afterDelete(e);
			return e;
		}, true);
	}

	public List<E> delete(List<ID> ids) {
		if (ids == null || ids.isEmpty())
			return List.of();
		return persistence.database.perform(() -> ids.stream().map(this::delete).toList(), true);
	}

	public List<ID> list() {
		return persistence.database.perform(() -> persistence.database.performOnTable(type.getSimpleName(), t -> {
			@SuppressWarnings("unchecked")
			var ii = (List<ID>) t.keys().boxed().toList();
			return ii;
		}), false);
	}

	public IdPage<ID> list(long skip, long limit) {
//		IO.println("Crud.list, skip=" + skip + ", limit=" + limit);
		return persistence.database.perform(() -> persistence.database.performOnTable(type.getSimpleName(), t -> {
			var kk = t.keys();
			if (skip > 0)
				kk = kk.skip(skip);
			if (limit >= 0)
				kk = kk.limit(limit);
			@SuppressWarnings("unchecked")
			var ii = (List<ID>) kk.boxed().toList();
			return new IdPage<>(ii, t.count());
		}), false);
	}

	public long count() {
		return persistence.database
				.perform(() -> persistence.database.performOnTable(type.getSimpleName(), t -> t.count()), false);
	}

	public long count(String index, Object key) {
		throw new RuntimeException();
	}

	public ID find(String index, Object key) {
		throw new RuntimeException();
	}

	public List<ID> filter(String index, Object... keys) {
		var n = Stream.of(type.getSimpleName(), index).filter(x -> x != null && !x.isEmpty())
				.collect(Collectors.joining("."));
//		IO.println("n=" + n + ", keys=" + Arrays.toString(keys));
		return switch (keys.length) {
		case 0 -> persistence.database.perform(() -> persistence.database.performOnIndex(n, t -> {
			@SuppressWarnings("unchecked")
			var ii = (List<ID>) t.foo().boxed().toList();
			return ii;
		}), false);
		case 1 -> persistence.database.perform(() -> persistence.database.performOnIndex(n, t -> {
			@SuppressWarnings("unchecked")
			var ii = (List<ID>) t.foo(keys[0]).boxed().toList();
			return ii;
		}), false);
		default -> throw new RuntimeException();
		};
	}

	public IdPage<ID> filter(String index, long skip, long limit, Object... keys) {
		throw new RuntimeException();
	}

	public List<ID> filter(String index, Predicate<Object> operation) {
		throw new RuntimeException();
	}

	public IdPage<ID> filter(String index, Predicate<Object> operation, long skip, long limit) {
		throw new RuntimeException();
	}

	public IdPage<ID> filter(Map<String, Object[]> keys, long skip, long limit) {
		throw new RuntimeException();
	}

	protected String format(Object object) {
		return Json.format(new CustomReflectionJsonIterator(object, true));
	}

	protected E parse(String string) {
		return parse(string, type);
	}

	protected <T> T parse(String string, Class<T> target) {
//		IO.println("Crud.parse, string=" + string + ", target=" + target);
		var c = new Converter(persistence.typeResolver);
		@SuppressWarnings("unchecked")
		var t = (T) c.convert(Json.parse(string), target);
		return t;
	}

	protected void updateIndexes(E entity1, E entity2, ID id) {
//		IO.println("Crud.updateIndexes, entity1=" + entity1 + ", entity2=" + entity2 + ", id=" + id);
		updateIndexes(entity1, entity2, id, this::getIndexName);
	}

	protected void updateIndexes(E entity1, E entity2, ID id, UnaryOperator<String> indexName) {
		var im1 = entity1 != null ? getIndexMap(entity1, id) : null;
		var im2 = entity2 != null ? getIndexMap(entity2, id) : null;
		for (var k : (im1 != null ? im1 : im2).keySet()) {
			var v1 = im1 != null ? im1.get(k) : null;
			var v2 = im2 != null ? im2.get(k) : null;
			if (v1 == null || v2 == null || !Objects.equals(v1.getKey(), v2.getKey())
//					|| !Arrays.equals((Object[]) v1.getValue(), (Object[]) v2.getValue())) {
					|| !Objects.equals(v1.getValue(), v2.getValue())) {
				var k1 = v1 != null ? v1.getKey() : null;
				var k2 = v2 != null ? v2.getKey() : null;
				var m1 = v1 == null ? null : k2 instanceof Collection<?> c ? toMap(v1, x -> !c.contains(x)) : toMap(v1);
				var m2 = v2 == null ? null : k1 instanceof Collection<?> c ? toMap(v2, x -> !c.contains(x)) : toMap(v2);
				updateIndex(indexName.apply(k), m1, m2);
			}
		}
	}

	protected String getIndexName(String key) {
		return Stream.of(type.getSimpleName(), key).filter(x -> x != null && !x.isEmpty())
				.collect(Collectors.joining("."));
	}

	protected Map<String, Map.Entry<Object, Object>> getIndexMap(E entity, ID id) {
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

	protected void updateIndex(String n, Map<Object, Object> remove, Map<Object, Object> add) {
//		var n = Stream.of(type.getSimpleName(), name).filter(x -> x != null && !x.isEmpty())
//				.collect(Collectors.joining("."));
//		IO.println("Crud.updateIndex, n=" + n + ", remove=" + remove + ", add=" + add);

		persistence.database.perform(() -> persistence.database.performOnIndex(n, t -> {
			if (remove != null)
				for (var e : remove.entrySet())
					t.delete(foo(e.getKey()), foo(e.getValue()));
			if (add != null)
				for (var e : add.entrySet())
					t.insert(foo(e.getKey()), foo(e.getValue()));
			return null;
		}), true);
	}

	protected Object foo(Object object) {
		return object == null || object instanceof Number ? object : object.toString();
	}

//	protected Stream<ID> getIndexIds(Stream<Object> results) {
//		return results.map(x -> {
//			@SuppressWarnings("unchecked")
//			var y = (ID) switch (x) {
//			case Object[] oo -> oo[oo.length - 1];
//			default -> x;
//			};
//			return y;
//		});
//	}

	public record IdPage<ID>(List<ID> ids, long total) {

		private static IdPage<?> EMPTY = new IdPage<>(List.of(), 0);

		@SuppressWarnings("unchecked")
		public static <ID> IdPage<ID> empty() {
			return (IdPage<ID>) EMPTY;
		}
	}

	public interface Observer {

		default <E> E beforeCreate(E entity) {
			return entity;
		}

		default <E> void afterCreate(E entity) {
		}

		default <E> E beforeUpdate(E entity) {
			return entity;
		}

		default <E> void afterUpdate(E entity1, E entity2) {
		}

		default <E> void afterDelete(E entity) {
		}
	}

	protected class CustomReflectionJsonIterator extends ReflectionJsonIterator {

		public CustomReflectionJsonIterator(Object object, boolean includeType) {
			super(object, includeType);
		}

		@Override
		public Iterator<JsonToken<?>> newValueIterator(Object object) {
			return new CustomReflectionValueIterator(this, object);
		}
	}

	protected class CustomReflectionValueIterator extends ReflectionValueIterator {

		public CustomReflectionValueIterator(TokenIterationContext context, Object object) {
			super(context, object);
		}

		@Override
		protected Iterator<JsonToken<?>> newIterator() {
			return value instanceof Class<?> c ? context.newStringIterator(persistence.typeResolver.format(c))
					: super.newIterator();
		}
	}
}

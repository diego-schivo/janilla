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

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.backend.sqlite.BTree;
import com.janilla.backend.sqlite.IndexBTree;
import com.janilla.backend.sqlite.PayloadCell;
import com.janilla.backend.sqlite.Record;
import com.janilla.backend.sqlite.RecordColumn;
import com.janilla.backend.sqlite.TableBTree;
import com.janilla.backend.sqlite.TableLeafCell;
import com.janilla.java.Java;
import com.janilla.java.JavaReflect;
import com.janilla.java.Property;
import com.janilla.java.TypeResolver;
import com.janilla.json.Json;
import com.janilla.json.JsonToken;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.json.ReflectionValueIterator;
import com.janilla.json.TokenIterationContext;
import com.janilla.persistence.Entity;
import com.janilla.persistence.ListPortion;

public class DefaultCrud<ID extends Comparable<ID>, E extends Entity<ID>> implements Crud<ID, E> {

	protected final Class<E> type;

	protected final IdHelper<ID> idHelper;

	protected final Persistence persistence;

	protected final Map<String, IndexKeyGetter> indexKeyGetters = new LinkedHashMap<>();

	protected final List<CrudObserver<E>> observers = new ArrayList<>();

	public DefaultCrud(Class<E> type, IdHelper<ID> idHelper, Persistence persistence) {
		this.type = type;
		this.idHelper = idHelper;
		this.persistence = persistence;
	}

	@Override
	public Class<E> type() {
		return type;
	}

	@Override
	public List<CrudObserver<E>> observers() {
		return observers;
	}

	@Override
	public E create(E entity) {
		IO.println("DefaultCrud.create, entity=" + entity);
		return persistence.database().perform(() -> {
			class A {
				ID i;
				E e;
			}
			var a = new A();
			a.e = entity;
			if (idHelper != null) {
				{
					@SuppressWarnings("unchecked")
					var x = (ID) JavaReflect.property(type, "id").get(a.e);
					a.i = x;
				}
				if (a.i == null)
					a.i = idHelper.random(a.e);
				a.e = JavaReflect.copy(Map.of("id", a.i), a.e);
				for (var o : observers)
					a.e = o.beforeCreate(a.e);
				bTree().insert(new Object[] { toDatabaseId(a.i) }, new Object[] { format(a.e) });
			} else {
				@SuppressWarnings("unchecked")
				var y = (ID) Long.valueOf(((TableBTree) bTree()).insert(x -> {
					a.e = JavaReflect.copy(Map.of("id", x), a.e);
					for (var o : observers)
						a.e = o.beforeCreate(a.e);
					return new Object[] { x, format(a.e) };
				}));
				a.i = y;
			}
			updateIndexes(null, a.e);
			for (var o : observers)
				o.afterCreate(a.e);
			return a.e;
		}, true);
	}

	@Override
	public E read(ID id, int depth) {
		if (id == null)
			return null;
		return persistence.database().perform(() -> read(bTree(), id, depth), false);
	}

	@Override
	public List<E> read(List<ID> ids, int depth) {
		if (ids == null || ids.isEmpty())
			return List.of();
		return persistence.database().perform(() -> {
			var t = bTree();
			return ids.stream().map(x -> x != null ? read(t, x, depth) : null).toList();
		}, false);
	}

	protected E read(BTree<?, ?> bTree, ID id, int depth) {
//		IO.println("DefaultCrud.read, type=" + type.getSimpleName() + ", id=" + id + ", depth=" + depth);
		class A {
			E e;
		}
		var a = new A();
		bTree.select(new Object[] { toDatabaseId(id) }, false, x -> {
			var oo = x.findFirst().orElse(null);
			a.e = oo != null ? parse((String) oo.reduce((_, y) -> y).get()) : null;
		});
//		IO.println("DefaultCrud.read, a.e=" + a.e);
		a.e = populate(a.e, depth);
//		IO.println("DefaultCrud.read, depth=" + depth + ", a.e=" + a.e);
		if (a.e != null)
			for (var o : observers)
				a.e = o.afterRead(a.e);
		return a.e;
	}

	@Override
	public E update(ID id, UnaryOperator<E> operator) {
		if (id == null)
			return null;
		return persistence.database().perform(() -> {
			class A {
				E e1;
				E e2;
			}
			var a = new A();
			var t = bTree();
			var kk = new Object[] { toDatabaseId(id) };
			t.delete(kk, rr -> {
				var oo = rr.findFirst().get();
//				IO.println("oo=" + Arrays.toString(oo));
				a.e1 = parse((String) oo.reduce((_, y) -> y).get());
			});
//			for (var o : observers)
//				a.e1 = o.afterRead(a.e1);
			a.e2 = operator.apply(a.e1);
			for (var o : observers)
				a.e2 = o.beforeUpdate(a.e2);
			t.insert(kk, new Object[] { format(a.e2) });
			updateIndexes(a.e1, a.e2);
			for (var x : observers)
				x.afterUpdate(a.e1, a.e2);
			return a.e2;
		}, true);
	}

	@Override
	public E delete(ID id) {
//		IO.println("DefaultCrud.delete, id=" + id);
		if (id == null)
			throw new NullPointerException();
		return persistence.database().perform(() -> {
			class A {
				E e;
			}
			var a = new A();
			bTree().delete(new Object[] { toDatabaseId(id) }, rr -> {
				var oo = rr.findFirst().get();
//				IO.println("DefaultCrud.delete, oo=" + Arrays.toString(oo));
				a.e = parse((String) oo.reduce((_, y) -> y).get());
			});
			updateIndexes(a.e, null);
			for (var x : observers)
				x.afterDelete(a.e);
			return a.e;
		}, true);
	}

	@Override
	public List<E> delete(List<ID> ids) {
		if (ids == null || ids.isEmpty())
			return List.of();
		return persistence.database().perform(() -> ids.stream().map(this::delete).toList(), true);
	}

	@Override
	public List<ID> list(boolean reverse) {
		return persistence.database().perform(() -> {
			var t = bTree();
			return t.cells(reverse).map(c -> {
				var o = switch (c) {
				case TableLeafCell x -> x.key();
				case PayloadCell x ->
					Record.fromBytes(t.payloadBuffers(x)).findFirst().map(RecordColumn::toObject).get();
				default -> throw new RuntimeException();
				};
				var id = fromDatabaseId(o);
//				IO.println("DefaultCrud.list, o=" + o + ", id=" + id);
				return id;
			}).toList();
		}, false);
	}

	@Override
	public long count() {
		return persistence.database().perform(() -> bTree().count(new Object[0]), false);
	}

	@Override
	public ListPortion<ID> listAndCount(boolean reverse, long skip, long limit) {
//		IO.println("DefaultCrud.list, skip=" + skip + ", limit=" + limit);
		return persistence.database().perform(() -> {
			var t = bTree();
			var cc = t.cells(reverse);
			if (skip > 0)
				cc = cc.skip(skip);
			if (limit >= 0)
				cc = cc.limit(limit);
			return new ListPortion<>(cc.map(c -> {
				var o = switch (c) {
				case TableLeafCell c2 -> c2.key();
				case PayloadCell c2 ->
					Record.fromBytes(t.payloadBuffers(c2)).findFirst().map(RecordColumn::toObject).get();
				default -> throw new RuntimeException();
				};
				var id = fromDatabaseId(o);
//				IO.println("id=" + id);
				return id;
			}).toList(), t.count(new Object[0]));
		}, false);
	}

	@Override
	public List<ID> filter(String index, Object[] keys, boolean reverse, long skip, long limit) {
//		IO.println("DefaultCrud.filter, type=" + type.getSimpleName() + ", index=" + index + ", keys="
//				+ Arrays.toString(keys));
		return persistence.database().perform(() -> {
			var t = getIndex(index);

			class A {
				Iterator<Object[]> vv;
				Object[] v;
			}
			var aa = (keys.length != 0 ? Arrays.stream(keys) : Stream.of((Object) null)).map(k -> {
				var a = new A();
				var kk = keys.length != 0 ? new Object[] { toDatabaseValue(k) } : new Object[0];
				t.select(kk, reverse, x -> a.vv = x.map(Stream::toArray).iterator());
				a.v = a.vv.hasNext() ? a.vv.next() : null;
				return a;
			}).toList();

			var ii = Stream.iterate((ID) null, _ -> {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Comparator<A> c = (a1, a2) -> {
					var c1 = a1.v != null ? (Comparable) a1.v[1] : null;
					var c2 = a2.v != null ? (Comparable) a2.v[1] : null;
					return c1 != null ? (c2 != null ? c1.compareTo(c2) : 1) : (c2 != null ? -1 : 0);
				};
				var s = aa.stream();
				var a = (reverse ? s.max(c) : s.min(c)).orElse(null);
				if (a == null || a.v == null)
					return null;
				var v = a.v;
				a.v = a.vv.hasNext() ? a.vv.next() : null;
				return fromDatabaseId(v[v.length - 1]);
			}).skip(1).takeWhile(Objects::nonNull);
			if (skip > 0)
				ii = ii.skip(skip);
			if (limit >= 0)
				ii = ii.limit(limit);
			return ii.toList();
		}, false);
	}

	@Override
	public long count(String index, Object[] keys) {
		return persistence.database().perform(() -> {
			var t = getIndex(index);
			return (keys.length != 0 ? Arrays.stream(keys) : Stream.of((Object) null))
					.mapToLong(k -> t.count(keys.length != 0 ? new Object[] { toDatabaseValue(k) } : keys)).sum();
		}, false);
	}

	@Override
	public ListPortion<ID> filterAndCount(String index, Object[] keys, boolean reverse, long skip, long limit) {
		return persistence.database()
				.perform(() -> new ListPortion<>(filter(index, keys, reverse, skip, limit), count(index, keys)), false);
	}

	@Override
	public List<ID> filter(String index, Predicate<Object> operation, boolean reverse) {
		return persistence.database().perform(() -> {
			var t = getIndex(index);
			@SuppressWarnings("unchecked")
			var ii = t.rows(reverse).map(Stream::toArray).filter(x -> operation.test(x[0]))
					.map(x -> (ID) x[x.length - 1]).toList();
			return ii;
		}, false);
	}

	@Override
	public ListPortion<ID> filterAndCount(String index, Predicate<Object> operation, boolean reverse, long skip,
			long limit) {
		return persistence.database().perform(() -> {
			var t = getIndex(index);
			@SuppressWarnings("unchecked")
			var ii = t.rows(reverse).map(Stream::toArray).filter(x -> operation.test(x[0]))
					.map(x -> (ID) x[x.length - 1]).toList();
			var s = ii.stream();
			if (skip > 0)
				s = s.skip(skip);
			if (limit >= 0)
				s = s.limit(limit);
			return new ListPortion<>(s.toList(), ii.size());
		}, false);
	}

	@Override
	public ListPortion<ID> filterAndCount(Map<String, Object[]> keys, boolean reverse, long skip, long limit) {
		var ee = keys.entrySet().stream().filter(x -> x.getValue() != null && x.getValue().length > 0).toList();
		if (ee.isEmpty())
			return listAndCount(reverse, skip, limit);
		if (ee.size() == 1) {
			var e = ee.getFirst();
			return filterAndCount(e.getKey(), e.getValue(), reverse, skip, limit);
		}
		return persistence.database().perform(() -> {
			class A {
				Iterator<ID> lli;
				ID l;
			}
			var aa = new ArrayList<A>();
			for (var e : ee) {
				class B {

					Iterator<Object[]> vvi;

					Object[] v;
				}
				var bb = new ArrayList<B>();
				var i = getIndex(e.getKey());
				for (var k : e.getValue()) {
					var b = new B();
					i.select(new Object[] { k }, reverse, x -> b.vvi = x.map(Stream::toArray).iterator());
					b.v = b.vvi != null && b.vvi.hasNext() ? b.vvi.next() : null;
					bb.add(b);
				}
				var a = new A();
				a.lli = Stream.iterate((ID) null, _ -> {
					var b = bb.stream().max((b1, b2) -> {
						@SuppressWarnings("unchecked")
						var v1 = b1.v != null ? (ID) b1.v[0] : null;
						@SuppressWarnings("unchecked")
						var v2 = b2.v != null ? (ID) b2.v[0] : null;
						return v1 != null ? (v2 != null ? v1.compareTo(v2) : 1) : (v2 != null ? -1 : 0);
					}).orElse(null);
					if (b == null || b.v == null)
						return null;
					@SuppressWarnings("unchecked")
					var l = (ID) b.v[b.v.length - 1];
					b.v = b.vvi != null && b.vvi.hasNext() ? b.vvi.next() : null;
					return l;
				}).skip(1).takeWhile(Objects::nonNull).iterator();
				a.l = a.lli.hasNext() ? a.lli.next() : null;
				aa.add(a);
			}
			var llb = Stream.<ID>builder();
			class C {

				long l;
			}
			var c = new C();
			var t = Stream.iterate((ID) null, _ -> {
				for (;;) {
					var ll = aa.stream().map(a -> a.l).toList();
//					IO.println("ll=" + Arrays.toString(ll));
					var l = ll.stream().allMatch(Objects::nonNull) ? ll.stream().max(Comparator.naturalOrder()).get()
							: null;
					if (l == null)
						return null;
					var e = true;
					for (var a : aa) {
						while (a.l != null && a.l.compareTo(l) < 0)
							a.l = a.lli.hasNext() ? a.lli.next() : null;
						if (!a.l.equals(l))
							e = false;
					}
					if (e) {
						for (var a : aa)
							a.l = a.lli.hasNext() ? a.lli.next() : null;
						return l;
					}
				}
			}).skip(1).takeWhile(Objects::nonNull).peek(x -> {
				var o = c.l++ - skip;
				if (o >= 0 && (limit < 0 || o < limit)) {
//					IO.println("x=" + x);
					llb.add(x);
				}
			}).count();
			return new ListPortion<>(llb.build().toList(), t);
		}, false);
	}

	protected BTree<?, ?> bTree() {
		return idHelper != null ? persistence.database().index(type.getSimpleName(), "table")
				: persistence.database().table(type.getSimpleName());
	}

	protected String format(Object object) {
		IO.println("DefaultCrud.format, object=" + object);
		var s = Json.format(new CustomJsonIterator(object, persistence.converter.typeResolver()));
		IO.println("DefaultCrud.format, s=" + s);
		return s;
	}

	protected E parse(String string) {
		return parse(string, type);
	}

	protected <T> T parse(String string, Class<T> target) {
//		IO.println("DefaultCrud.parse, string=" + string + ", target=" + target);
		@SuppressWarnings("unchecked")
		var t = (T) persistence.converter().convert(Json.parse(string), target);
//		IO.println("DefaultCrud.parse, t=" + t);
		return t;
	}

	protected void updateIndexes(E entity1, E entity2) {
//		IO.println("DefaultCrud.updateIndexes, entity1=" + entity1 + ", entity2=" + entity2 + ", id=" + id);
		updateIndexes(entity1, entity2, this::getIndex);
	}

	protected void updateIndexes(E entity1, E entity2, Function<String, IndexBTree> index) {
		var im1 = entity1 != null ? getIndexMap(entity1) : null;
		var im2 = entity2 != null ? getIndexMap(entity2) : null;
		for (var k : (im1 != null ? im1 : im2).keySet()) {
			var s1 = im1 != null ? im1.get(k) : null;
			var s2 = im2 != null ? im2.get(k) : null;
			if (!Objects.equals(s1, s2)) {
//				IO.println("k=" + k);
				if (s1 != null && !s1.isEmpty() && s2 != null && !s2.isEmpty()) {
					var s = new HashSet<>(s1);
					s1.removeAll(s2);
					s2.removeAll(s);
				}
				updateIndex(index.apply(k), s1, s2, (entity1 != null ? entity1 : entity2).id());
			}
		}
	}

	protected IndexBTree getIndex(String key) {
		var n = Stream.of(type.getSimpleName(), key).filter(x -> x != null && !x.isEmpty())
				.collect(Collectors.joining("."));
		return persistence.database().index(n);
	}

	protected Map<String, Set<List<Object>>> getIndexMap(E entity) {
		return indexKeyGetters.entrySet().stream().collect(
				Collectors.toMap(x -> x.getKey(), x -> x.getValue().keys(entity), (x, _) -> x, LinkedHashMap::new));
	}

	protected void updateIndex(IndexBTree index, Set<List<Object>> remove, Set<List<Object>> add, ID id) {
//		IO.println("DefaultCrud.updateIndex, remove=" + remove + ", add=" + add);
		if (remove != null)
			for (var oo : remove)
				index.delete(
						Stream.concat(oo.stream().map(this::toDatabaseValue), Stream.of(toDatabaseId(id))).toArray(),
						null);
		if (add != null)
			for (var oo : add)
				index.insert(
						Stream.concat(oo.stream().map(this::toDatabaseValue), Stream.of(toDatabaseId(id))).toArray(),
						null);
	}

	@SuppressWarnings("unchecked")
	protected <T> T fromDatabaseValue(Object object, Class<T> target) {
//		return object == null || object instanceof Number ? object : object.toString();
		if (object == null)
			return null;
		if (target.isAssignableFrom(object.getClass()))
			return (T) object;
		if (target == Integer.class)
			return (T) Integer.valueOf(((Number) object).intValue());
		throw new RuntimeException();
	}

	protected Object toDatabaseValue(Object object) {
//		return object == null || object instanceof Number ? object : object.toString();
		if (object == null)
			return null;
		return switch (object) {
		case Integer x -> x.longValue();
		case Long x -> x;
		case Number x -> x.doubleValue();
		default -> object.toString();
		};
	}

	@SuppressWarnings("unchecked")
	protected ID fromDatabaseId(Object object) {
		return idHelper != null ? idHelper.fromDatabaseValue(object) : (ID) object;
	}

	protected Object toDatabaseId(ID id) {
		return idHelper != null ? idHelper.toDatabaseValue(id) : id;
	}

	public <T> T populate(T input, int depth) {
//		IO.println("DefaultCrud.populate, input=" + input + ", depth=" + depth);
		var pp = input != null && depth != 0 && !(input.getClass().getPackageName().startsWith("java."))
				? JavaReflect.properties(input.getClass()).filter(Property::canGet)
				: Stream.<Property>empty();

		@SuppressWarnings({ "rawtypes", "unchecked" })
		var m = pp.map(p -> {
//			IO.println("DefaultCrud.populate, p=" + p + " (" + p.type() + "), depth=" + depth);

			if (p.type() == List.class) {
				var t = p.genericType() instanceof ParameterizedType pt
						? (Class) Java.toClass(pt.getActualTypeArguments()[0])
						: null;
				var c = t != null && Entity.class.isAssignableFrom(t) ? persistence.crud(t) : null;
				var n = c != null
						? JavaReflect.properties(c.type())
								.filter(x -> x.canGet() && x.type().isAssignableFrom(input.getClass())).findFirst()
								.map(x -> x.name()).orElse(null)
						: null;
				List l;
				if (n != null)
					l = c.read(c.filter(n, new Object[] { ((Entity) input).id() }), depth - 1);
				else {
					var l1 = (List) p.get(input);
//					IO.println("DefaultCrud.populate, l1=" + l1);
					if (l1 == null)
						l = null;
					else {
						var l2 = c != null ? c.read(l1.stream().map(x -> ((Entity) x).id()).toList(), depth - 1)
								: l1.stream().map(x -> populate(x, depth)).toList();
						l = IntStream.range(0, l2.size()).anyMatch(x -> l2.get(x) != l1.get(x)) ? l2 : null;
					}
				}
				return l != null ? Java.mapEntry(p.name(), l) : null;
			}

			var v1 = p.get(input);
			var v2 = v1 instanceof Entity e ? persistence.crud((Class) e.getClass()).read(e.id(), depth - 1)
					: populate(v1, depth);
			return v2 != v1 ? Java.mapEntry(p.name(), v2) : null;
		}).filter(Objects::nonNull).collect(LinkedHashMap::new, (x, y) -> x.put(y.getKey(), y.getValue()), Map::putAll);
//		IO.println("DefaultCrud.populate, m=" + m);

		var t = !m.isEmpty() ? JavaReflect.copy(m, input) : input;
//		IO.println("DefaultCrud.populate, t=" + t);
		return t;
	}

	protected class CustomJsonIterator extends ReflectionJsonIterator {

		public CustomJsonIterator(Object object, TypeResolver typeResolver) {
			super(object, typeResolver);
		}

		@Override
		public Iterator<JsonToken<?>> newValueIterator(Object object) {
			return new CustomValueIterator(this, object);
		}
	}

	protected class CustomValueIterator extends ReflectionValueIterator {

		public CustomValueIterator(TokenIterationContext context, Object object) {
			super(context, object);
		}

		@Override
		protected Iterator<JsonToken<?>> newIterator() {
			if (value instanceof Class<?> c) {
//				var t = Modifier.isPublic(c.getModifiers()) ? c : c.getInterfaces()[0];
				var t = c;
				return context.newStringIterator(persistence.converter().convert(t, String.class));
			}
			return super.newIterator();
		}

		@Override
		protected boolean includeEntry(Property property) {
			return super.includeEntry(property) && DefaultCrud.this.includeEntry(property, this);
		}
	}

	protected boolean includeEntry(Property property, ReflectionValueIterator valueIterator) {
//		IO.println("DefaultCrud.includeEntry, property=" + property + ", (" + valueIterator.context().stack().size()
//				+ ", " + (valueIterator.value() != null ? valueIterator.value().getClass().getSimpleName() : null)
//				+ ")");

		if (property.derived())
			return false;

		if (valueIterator.value() instanceof Entity
				&& valueIterator.context().stack().size() >= referenceDepth(valueIterator)) {
			if (!property.name().equals("id"))
				return false;
		}

		return property.get(valueIterator.value()) != null;
	}

	protected int referenceDepth(ReflectionValueIterator valueIterator) {
		return 3;
	}
}

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
import com.janilla.io.IO;
import com.janilla.persistence.Persistence.IndexEntryGetter;
import com.janilla.reflect.Reflection;

public class Crud<E> {

	protected Class<E> type;

	protected Database database;

	protected Function<E, Object> formatter;

	protected Function<Object, E> parser;

	protected Map<String, IndexEntryGetter> indexEntryGetters = new HashMap<>();

	public E create(E entity) throws IOException {
		var i = database.performOnStore(type.getSimpleName(), x -> (Long) x.create(y -> {
			try {
				Reflection.setter(type, "id").invoke(entity, y);
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
		return entity;
	}

	public E read(long id) throws IOException {
		var o = database.performOnStore(type.getSimpleName(), x -> (Object) x.read(id));
		return o != null ? parser.apply(o) : null;
	}

	public Stream<E> read(long[] ids) throws IOException {
		var b = Stream.<E>builder();
		try {
			Arrays.stream(ids).mapToObj(x -> {
				try {
					return read(x);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).forEach(b::add);
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		return b.build();
	}

	public E update(long id, Consumer<E> consumer) throws IOException {
		class A {

			E e;

			Map<String, Entry<Object, Object>> m;
		}
		var a = new A();
		database.performOnStore(type.getSimpleName(), x -> {
			x.update(id, y -> {
				a.e = parser.apply(y);
				a.m = getIndexMap(a.e, id);
				consumer.accept(a.e);
				return formatter.apply(a.e);
			});
		});
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
		var o = database.performOnStore(type.getSimpleName(), x -> (Object) x.delete(id));
		var e = parser.apply(o);
		for (var f : getIndexMap(e, id).entrySet()) {
			var m = toMap(f.getValue());
			updateIndex(f.getKey(), m, null);
		}
		return e;
	}

	public long[] list() throws IOException {
		return database.performOnStore(type.getSimpleName(), s -> {
			return s.ids().toArray();
		});
	}

	public Page list(long skip, long limit) throws IOException {
		return database.performOnStore(type.getSimpleName(), s -> {
			long[] i = s.ids().skip(skip).limit(limit).toArray();
			long c = s.count();
			return new Page(i, c);
		});
	}

	public long count() throws IOException {
		return database.performOnStore(type.getSimpleName(), x -> {
			return x.count();
		});
	}

	public long count(String index, Object value) throws IOException {
		var n = type.getSimpleName() + (index != null && !index.isEmpty() ? "." + index : "");
		return database.performOnIndex(n, x -> {
			return x.count(value);
		});
	}

	public long find(String index, Object value) throws IOException {
		return performOnIndex(index, value, x -> {
			return x.findFirst().orElse(-1);
		});
	}

	public long[] filter(String index, Object... values) throws IOException {
		var n = type.getSimpleName() + (index != null && !index.isEmpty() ? "." + index : "");
		return switch (values.length) {
		case 0 -> database.performOnIndex(n, x -> {
			return x.values().mapToLong(y -> {
				var z = (Object[]) y;
				return (Long) z[z.length - 1];
			}).toArray();
		});
		case 1 -> database.performOnIndex(n, x -> {
			return x.list(values[0]).mapToLong(y -> {
				var z = (Object[]) y;
				return (Long) z[z.length - 1];
			}).toArray();
		});
		default -> {
			class A {

				Iterator<Object> i;

				Object o;
			}
			List<A> a;
			try {
				a = database.performOnIndex(n, x -> (List<A>) Arrays.stream(values).map(y -> {
					try {
						var z = new A();
						z.i = x.list(y).iterator();
						z.o = z.i.hasNext() ? z.i.next() : null;
						return z;
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}).toList());
			} catch (UncheckedIOException e) {
				throw e.getCause();
			}

			IO.LongSupplier s = () -> {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				var z = a.stream().max((x, y) -> {
					var x0 = x.o != null ? (Comparable) ((Object[]) x.o)[0] : null;
					var y0 = y.o != null ? (Comparable) ((Object[]) y.o)[0] : null;
					return x0 != null ? (y0 != null ? x0.compareTo(y0) : 1) : (y0 != null ? -1 : 0);
				}).orElse(null);
				if (z == null || z.o == null)
					return -1;
				var i = (Long) ((Object[]) z.o)[1];
				z.o = z.i.hasNext() ? z.i.next() : null;
				return i;
			};
			try {
				yield LongStream.iterate(s.getAsLong(), x -> {
					try {
						return s.getAsLong();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}).toArray();
			} catch (UncheckedIOException e) {
				throw e.getCause();
			}
		}
		};
	}

	public Page filter(String index, long skip, long limit, Object... values) throws IOException {
		var n = type.getSimpleName() + (index != null && !index.isEmpty() ? "." + index : "");
		return switch (values.length) {
		case 0 -> database.performOnIndex(n, x -> {
			var i = x.values().mapToLong(y -> {
				var z = (Object[]) y;
				return (Long) z[z.length - 1];
			}).skip(skip).limit(limit).toArray();
			var c = x.count();
			return (Page) new Page(i, c);
		});
		case 1 -> database.performOnIndex(n, x -> {
			var i = x.list(values[0]).mapToLong(y -> {
				var z = (Object[]) y;
				return (Long) z[z.length - 1];
			}).skip(skip).limit(limit).toArray();
			var c = x.count(values[0]);
			return (Page) new Page(i, c);
		});
		default -> {
			class A {

				Iterator<Object> i;

				Object o;

				long l;
			}
			List<A> a;
			try {
				a = database.performOnIndex(n, x -> (List<A>) Arrays.stream(values).map(y -> {
					try {
						var z = new A();
						z.i = x.list(y).iterator();
						z.o = z.i.hasNext() ? z.i.next() : null;
						z.l = x.count(y);
						return z;
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}).toList());
			} catch (UncheckedIOException e) {
				throw e.getCause();
			}

			IO.LongSupplier s = () -> {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				var z = a.stream().max((x, y) -> {
					var x0 = x.o != null ? (Comparable) ((Object[]) x.o)[0] : null;
					var y0 = y.o != null ? (Comparable) ((Object[]) y.o)[0] : null;
					return x0 != null ? (y0 != null ? x0.compareTo(y0) : 1) : (y0 != null ? -1 : 0);
				}).orElse(null);
				if (z == null || z.o == null)
					return -1;
				var i = (Long) ((Object[]) z.o)[1];
				z.o = z.i.hasNext() ? z.i.next() : null;
				return i;
			};
			try {
				yield new Page(LongStream.iterate(s.getAsLong(), x -> {
					try {
						return s.getAsLong();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}).skip(skip).limit(limit).filter(x -> x > 0).toArray(), a.stream().mapToLong(x -> x.l).sum());
			} catch (UncheckedIOException e) {
				throw e.getCause();
			}
		}
		};
	}

	public Page filter2(String index, long skip, long limit, Predicate<Object> operation) throws IOException {
		var n = type.getSimpleName() + (index != null && !index.isEmpty() ? "." + index : "");
		return database.performOnIndex(n, x -> {
			var i = x.valuesIf(operation).mapToLong(y -> {
				var z = (Object[]) y;
				return (Long) z[z.length - 1];
			}).skip(skip).limit(limit).toArray();
			var t = x.countIf(operation);
			return new Page(i, t);
		});
	}

	public Page filter(Map<String, Object[]> keys, long skip, long limit) throws IOException {
		var ee = keys.entrySet().stream().filter(e -> e.getValue() != null && e.getValue().length > 0).toList();
		switch (ee.size()) {
		case 0:
			return list(skip, limit);
		case 1:
			var e = ee.get(0);
			return filter(e.getKey(), skip, limit, e.getValue());
		}
		synchronized (database) {
			class A {

				PrimitiveIterator.OfLong i;

				long j;
			}
			var aa = new ArrayList<A>();
			for (var e : ee) {
				var n = type.getSimpleName() + (e.getKey() != null && !e.getKey().isEmpty() ? "." + e.getKey() : "");
				class B {

					Iterator<Object[]> i;

					Object[] o;
				}
				var bb = new ArrayList<B>();
				database.performOnIndex(n, i -> {
					for (var k : e.getValue()) {
						var b = new B();
						b.i = i.list(k).map(x -> (Object[]) x).iterator();
						b.o = b.i.hasNext() ? b.i.next() : null;
						bb.add(b);
					}
				});
				IO.LongSupplier s = () -> {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					var b = bb.stream().max((b1, b2) -> {
						var c1 = b1.o != null ? (Comparable) b1.o[0] : null;
						var c2 = b2.o != null ? (Comparable) b2.o[0] : null;
						return c1 != null ? (c2 != null ? c1.compareTo(c2) : 1) : (c2 != null ? -1 : 0);
					}).orElse(null);
					if (b == null || b.o == null)
						return 0;
					var i = (Long) b.o[b.o.length - 1];
					b.o = b.i.hasNext() ? b.i.next() : null;
					return i;
				};
				var a = new A();
				a.i = LongStream.iterate(s.getAsLong(), x -> {
					try {
						return s.getAsLong();
					} catch (IOException f) {
						throw new UncheckedIOException(f);
					}
				}).takeWhile(x -> x > 0).iterator();
				a.j = a.i.hasNext() ? a.i.nextLong() : 0;
				aa.add(a);
			}
			IO.LongSupplier s = () -> {
				for (;;) {
					var j = aa.stream().mapToLong(a -> a.j).max().getAsLong();
					if (j == 0)
						return 0;
					var e = true;
					for (var a : aa) {
						while (a.j > 0 && a.j < j)
							a.j = a.i.hasNext() ? a.i.nextLong() : 0;
						if (a.j != j)
							e = false;
					}
					if (e) {
						for (var a : aa)
							a.j = a.i.hasNext() ? a.i.nextLong() : 0;
						return j;
					}
				}
			};
			var i = LongStream.builder();
			var j = new long[1];
			var t = LongStream.iterate(s.getAsLong(), x -> {
				try {
					return s.getAsLong();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).takeWhile(x -> x > 0).peek(x -> {
				var d = j[0]++ - skip;
				if (d >= 0 && d < limit)
					i.add(x);
			}).count();
			return new Page(i.build().toArray(), t);
		}
	}

	public void performOnIndex(String name, Object value, IO.Consumer<LongStream> operation) throws IOException {
		performOnIndex(name, value, x -> {
			operation.accept(x);
			return null;
		});
	}

	public <R> R performOnIndex(String name, Object value, IO.Function<LongStream, R> operation) throws IOException {
		var n = type.getSimpleName() + (name != null && !name.isEmpty() ? "." + name : "");
		return database.performOnIndex(n, x -> {
			var l = x.list(value).mapToLong(o -> (Long) (o instanceof Object[] a ? a[a.length - 1] : o));
			return operation.apply(l);
		});
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

	public record Page(long[] ids, long total) {

		static Page EMPTY = new Page(new long[0], 0);

		public static Page empty() {
			return EMPTY;
		}
	}
}

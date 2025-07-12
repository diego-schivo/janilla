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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.janilla.database.BTree;
import com.janilla.database.Database;
import com.janilla.database.KeyAndData;
import com.janilla.io.ByteConverter;
import com.janilla.json.MapAndType;
import com.janilla.reflect.Property;
import com.janilla.reflect.Reflection;

public class Persistence {

	protected final Database database;

	protected final Set<Class<? extends Entity<?>>> types;

	protected final MapAndType.TypeResolver typeResolver;

	protected final Configuration configuration = new Configuration();

	public Persistence(Database database, Set<Class<? extends Entity<?>>> types, MapAndType.TypeResolver typeResolver) {
		this.database = database;
		this.types = types;
		this.typeResolver = typeResolver;
		for (var t : types)
			configure(t);
		createStoresAndIndexes();
	}

	public Database database() {
		return database;
	}

	public MapAndType.TypeResolver typeResolver() {
		return typeResolver;
	}

	public <K, V> com.janilla.database.Index<K, V> newIndex(KeyAndData<String> keyAndData) {
		var ni = configuration.indexFactories.get(keyAndData.key());
		if (ni == null)
			return null;
		@SuppressWarnings("unchecked")
		var i = (com.janilla.database.Index<K, V>) ni.newIndex(keyAndData, database);
		return i;
	}

	public <ID extends Comparable<ID>, E extends Entity<ID>> Crud<ID, E> crud(Class<E> class1) {
		@SuppressWarnings("unchecked")
		var c = (Crud<ID, E>) configuration.cruds.get(class1);
		return c;
	}

	protected <E extends Entity<?>, K, V> void configure(Class<E> type) {
		Crud<?, E> c = newCrud(type);
		if (c == null)
			return;
		// System.out.println("Persistence.configure, type=" + type);
		configuration.cruds.put(type, c);

		for (var oo = Stream.concat(Stream.of(type), Reflection.properties(type)).iterator(); oo.hasNext();) {
			var o = oo.next();
			var p = o instanceof Property x ? x : null;
			var ae = p != null ? p.annotatedElement() : (AnnotatedElement) o;
			var i = ae.getAnnotation(Index.class);
			if (i == null)
				continue;

			var t = p != null ? p.genericType() : null;
			var ii = new IndexFactory<K, V>();
			ii.keyConverter = keyConverter(t);
			if (ii.keyConverter == null)
				throw new NullPointerException("No keyConverter for index on " + p + " (" + t + ")");

			var s = i.sort();
			if (s.startsWith("+") || s.startsWith("-"))
				s = s.substring(1);
			var sp = !s.isEmpty() ? Reflection.property(type, s) : null;
			// System.out.println("Persistence.configure, sp=" + sp);
			if (sp != null && sp.type() != null) {
				@SuppressWarnings("unchecked")
				var h = (ByteConverter<V>) ByteConverter.of(type, i.sort(), "id");
				ii.valueConverter = h;
			} else {
				@SuppressWarnings("unchecked")
				var h = (ByteConverter<V>) ByteConverter.of(type, "id");
				ii.valueConverter = h;
			}
			var k = type.getSimpleName() + (p != null ? "." + p.name() : "");
			configuration.indexFactories.put(k, ii);

			var ieg = new IndexEntryGetter();
			BiFunction<Class<?>, String, Function<Object, Object>> kgf;
			try {
				kgf = i.keyGetter().getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			ieg.keyGetter = p != null ? kgf.apply(type, p.name()) : null;
			ieg.sortGetter = sp;
			c.indexEntryGetters.put(p != null ? p.name() : null, ieg);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		return !Modifier.isInterface(type.getModifiers()) && !Modifier.isAbstract(type.getModifiers())
				&& type.isAnnotationPresent(Store.class) ? new Crud(type, nextId(type), this) : null;
	}

	protected <ID extends Comparable<ID>> Function<Map<String, Object>, ID> nextId(Class<?> type) {
		var t = Reflection.property(type, "id").type();
		if (t == Long.class)
			return x -> {
				var v = x.get("nextId");
				var l = v != null ? (long) v : 1L;
				x.put("nextId", l + 1);
				@SuppressWarnings("unchecked")
				var id = (ID) Long.valueOf(l);
				return id;
			};
		else if (t == UUID.class)
			return _ -> {
				@SuppressWarnings("unchecked")
				var id = (ID) UUID.randomUUID();
				return id;
			};
		else
			// throw new RuntimeException(type + " " + t);
			return _ -> null;
	}

	protected void createStoresAndIndexes() {
		for (var t : configuration.cruds.keySet())
			database.perform((ss, _) -> {
				ss.create(t.getSimpleName());
				return null;
			}, true);
		for (var k : configuration.indexFactories.keySet())
			database.perform((_, ii) -> {
				ii.create(k);
				return null;
			}, true);
	}

	protected <K> ByteConverter<K> keyConverter(Type type) {
		if (type == null) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<K>) ByteConverter.NULL;
			return h;
		}
		var t = type instanceof ParameterizedType pt && Collection.class.isAssignableFrom((Class<?>) pt.getRawType())
				? pt.getActualTypeArguments()[0]
				: null;
		if (type == Boolean.class || type == Boolean.TYPE || type == boolean[].class || t == Boolean.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<K>) ByteConverter.BOOLEAN;
			return h;
		}
		if (type == Integer.class || type == Integer.TYPE || type == int[].class || t == Integer.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<K>) ByteConverter.INTEGER;
			return h;
		}
		if (type == Long.class || type == Long.TYPE || type == long[].class || t == Long.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<K>) ByteConverter.LONG;
			return h;
		}
		if (type == String.class || t == String.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<K>) ByteConverter.STRING;
			return h;
		}
		if (type == UUID.class || t == UUID.class) {
			@SuppressWarnings("unchecked")
			var h = (ByteConverter<K>) ByteConverter.UUID1;
			return h;
		}
		if ((type instanceof Class<?> c1 && c1.isEnum()) || (t instanceof Class<?> c2 && c2.isEnum())) {
			@SuppressWarnings("unchecked")
			var h = new ByteConverter<K>() {

				@Override
				public byte[] serialize(K element) {
					return STRING.serialize(element.toString());
				}

				@Override
				public int getLength(ByteBuffer buffer) {
					return STRING.getLength(buffer);
				}

				@Override
				public K deserialize(ByteBuffer buffer) {
					@SuppressWarnings("rawtypes")
					var ec = (Class) type;
					return (K) Enum.valueOf(ec, STRING.deserialize(buffer));
				}

				@Override
				public int compare(ByteBuffer buffer, K element) {
					return STRING.compare(buffer, element.toString());
				}
			};
			return h;
		}
		return null;
	}

	protected static class Configuration {

		protected Map<Class<?>, Crud<?, ?>> cruds = new HashMap<>();

		protected Map<String, IndexFactory<?, ?>> indexFactories = new HashMap<>();

		public Map<Class<?>, Crud<?, ?>> cruds() {
			return cruds;
		}

		public Map<String, IndexFactory<?, ?>> indexFactories() {
			return indexFactories;
		}
	}

	protected static class IndexFactory<K, V> {

		protected ByteConverter<K> keyConverter;

		protected ByteConverter<V> valueConverter;

		protected com.janilla.database.Index<K, V> newIndex(KeyAndData<String> keyAndData, Database database) {
			return new com.janilla.database.Index<K, V>(new BTree<>(database.bTreeOrder(), database.channel(),
					database.memory(), KeyAndData.getByteConverter(keyConverter), keyAndData.bTree()), valueConverter);
		}
	}

	protected static class IndexEntryGetter {

		protected Function<Object, Object> keyGetter;

		protected Property sortGetter;

		protected Map.Entry<Object, Object> getIndexEntry(Object entity, Comparable<?> id)
				throws ReflectiveOperationException {
			var k = keyGetter != null ? keyGetter.apply(entity) : null;
			var v = sortGetter != null ? new Object[] { sortGetter.get(entity), id } : new Object[] { id };
			return keyGetter == null || k != null ? new AbstractMap.SimpleImmutableEntry<>(k, v) : null;
		}
	}
}

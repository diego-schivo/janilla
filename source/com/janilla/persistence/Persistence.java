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
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.janilla.database.BTree;
import com.janilla.database.Database;
import com.janilla.database.KeyAndData;
import com.janilla.database.NameAndData;
import com.janilla.io.ByteConverter;
import com.janilla.json.MapAndType;
import com.janilla.reflect.Property;
import com.janilla.reflect.Reflection;

public class Persistence {

	protected final Database database;

	protected final Iterable<Class<?>> types;

	protected final MapAndType.TypeResolver typeResolver;

	protected final Configuration configuration = new Configuration();

	public Persistence(Database database, Iterable<Class<?>> types, MapAndType.TypeResolver typeResolver) {
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

	public <K, V> com.janilla.database.Index<K, V> newIndex(NameAndData nameAndData) {
		var ni = configuration.indexFactories.get(nameAndData.name());
		if (ni == null)
			return null;
		@SuppressWarnings("unchecked")
		var i = (com.janilla.database.Index<K, V>) ni.newIndex(nameAndData, database);
		return i;
	}

	public <E> Crud<E> crud(Class<E> class1) {
		@SuppressWarnings("unchecked")
		var c = (Crud<E>) configuration.cruds.get(class1);
		return c;
	}

	protected <E, K, V> void configure(Class<E> type) {
		Crud<E> c = newCrud(type);
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

			var t = p != null ? p.type() : null;
			var ii = new IndexFactory<K, V>();
			if (t == null) {
				@SuppressWarnings("unchecked")
				var h = (ByteConverter<K>) ByteConverter.NULL;
				ii.keyConverter = h;
			} else if (t == Boolean.class || t == Boolean.TYPE || t == boolean[].class) {
				@SuppressWarnings("unchecked")
				var h = (ByteConverter<K>) ByteConverter.BOOLEAN;
				ii.keyConverter = h;
			} else if (t == Integer.class || t == Integer.TYPE || t == int[].class) {
				@SuppressWarnings("unchecked")
				var h = (ByteConverter<K>) ByteConverter.INTEGER;
				ii.keyConverter = h;
			} else if (t == Long.class || t == Long.TYPE || t == long[].class) {
				@SuppressWarnings("unchecked")
				var h = (ByteConverter<K>) ByteConverter.LONG;
				ii.keyConverter = h;
			} else if (t == String.class || t == Collection.class) {
				@SuppressWarnings("unchecked")
				var h = (ByteConverter<K>) ByteConverter.STRING;
				ii.keyConverter = h;
			} else if (t == UUID.class) {
				@SuppressWarnings("unchecked")
				var h = (ByteConverter<K>) ByteConverter.UUID1;
				ii.keyConverter = h;
			} else if (t.isEnum()) {
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
						var ec = (Class) t;
						return (K) Enum.valueOf(ec, STRING.deserialize(buffer));
					}

					@Override
					public int compare(ByteBuffer buffer, K element) {
						return STRING.compare(buffer, element.toString());
					}
				};
				ii.keyConverter = h;
			} else
				throw new RuntimeException();

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

	protected <E> Crud<E> newCrud(Class<E> type) {
		return Modifier.isInterface(type.getModifiers()) || Modifier.isAbstract(type.getModifiers())
				|| !type.isAnnotationPresent(Store.class) ? null : new Crud<>(type, this);
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

	static class Configuration {

		Map<String, IndexFactory<?, ?>> indexFactories = new HashMap<>();

		Map<Class<?>, Crud<?>> cruds = new HashMap<>();
	}

	static class IndexFactory<K, V> {

		ByteConverter<K> keyConverter;

		ByteConverter<V> valueConverter;

		com.janilla.database.Index<K, V> newIndex(NameAndData nameAndData, Database database) {
			return new com.janilla.database.Index<K, V>(new BTree<>(database.bTreeOrder(), database.channel(),
					database.memory(), KeyAndData.getByteConverter(keyConverter), nameAndData.bTree()), valueConverter);
		}
	}

	static class IndexEntryGetter {

		Function<Object, Object> keyGetter;

		Property sortGetter;

		Map.Entry<Object, Object> getIndexEntry(Object entity, long id) throws ReflectiveOperationException {
			var k = keyGetter != null ? keyGetter.apply(entity) : null;
			var v = sortGetter != null ? new Object[] { sortGetter.get(entity), id } : new Object[] { id };
			return keyGetter == null || k != null ? new AbstractMap.SimpleEntry<>(k, v) : null;
		}
	}
}

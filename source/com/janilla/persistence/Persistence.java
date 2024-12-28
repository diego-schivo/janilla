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

import com.janilla.database.Database;
import com.janilla.io.ElementHelper;
import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.reflect.Property;
import com.janilla.reflect.Reflection;

public class Persistence {

	protected Database database;

	protected Configuration configuration;

	Function<String, Class<?>> typeResolver;

	public Database database() {
		return database;
	}

	public void setTypeResolver(Function<String, Class<?>> typeResolver) {
		this.typeResolver = typeResolver;
	}

	public <K, V> boolean initializeIndex(String name, com.janilla.database.Index<K, V> index) {
		var i = configuration.indexInitializers.get(name);
		if (i == null)
			return false;
		@SuppressWarnings("unchecked")
		var j = (IndexInitializer<K, V>) i;
		j.initialize(index);
		return true;
	}

	public <E> Crud<E> crud(Class<E> class1) {
		@SuppressWarnings("unchecked")
		var c = (Crud<E>) configuration.cruds.get(class1);
		return c;
	}

	protected void createStoresAndIndexes() {
		for (var t : configuration.cruds.keySet())
			database.perform((ss, ii) -> {
				ss.create(t.getSimpleName());
				return null;
			}, true);
		for (var k : configuration.indexInitializers.keySet())
			database.perform((ss, ii) -> {
				ii.create(k);
				return null;
			}, true);
	}

	protected <E> Crud<E> createCrud(Class<E> type) {
		return Modifier.isInterface(type.getModifiers()) || Modifier.isAbstract(type.getModifiers())
				|| !type.isAnnotationPresent(Store.class) ? null : new Crud<>();
	}

	<E, K, V> void configure(Class<E> type) {
		Crud<E> c = createCrud(type);
		if (c == null)
			return;
		// System.out.println("Persistence.configure, type=" + type);
		c.type = type;
		c.database = database;
		if (c.formatter == null)
			c.formatter = x -> {
				var tt = new ReflectionJsonIterator();
				tt.setObject(x);
				tt.setIncludeType(true);
				return Json.format(tt);
			};
		if (c.parser == null)
			c.parser = x -> {
				var c2 = new Converter();
				c2.setResolver(y -> y.map().containsKey("$type")
						? new Converter.MapType(y.map(), typeResolver.apply((String) y.map().get("$type")))
						: null);
				@SuppressWarnings("unchecked")
				var e = (E) c2.convert(Json.parse((String) x), type);
				return e;
			};
		c.indexPresent = type.isAnnotationPresent(Index.class);
		configuration.cruds.put(type, c);

		var oi = Stream.concat(Stream.of(type), Reflection.properties(type)).iterator();
		while (oi.hasNext()) {
			var o = oi.next();
			var p = o instanceof Property x ? x : null;
			var ae = p != null ? p.annotatedElement() : (AnnotatedElement) o;
			var i = ae.getAnnotation(Index.class);
			if (i == null)
				continue;

//			var n = p != null ? p.name() : null;
			// System.out.println("Persistence.configure, n=" + n);
			var t = p != null ? p.type() : null;
			var ii = new IndexInitializer<K, V>();
			if (t == null) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.NULL;
				ii.keyHelper = h;
			} else if (t == Boolean.class || t == Boolean.TYPE || t == boolean[].class) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.BOOLEAN;
				ii.keyHelper = h;
			} else if (t == Integer.class || t == Integer.TYPE || t == int[].class) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.INTEGER;
				ii.keyHelper = h;
			} else if (t == Long.class || t == Long.TYPE || t == long[].class) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.LONG;
				ii.keyHelper = h;
			} else if (t == String.class || t == Collection.class) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.STRING;
				ii.keyHelper = h;
			} else if (t == UUID.class) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<K>) ElementHelper.UUID1;
				ii.keyHelper = h;
			} else if (t.isEnum()) {
				@SuppressWarnings("unchecked")
				var h = new ElementHelper<K>() {

					@Override
					public byte[] getBytes(K element) {
						return STRING.getBytes(element.toString());
					}

					@Override
					public int getLength(ByteBuffer buffer) {
						return STRING.getLength(buffer);
					}

					@Override
					public K getElement(ByteBuffer buffer) {
						@SuppressWarnings("rawtypes")
						var ec = (Class) t;
						return (K) Enum.valueOf(ec, STRING.getElement(buffer));
					}

					@Override
					public int compare(ByteBuffer buffer, K element) {
						return STRING.compare(buffer, element.toString());
					}
				};
				ii.keyHelper = h;
			} else
				throw new RuntimeException();

			var s = i.sort();
			if (s.startsWith("+") || s.startsWith("-"))
				s = s.substring(1);
			var sp = !s.isEmpty() ? Reflection.property(type, s) : null;
			// System.out.println("Persistence.configure, sp=" + sp);
			if (sp != null && sp.type() != null) {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<V>) ElementHelper.of(type, i.sort(), "id");
				ii.valueHelper = h;
			} else {
				@SuppressWarnings("unchecked")
				var h = (ElementHelper<V>) ElementHelper.of(type, "id");
				ii.valueHelper = h;
			}
			var k = type.getSimpleName() + (p != null ? "." + p.name() : "");
			configuration.indexInitializers.put(k, ii);

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

	static class Configuration {

		Map<String, IndexInitializer<?, ?>> indexInitializers = new HashMap<>();

		Map<Class<?>, Crud<?>> cruds = new HashMap<>();
	}

	static class IndexInitializer<K, V> {

		ElementHelper<K> keyHelper;

		ElementHelper<V> valueHelper;

		void initialize(com.janilla.database.Index<K, V> index) {
			index.setKeyHelper(keyHelper);
			index.setValueHelper(valueHelper);
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

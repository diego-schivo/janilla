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
package com.janilla.cms;

import java.lang.reflect.Modifier;

import com.janilla.database.BTree;
import com.janilla.database.Database;
import com.janilla.database.Index;
import com.janilla.database.KeyAndData;
import com.janilla.database.NameAndData;
import com.janilla.io.ByteConverter;
import com.janilla.json.MapAndType.TypeResolver;
import com.janilla.persistence.Crud;
import com.janilla.persistence.Persistence;
import com.janilla.persistence.Store;

public class VersionsPersistence extends Persistence {

	public VersionsPersistence(Database database, Iterable<Class<?>> types, TypeResolver typeResolver) {
		super(database, types, typeResolver);
	}

	@Override
	public <K, V> Index<K, V> newIndex(NameAndData nameAndData) {
		if (nameAndData.name().startsWith(Version.class.getSimpleName() + "<")
				&& nameAndData.name().endsWith(">.document")) {
			@SuppressWarnings("unchecked")
			var i = (Index<K, V>) new Index<Long, Object[]>(
					new BTree<>(database.bTreeOrder(), database.channel(), database.memory(),
							KeyAndData.getByteConverter(ByteConverter.LONG), nameAndData.bTree()),
					ByteConverter.of(Version.class, "-updatedAt", "id"));
			return i;
		}
		return super.newIndex(nameAndData);
	}

	@Override
	protected <E, K, V> void configure(Class<E> type) {
		super.configure(type);
		var v = type.getAnnotation(Versions.class);
		if (v != null && v.drafts())
			for (var k : configuration.indexFactories().keySet().toArray(String[]::new))
				if (k.equals(type.getSimpleName()) || k.startsWith(type.getSimpleName() + "."))
					configuration.indexFactories().put(k + "Draft", configuration.indexFactories().get(k));
	}

	@Override
	protected <E> Crud<E> newCrud(Class<E> type) {
		if (!Modifier.isInterface(type.getModifiers()) && !Modifier.isAbstract(type.getModifiers())
				&& type.isAnnotationPresent(Store.class)) {
			@SuppressWarnings("unchecked")
			var t = (Class<? extends Document>) type;
			@SuppressWarnings({ "unchecked", "rawtypes" })
			var c = (Crud<E>) new DocumentCrud(t, this, type.isAnnotationPresent(Versions.class));
			return c;
		}
		return null;
	}

	@Override
	protected void createStoresAndIndexes() {
		super.createStoresAndIndexes();
//		var nn = new HashSet<String>();
		for (var t : configuration.cruds().keySet()) {
//			var v = t.getAnnotation(Versions.class);
			if (t.isAnnotationPresent(Versions.class)) {
				database.perform((ss, ii) -> {
					var n = Version.class.getSimpleName() + "<" + t.getSimpleName() + ">";
					ss.create(n);
					ii.create(n + ".document");
					return null;
				}, true);
//				if (v.drafts())
//					nn.add(t.getSimpleName());
			}
		}
//		for (var k : configuration.indexFactories().keySet()) {
//			var i = k.indexOf('.');
//			var n = i != -1 ? k.substring(0, i) : k;
//			if (nn.contains(n))
//				database.perform((_, ii) -> {
//					ii.create(k + "Draft");
//					return null;
//				}, true);
//		}
	}
}

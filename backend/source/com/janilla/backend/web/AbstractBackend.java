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
package com.janilla.backend.web;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.persistence.PersistenceBuilder;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Converter;
import com.janilla.java.DollarTypeResolver;
import com.janilla.java.TypeResolver;
import com.janilla.persistence.Store;
import com.janilla.web.AbstractApp;
import com.janilla.web.InvocationResolver;

public abstract class AbstractBackend extends AbstractApp {

	protected Converter converter;

	protected Persistence persistence;

	protected List<Class<?>> resolvables;

	protected List<Class<?>> storables;

	protected TypeResolver typeResolver;

	protected AbstractBackend(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey);
	}

	public Converter converter() {
		return converter;
	}

	public Persistence persistence() {
		return persistence;
	}

	public List<Class<?>> resolvables() {
		return resolvables;
	}

	public List<Class<?>> storables() {
		return storables;
	}

	public TypeResolver typeResolver() {
		return typeResolver;
	}

	@Override
	protected InvocationResolver newInvocationResolver() {
		{
			Map<String, Class<?>> m = diFactory.types().stream()
					.collect(Collectors.toMap(x -> x.getSimpleName(), x -> x, (_, x) -> x, LinkedHashMap::new));
			resolvables = m.values().stream().toList();
		}
		typeResolver = diFactory.newInstance(diFactory.classFor(DollarTypeResolver.class));
		converter = diFactory.newInstance(diFactory.classFor(Converter.class));

		storables = resolvables.stream().filter(x -> x.isAnnotationPresent(Store.class)).toList();
		{
			var f = configuration.getProperty(configurationKey + ".database.file");
			if (f.startsWith("~"))
				f = System.getProperty("user.home") + f.substring(1);
			var b = diFactory.newInstance(diFactory.classFor(PersistenceBuilder.class),
					Map.of("databaseFile", Path.of(f)));
			persistence = b.build(diFactory);
		}
		return super.newInvocationResolver();
	}
}

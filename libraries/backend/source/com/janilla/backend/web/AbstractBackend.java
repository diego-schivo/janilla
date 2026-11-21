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

import java.lang.reflect.AnnotatedElement;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.persistence.PersistenceBuilder;
import com.janilla.ioc.DiFactory;
import com.janilla.java.JavaReflect;
import com.janilla.persistence.Store;
import com.janilla.web.AbstractWebApp;
import com.janilla.web.Domain;
import com.janilla.web.InvocationResolver;

public abstract class AbstractBackend<C extends BackendConfig, D extends Domain> extends AbstractWebApp<C, D>
		implements Backend<C, D> {

	protected Persistence persistence;

	protected List<Class<?>> storables;

	protected AbstractBackend(C config, DiFactory diFactory, Consumer<Object> context) {
		super(config, diFactory, context);
	}

	public Persistence persistence() {
		return persistence;
	}

	public List<Class<?>> storables() {
		return storables;
	}

	@Override
	protected InvocationResolver newInvocationResolver() {
		{
			Map<AnnotatedElement, Class<?>> m = resolvables.stream().map(x -> {
				var ae = JavaReflect.inheritedAnnotation(x, Store.class);
				var e = ae != null ? Map.entry(ae.annotated(), x) : null;
				return e;
			}).filter(x -> x != null)
					.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue(), (_, x) -> x, LinkedHashMap::new));
			storables = m.values().stream().toList();
		}

		{
			var f = ((BackendConfig) config).database().file();
			if (f.startsWith("~"))
				f = System.getProperty("user.home") + f.substring(1);
			var b = diFactory.newInstance(diFactory.classFor(PersistenceBuilder.class),
					Map.of("databaseFile", Path.of(f)));
			persistence = b.build(diFactory);
		}

		return super.newInvocationResolver();
	}
}

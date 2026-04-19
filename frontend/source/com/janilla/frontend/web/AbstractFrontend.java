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
package com.janilla.frontend.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.janilla.frontend.IndexFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.web.AbstractWebApp;
import com.janilla.web.InvocationResolver;
import com.janilla.web.ResourceMap;

public abstract class AbstractFrontend extends AbstractWebApp {

	protected IndexFactory indexFactory;

	protected ResourceMap resourceMap;

	protected Map<String, String> resourcePrefixes;

	protected AbstractFrontend(DiFactory diFactory, Path configurationFile, String configurationKey) {
		super(diFactory, configurationFile, configurationKey);
	}

	public IndexFactory indexFactory() {
		return indexFactory;
	}

	public ResourceMap resourceMap() {
		return resourceMap;
	}

	public Map<String, String> resourcePrefixes() {
		return resourcePrefixes;
	}

	@Override
	protected InvocationResolver newInvocationResolver() {
		{
			resourcePrefixes = new LinkedHashMap<>();
			putResourcePrefixes();
			resourceMap = diFactory.newInstance(diFactory.classFor(ResourceMap.class), Map.of("paths",
					resourcePrefixes.entrySet().stream().reduce(new HashMap<String, List<Path>>(), (x, y) -> {
						x.computeIfAbsent(y.getValue(), _ -> new ArrayList<>())
								.addAll(Java.getPackagePaths(y.getKey()).filter(Files::isRegularFile).toList());
						return x;
					}, (_, x) -> x)));
		}
		indexFactory = diFactory.newInstance(diFactory.classFor(IndexFactory.class));
		return super.newInvocationResolver();
	}

	protected void putResourcePrefixes() {
		resourcePrefixes.put("com.janilla.frontend", "/base");
	}
}

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.janilla.frontend.IndexFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.web.AbstractWebApp;
import com.janilla.web.AnnotatedValue;
import com.janilla.web.Domain;
import com.janilla.web.HtmlEvaluator;
import com.janilla.web.HtmlRenderer;
import com.janilla.web.InvocationResolver;
import com.janilla.web.PackageResourcesProvider;
import com.janilla.web.ResourceMap;
import com.janilla.web.ResourcesProvider;

public abstract class AbstractFrontend<C extends FrontendConfig, D extends Domain> extends AbstractWebApp<C, D>
		implements Frontend<C, D> {

	protected final HtmlEvaluator htmlEvaluator = new HtmlEvaluator() {

		@Override
		public String evaluate(AnnotatedValue input, String expression, Consumer<Object> context,
				HtmlRenderer<?> renderer) {
			var x = super.evaluate(input, expression, context, renderer);
			if (x.isEmpty() && expression.equals("basePath"))
				x = config.basePath();
//			IO.println("expression=" + expression + ", x=" + x);
			return x;
		}
	};

	protected IndexFactory indexFactory;

	protected ResourceMap resourceMap;

	protected Map<ResourcesProvider, String> resourcesProviders;

	protected AbstractFrontend(C config, DiFactory diFactory, Consumer<Object> consumer) {
		super(config, diFactory, consumer);
	}

	public HtmlEvaluator htmlEvaluator() {
		return htmlEvaluator;
	}

	public IndexFactory indexFactory() {
		return indexFactory;
	}

	public ResourceMap resourceMap() {
		return resourceMap;
	}

	public Map<ResourcesProvider, String> resourcesProviders() {
		return resourcesProviders;
	}

	@Override
	protected InvocationResolver newInvocationResolver() {
		{
			resourcesProviders = new LinkedHashMap<>();
			putResourcePrefixes();

			var pp = resourcesProviders.entrySet().stream().reduce(new HashMap<String, List<ResourcesProvider>>(),
					(m, e) -> {
						var p = e.getKey();
						var bp = e.getValue();
						m.computeIfAbsent(bp, _ -> new ArrayList<>()).add(p);
						return m;
					}, (m1, m2) -> {
						m1.putAll(m2);
						return m1;
					});
			resourceMap = diFactory.newInstance(diFactory.classFor(ResourceMap.class), Map.of("providers", pp));
		}
		indexFactory = diFactory.newInstance(diFactory.classFor(IndexFactory.class));
		return super.newInvocationResolver();
	}

	protected void putResourcePrefixes() {
		resourcesProviders.put(new PackageResourcesProvider("com.janilla.frontend"), "/base");
	}
}

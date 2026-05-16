/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.blanktemplate.frontend;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.janilla.ioc.DiFactory;
import com.janilla.java.AnnotationAndElement;
import com.janilla.web.DefaultRenderableFactory;
import com.janilla.web.PackageResourcesProvider;
import com.janilla.web.Render;
import com.janilla.web.ResourceMap;
import com.janilla.web.ResourcesProvider;

public class BlankRenderableFactory extends DefaultRenderableFactory {

	protected final Map<ResourcesProvider, String> resourcesProviders;

	protected final Map<String, String> foo = new ConcurrentHashMap<>();

	public BlankRenderableFactory(ResourceMap resourceMap, DiFactory diFactory,
			Map<ResourcesProvider, String> resourcesProviders) {
		super(resourceMap, diFactory);
		this.resourcesProviders = resourcesProviders;
	}

	@Override
	protected Stream<String> resourceKeys(AnnotationAndElement<Render> render) {
		return super.resourceKeys(render).map(x -> {
			String k;
			if (x.startsWith("/"))
				k = x;
			else {
				var bp = foo.computeIfAbsent(((Class<?>) render.annotated()).getPackageName(),
						y -> resourcesProviders.get(new PackageResourcesProvider(y)));
				k = bp + "/" + x;
			}
//			IO.println("BlankRenderableFactory.resourceKeys, x=" + x + ", y=" + y);
			return k;
		});
	}
}

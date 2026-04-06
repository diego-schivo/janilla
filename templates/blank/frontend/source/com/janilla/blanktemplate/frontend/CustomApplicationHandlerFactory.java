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

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.janilla.http.HttpHandlerFactory;
import com.janilla.ioc.DiFactory;
import com.janilla.web.ApplicationHandlerFactory;
import com.janilla.web.ResourceHandlerFactory;

public class CustomApplicationHandlerFactory extends ApplicationHandlerFactory {

	public CustomApplicationHandlerFactory(DiFactory diFactory) {
		super(diFactory);
	}

	@Override
	protected List<HttpHandlerFactory> buildFactories() {
		return super.buildFactories().stream().flatMap(
				x -> x instanceof ResourceHandlerFactory ? Stream.of(x, buildDownloadHandlerFactory()) : Stream.of(x))
				.toList();
	}

	protected DownloadHandlerFactory buildDownloadHandlerFactory() {
		return Objects.requireNonNull(diFactory.newInstance(diFactory.classFor(DownloadHandlerFactory.class)));
	}
}

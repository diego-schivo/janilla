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

import com.janilla.blanktemplate.BlankDomain;
import com.janilla.frontend.Index;
import com.janilla.frontend.IndexFactory;
import com.janilla.web.Handle;

public class BlankWeb<D extends BlankDomain, C extends BlankApiClient> {

	protected final D domain;

	protected final C apiClient;

	protected final IndexFactory indexFactory;

	public BlankWeb(IndexFactory indexFactory, D domain, C apiClient) {
		this.indexFactory = indexFactory;
		this.domain = domain;
		this.apiClient = apiClient;
	}

	@Handle(method = "GET", path = "/admin(/[\\w\\d/-]*)?")
	public Index admin(String path) {
		return indexFactory.newIndex();
	}

	@Handle(method = "GET", path = "/")
	public Index home() {
		return indexFactory.newIndex();
	}
}

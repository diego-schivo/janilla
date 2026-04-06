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
package com.janilla.blanktemplate.backend;

import java.io.IOException;
import java.nio.file.Files;

import com.janilla.backend.cms.Cms;
import com.janilla.backend.cms.CmsResourceHandling;
import com.janilla.http.HttpRequest;
import com.janilla.web.Handle;

@Handle(path = "/api/files")
public class FileApi {

	protected final CmsResourceHandling handling;

	public FileApi(CmsResourceHandling cmsResourceHandling) {
		this.handling = cmsResourceHandling;
	}

	@Handle(method = "POST", path = "upload")
	public void upload(HttpRequest request) throws IOException {
		var ff = Cms.files(request);
		for (var nbb : ff.entrySet()) {
			var n = nbb.getKey();
			var bb = nbb.getValue();

			var f = handling.directory().resolve(n);
			Files.write(f, bb);
		}
	}
}

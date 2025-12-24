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
 * Note that authoring this file involved dealing in other programs that are
 * provided under the following license:
 *
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
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
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.cms;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import com.janilla.http.HttpHandler;
import com.janilla.http.HttpRequest;
import com.janilla.http.HttpResponse;
import com.janilla.web.FileHandlerFactory;

public abstract class CmsFileHandlerFactory implements FileHandlerFactory {

	protected final Path directory;

	public CmsFileHandlerFactory(Path directory) {
		this.directory = directory;
	}

	@Override
	public HttpHandler createHandler(Object object) {
		var p = object instanceof HttpRequest x ? x.getPath() : null;
		var n = p != null && p.startsWith("/api/images/") ? p.substring("/api/images/".length()) : null;
		if (n == null)
			return null;
		var f = directory.resolve(n);
		return Files.exists(f) ? x -> handle(f, x.response()) : null;
	}

	public static boolean handle(Path file, HttpResponse response) {
		response.setStatus(200);
		response.setHeaderValue("cache-control", "max-age=3600");
		var n = file.getFileName().toString();
		switch (n.substring(n.lastIndexOf('.') + 1)) {
		case "ico":
			response.setHeaderValue("content-type", "image/x-icon");
			break;
		case "svg":
			response.setHeaderValue("content-type", "image/svg+xml");
			break;
		}
		try {
			response.setHeaderValue("content-length", String.valueOf(Files.size(file)));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		try (var in = Files.newInputStream(file);
				var out = Channels.newOutputStream((WritableByteChannel) response.getBody())) {
			in.transferTo(out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return true;
	}
}

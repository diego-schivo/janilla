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
package com.janilla.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;

import com.janilla.java.Java;

public record ZipEntryResource(Resource archive, String path, long size) implements Resource {

	@Override
	public URI uri() {
		return URI.create("jar:" + archive.uri() + "!" + path);
	}

	@Override
	public InputStream newInputStream() {
		URI u;
		{
			u = archive.uri();
			var s = u.toString();
			if (!s.startsWith("jar:"))
				u = URI.create("jar:" + s);
		}

		var f = Java.zipFileSystem(u).getPath(path);
		try {
			return Files.newInputStream(f);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

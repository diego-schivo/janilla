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
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.zip;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Zip {

	private static final Map<String, FileSystem> ZIP_FILE_SYSTEMS = new ConcurrentHashMap<>();

	private Zip() {
		throw new Error("no instances");
	}

	public static FileSystem zipFileSystem(URI uri) {
		return ZIP_FILE_SYSTEMS.computeIfAbsent(uri.toString(), k -> {
			var i = k.lastIndexOf('!');
			if (i == -1)
				try {
					return FileSystems.getFileSystem(uri);
				} catch (FileSystemNotFoundException _) {
					try {
						return FileSystems.newFileSystem(uri, Map.of());
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			var p = zipFileSystem(URI.create(k.substring(0, i))).getPath(k.substring(i + 1));
			try {
				return FileSystems.newFileSystem(p, Map.of());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}
}

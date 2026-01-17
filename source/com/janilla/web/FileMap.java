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
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.janilla.java.Java;

public class FileMap extends HashMap<String, File> {

	private static final long serialVersionUID = -334340699116362752L;

	protected static final Set<String> EXTENSIONS = Set.of("avif", "css", "html", "ico", "jpg", "js", "png", "svg",
			"ttf", "webp", "woff", "woff2");

	public FileMap(List<Path> paths) {
//		IO.println("FileMap, paths=" + paths);
		for (var x : paths) {
//			IO.println("x.getFileSystem()=" + (x.getFileSystem() == FileSystems.getDefault()));
			var d = x.getFileSystem() == FileSystems.getDefault()
					? Stream.iterate(x, y -> y.getParent())
							.dropWhile(y -> !y.getFileName().toString().equals("classes")).findFirst().get()
					: x.getFileSystem().getRootDirectories().iterator().next();
			x = d.relativize(x);
			try {
				var n0 = x.getFileName().toString();
				var i = n0.lastIndexOf('.');
				var ex = i != -1 ? n0.substring(i + 1).toLowerCase() : null;

				class A {
					private static DefaultFile moduleUri(String name) {
						if (Java.class.getModule().isNamed())
							for (var m : ModuleLayer.boot().configuration().modules())
								try (var r = m.reference().open()) {
									var u = r.find(name);
									if (u.isPresent()) {
										return new DefaultFile(ModuleLayer.boot().findModule(m.name()).get(), u.get(),
												null, null, 0);
									}
								} catch (IOException e) {
									throw new UncheckedIOException(e);
								}
						else {
							var x = Thread.currentThread().getContextClassLoader().getResource(name);
							if (x != null)
								try {
									return new DefaultFile(null, x.toURI(), null, null, 0);
								} catch (URISyntaxException e) {
									throw new RuntimeException(e);
								}
						}
						return null;
					}
				}

				if (ex == null)
					;
				else if (EXTENSIONS.contains(ex)) {
					var p = x.getParent().toString().replace(java.io.File.separatorChar, '/').replace('/', '.');
					var n = x.toString().replace(java.io.File.separatorChar, '/');
					var mu = A.moduleUri(n);
					var f = d.resolve(x);
					var r = new DefaultFile(mu.module(), mu.uri(), p, "/" + n, Files.size(f));
//					IO.println("r=" + r);
					put(r.path().substring(r.package1().length() + 1), r);
				} else if (ex.equals("zip")) {
					var p = x.getParent().toString().replace(java.io.File.separatorChar, '/').replace('/', '.');
					var n = x.toString().replace(java.io.File.separatorChar, '/');
					var mu = A.moduleUri(n);
					var u = mu.uri();
					if (!u.toString().startsWith("jar:"))
						u = URI.create("jar:" + u);
					var fs = Java.zipFileSystem(u);
					var f = d.resolve(x);
					var a = new DefaultFile(mu.module(), u, p, "/" + n, Files.size(f));
					Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<>() {

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//							IO.println("file=" + file);
							var n = file.getFileName().toString();
							var i = n.lastIndexOf('.');
							var ex = i != -1 ? n.substring(i + 1).toLowerCase() : null;
							if (ex != null && EXTENSIONS.contains(ex)) {
								var r = new ZipEntryFile(a, file.toString(), Files.size(file));
//								IO.println("r=" + r);
								put(r.archive().path().substring(0, r.archive().path().length() - 4)
										.substring(r.archive().package1().length() + 1) + r.path(), r);
							}
							return FileVisitResult.CONTINUE;
						}
					});
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
//		IO.println("FileMap=" + this);
	}
}

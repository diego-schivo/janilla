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

import java.io.File;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.janilla.java.Java;

public class ResourceMap extends HashMap<String, Resource> {

	private static final long serialVersionUID = -334340699116362752L;

	protected static final Set<String> EXTENSIONS = Set.of("avif", "css", "html", "ico", "jpg", "js", "png", "svg",
			"ttf", "webp", "woff", "woff2");

	public ResourceMap(Map<String, List<Path>> paths) {
//		IO.println("ResourceMap, paths=" + paths);
		for (var kv : paths.entrySet()) {
			var k = kv.getKey();
			var pp = kv.getValue();
			for (var p : pp) {
				var d = p.getFileSystem() == FileSystems.getDefault()
						? Stream.iterate(p, x -> x.getParent())
								.dropWhile(x -> !x.getFileName().toString().equals("classes")).findFirst().get()
						: p.getFileSystem().getRootDirectories().iterator().next();
				p = d.relativize(p);
//			IO.println("ResourceMap, p=" + p);
				try {
					String ex;
					{
						var n = p.getFileName().toString();
						var i = n.lastIndexOf('.');
						ex = i != -1 ? n.substring(i + 1).toLowerCase() : null;
					}
					if (ex == null)
						continue;

					class A {
						private static DefaultResource moduleAndUri(String name) {
							if (Java.class.getModule().isNamed())
								for (var m : ModuleLayer.boot().configuration().modules())
									try (var r = m.reference().open()) {
										var u = r.find(name);
										if (u.isPresent()) {
											return new DefaultResource(ModuleLayer.boot().findModule(m.name()).get(),
													u.get(), null, null, 0);
										}
									} catch (IOException e) {
										throw new UncheckedIOException(e);
									}
							else {
								var x = Thread.currentThread().getContextClassLoader().getResource(name);
								if (x != null)
									try {
										return new DefaultResource(null, x.toURI(), null, null, 0);
									} catch (URISyntaxException e) {
										throw new RuntimeException(e);
									}
							}
							return null;
						}
					}

					if (EXTENSIONS.contains(ex)) {
						var p2 = p.getParent().toString().replace(File.separatorChar, '/').replace('/', '.');
						var n = p.toString().replace(File.separatorChar, '/');
						var mu = A.moduleAndUri(n);
						var f = d.resolve(p);
						var r = new DefaultResource(mu.module(), mu.uri(), p2, "/" + n, Files.size(f));
//					IO.println("r=" + r);
						var k2 = r.path().substring(r.package1().length() + 1);
						if (k != null && !k.isEmpty())
							k2 = k + k2;
						put(k2, r);
					} else if (ex.equals("zip")) {
						var p2 = p.getParent().toString().replace(File.separatorChar, '/').replace('/', '.');
						var n = p.toString().replace(File.separatorChar, '/');
						var mu = A.moduleAndUri(n);
						var u = mu.uri();
						if (!u.toString().startsWith("jar:"))
							u = URI.create("jar:" + u);
						var fs = Java.zipFileSystem(u);
						var f = d.resolve(p);
						var a = new DefaultResource(mu.module(), u, p2, "/" + n, Files.size(f));
						Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<>() {

							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//							IO.println("file=" + file);
								String ex;
								{
									var n = file.getFileName().toString();
									var i = n.lastIndexOf('.');
									ex = i != -1 ? n.substring(i + 1).toLowerCase() : null;
								}
								if (ex != null && EXTENSIONS.contains(ex)) {
									var r = new ZipEntryResource(a, file.toString(), Files.size(file));
//								IO.println("r=" + r);
									var k2 = r.archive().path().substring(0, r.archive().path().length() - 4)
											.substring(r.archive().package1().length() + 1) + r.path();
									if (k != null && !k.isEmpty())
										k2 = k + k2;
									put(k2, r);
								}
								return FileVisitResult.CONTINUE;
							}
						});
					}
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
//		IO.println("ResourceMap=" + this);
	}
}

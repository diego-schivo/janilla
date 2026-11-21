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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultResourceMap extends HashMap<String, Resource> implements ResourceMap {

	private static final Logger LOGGER = System.getLogger(DefaultResourceMap.class.getName());

	private static final long serialVersionUID = -334340699116362752L;

	public DefaultResourceMap(Map<String, List<ResourcesProvider>> providers) {
		LOGGER.log(Level.DEBUG, "providers={0}", providers);

		for (var kv : providers.entrySet()) {
			var bp = kv.getKey();
			var pp = kv.getValue();

			pp.stream().map(x -> x.getResources()).reduce((m1, m2) -> {
				m1.putAll(m2);
				return m1;
			}).get().entrySet().forEach(x -> put(bp + x.getKey(), x.getValue()));
		}

		LOGGER.log(Level.DEBUG, "this={0}", this);
	}
}

//for (var f : ff) {
//	Path d;
//	{
//		var fs = f.getFileSystem();
//		d = fs == FileSystems.getDefault()
//				? Stream.iterate(f, x -> x.getParent())
//						.filter(x -> x.getFileName().toString().equals("classes")).findFirst().get()
//				: fs.getRootDirectories().iterator().next();
//	}
//	f = d.relativize(f);
//	LOGGER.log(Level.DEBUG, "d={0}, f={1}", d, f);
//
//	try {
//		record A(Module module, URI uri) {
//			static A of(String name) {
//				if (Java.class.getModule().isNamed()) {
//					var l = ModuleLayer.boot();
//					return l.configuration().modules().stream().flatMap(x -> {
//						try (var r = x.reference().open()) {
//							return r.find(name).map(y -> new A(l.findModule(x.name()).get(), y)).stream();
//						} catch (IOException e) {
//							throw new UncheckedIOException(e);
//						}
//					}).findFirst().orElse(null);
//				} else {
//					var u = Thread.currentThread().getContextClassLoader().getResource(name);
//					return Optional.ofNullable(u).map(x -> {
//						try {
//							return new A(null, x.toURI());
//						} catch (URISyntaxException e) {
//							throw new RuntimeException(e);
//						}
//					}).orElse(null);
//				}
//			}
//		}
//
//		String ex;
//		{
//			var n = f.getFileName().toString();
//			var i = n.lastIndexOf('.');
//			ex = i != -1 ? n.substring(i + 1).toLowerCase() : null;
//		}
//
//		if (ex == null)
//			;
//		else if (EXTENSIONS.contains(ex)) {
//			DefaultResource r;
//			{
//				var p = f.getParent().toString().replace(File.separatorChar, '/').replace('/', '.');
//				var n = f.toString().replace(File.separatorChar, '/');
//				var mu = A.of(n);
//				r = new DefaultResource(mu.module(), mu.uri(), p, "/" + n, Files.size(d.resolve(f)));
//			}
//
//			var n = r.path().substring(r.package1().length() + 1);
//			put(bp != null ? bp + n : n, r);
//		} else if (ex.equals("zip")) {
//			FileSystem fs;
//			DefaultResource r0;
//			{
//				var p = f.getParent().toString().replace(File.separatorChar, '/').replace('/', '.');
//				var n = f.toString().replace(File.separatorChar, '/');
//				var mu = A.of(n);
//				var u = mu.uri();
//				if (!u.toString().startsWith("jar:"))
//					u = URI.create("jar:" + u);
//				fs = Java.zipFileSystem(u);
//				r0 = new DefaultResource(mu.module(), u, p, "/" + n, Files.size(d.resolve(f)));
//			}
//
//			Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<>() {
//
//				@Override
//				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//// IO.println("file=" + file);
//					String ex;
//					{
//						var n = file.getFileName().toString();
//						var i = n.lastIndexOf('.');
//						ex = i != -1 ? n.substring(i + 1).toLowerCase() : null;
//					}
//
//					if (ex != null && EXTENSIONS.contains(ex)) {
//						var r = new ZipEntryResource(r0, file.toString(), Files.size(file));
//// IO.println("r=" + r);
//						var n = r0.path().substring(0, r0.path().length() - 4)
//								.substring(r0.package1().length() + 1) + r.path();
//						put(bp != null ? bp + n : n, r);
//					}
//
//					return FileVisitResult.CONTINUE;
//				}
//			});
//		}
//	} catch (IOException e) {
//		throw new UncheckedIOException(e);
//	}
//}

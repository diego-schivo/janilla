/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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
package com.janilla.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

class HeaderAdder {

	public static void main(String[] args) {
		var h = Map.of("janilla", """
				/*
				 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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
				 */""".split("\n"));
//		var s = Path.of(System.getProperty("user.home")).resolve("eclipse-workspace");
		var s = Path.of(System.getProperty("user.home")).resolve("git");
		try {
			Files.walkFileTree(s, new SimpleFileVisitor<>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (dir.getParent().equals(s)) {
						var n = dir.getFileName().toString();
//						if (!n.equals("janilla") && !n.startsWith("janilla-"))
						if (!h.containsKey(n))
							return FileVisitResult.SKIP_SUBTREE;
					}
					if (dir.getFileName().toString().startsWith("."))
						return FileVisitResult.SKIP_SUBTREE;
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					var n = file.getFileName().toString();
					if (!n.endsWith(".java") && !n.endsWith(".js"))
						return FileVisitResult.CONTINUE;
					var l = Files.readAllLines(file);
					var i = l.iterator();
					var c = new Consumer<String>() {

						int state;

						@Override
						public void accept(String t) {
							state = switch (state) {
							case 0 -> t.equals("/*") ? 1 : -1;
							case 1 -> t.startsWith(" *") ? t.equals(" */") ? 2 : 1 : -1;
							case 2 -> 3;
							default -> throw new RuntimeException();
							};
						}
					};
					var a = Stream.iterate(i.next(), t -> {
						c.accept(t);
						return c.state == 1 || c.state == 2;
					}, t -> i.next()).toArray(String[]::new);
					var b = h.get(s.relativize(file).getName(0).toString());
					if (!Arrays.equals(a, b)) {
						System.out.println(file);
						Files.write(file, Stream.concat(Arrays.stream(b), l.stream().skip(a.length)).toList());
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

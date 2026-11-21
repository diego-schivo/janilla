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
package com.janilla.createexample;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.regex.Pattern;

import com.janilla.java.Java;

public class CreateExample {

	public static void main(String[] args) throws IOException {
		var tt = Java.linkedHashMap("blank",
				new String[] { "new-blank", "newblank", "NewBlank", "janilla-new-blank", "Janilla New Blank" },
				"website",
				new String[] { "new-website", "newwebsite", "NewWebsite", "janilla-new-website",
						"Janilla New Website" },
				"ecommerce", new String[] { "new-ecommerce", "newecommerce", "NewEcommerce", "janilla-new-ecommerce",
						"Janilla New Ecommerce" });
		String[] r;
		do {
			var s = IO.readln("Enter template " + tt.keySet() + " ");
			r = tt.get(s);
		} while (r == null);
		var r1 = r;

		var p = Pattern.compile("[\\w -]+");

		r = new String[5];
		for (var i = 0; i < r.length; i++)
			do {
				var s = IO.readln("Enter replacement for " + r1[i] + " ");
				if (p.matcher(s).matches())
					r[i] = s;
			} while (r[i] == null);
		var r2 = r;

		var d0 = Path.of("").resolve("../../examples").toAbsolutePath().normalize();
		var d1 = d0.resolve(r1[0]);
		var d2 = d0.resolve(r2[0]);

		if (Files.exists(d2))
			throw new RuntimeException();

		var m = new HashMap<Path, Path>();

		Files.walkFileTree(d1, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				boolean c;
				if (dir.equals(d1))
					c = true;
				else {
					var n = dir.getFileName().toString();
					c = !(n.startsWith(".") || n.equals("target"));
				}

				if (c) {
					var p1 = d1.relativize(dir);

					Path p2;
					if (p1.getNameCount() == 1)
						p2 = p1;
					else {
						var n1 = p1.getFileName();
						var n2 = n1.toString().equals(r1[1]) ? Path.of(r2[1]) : n1;
						p2 = m.get(p1.getParent()).resolve(n2);
					}
					m.put(p1, p2);

					var d = d2.resolve(p2);
					IO.println(d);
					Files.createDirectory(d);
				}

				return c ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				var c = !file.getFileName().toString().startsWith(".");
				if (c) {
					var p1 = d1.relativize(file);

					var n1 = p1.getFileName();
					var n1s = n1.toString();
					var n2 = n1s.startsWith(r1[2]) ? Path.of(r2[2] + n1s.substring(r1[2].length())) : n1;

					var p2 = p1.getNameCount() == 1 ? n2 : m.get(p1.getParent()).resolve(n2);
					var f = d2.resolve(p2);
					IO.println(f);

					if (n1s.endsWith(".xml") || n1s.endsWith(".java") || n1s.endsWith(".json") || n1s.endsWith(".md")) {
						var s1 = Files.readString(file);
						var s2 = s1.replace(r1[4], r2[4]).replace(r1[3], r2[3]).replace(r1[2], r2[2])
								.replace(r1[1], r2[1]).replace(r1[0], r2[0]);
						Files.writeString(f, s2);
					} else
						Files.copy(file, f);
				}

				return FileVisitResult.CONTINUE;
			}

		});
	}
}

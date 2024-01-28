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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.janilla.io.IO;

public interface Util {

	static Class<?> getClass(Path p) {
		var t = !Files.isDirectory(p) ? p.toString() : null;
		t = t != null && t.endsWith(".class") ? t : null;
		t = t != null ? t.substring(0, t.length() - ".class".length()).replace(File.separatorChar, '.') : null;
		Class<?> c;
		try {
			c = t != null ? Class.forName(t) : null;
		} catch (ClassNotFoundException e) {
			c = null;
		}
		return c;
	}

	static Stream<Class<?>> getPackageClasses(String package1) {
		var b = Stream.<Class<?>>builder();
		IO.acceptPackageFiles(package1, f -> {
			var c = Util.getClass(f);
			if (c != null)
				b.add(c);
		});
		return b.build();
	}

	static boolean startsWithIgnoreCase(String string, String prefix) {
		return string == prefix || (prefix != null && prefix.length() <= string.length()
				&& string.regionMatches(true, 0, prefix, 0, prefix.length()));
	}
}

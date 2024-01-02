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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.stream.Collectors;

class Format implements Runnable {

	public static void main(String[] args) {
		var e = new Format();
		e.run();
	}

	@Override
	public void run() {
		int x = 80;
		try {
//			var f = Path.of(System.getProperty("user.home")).resolve( "Projects/janilla/LICENSE");
			var f = Path.of(System.getProperty("user.home")).resolve( "Projects/janilla/README");
			var l = Files.readAllLines(f);
			var m = new LinkedList<String>();
			var n = 0;
			StringBuilder b = null;
			for (var s : l) {
				if (b == null)
					b = new StringBuilder(s);
				else if (s.isEmpty() || b.isEmpty()) {
					m.add(b.toString());
					b.setLength(0);
					b.append(s);
				} else {
					var t = s.stripLeading();
					var i = t.indexOf(' ');
					if (i < 0)
						i = t.length();
					if (n + 1 + i >= x) {
						b.append(' ').append(t);
					} else {
						m.add(b.toString());
						b.setLength(0);
						b.append(s);
					}
				}
				n = s.length();
			}
//			if (!b.isEmpty()) {
			m.add(b.toString());
			b.setLength(0);
//			}
//			System.out.println(m.stream().collect(Collectors.joining("\n")));
			l = new LinkedList<String>();
			for (var s : m) {
				n = 0;
				var p = "";
				for (var i = 0;;) {
					if (i + x - n >= s.length()) {
						l.add(p + (i == 0 ? s : s.substring(i)));
						break;
					}
					var j = s.lastIndexOf(' ', i + x - n - 1);
					if (j < i) {
						l.add(p + s.substring(i));
						break;
					}
					l.add(p + s.substring(i, j));
					if (i == 0) {
						for (; n < s.length() && s.charAt(n) == ' '; n++)
							;
						if (n > 0)
							p = " ".repeat(n);
					}
					i = j + 1;
				}
			}
//			System.out.println(l.stream().collect(Collectors.joining("\n")));
//			f = Path.of(System.getProperty("user.home")).resolve( "Projects/janilla/LICENSE2");
			f = Path.of(System.getProperty("user.home")).resolve( "Projects/janilla/README2");
//			Files.write(f, l);
			Files.writeString(f, l.stream().collect(Collectors.joining("\n")));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

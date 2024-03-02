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
package com.janilla.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.janilla.util.Lazy;

public abstract class ToResourceStream implements Function<URI, InputStream> {

	public static void main(String[] args) throws IOException {
		var f = new ToResourceStream() {

			@Override
			protected InputStream newInputStream(String name) {
				return name.equals("style.css") ? new ByteArrayInputStream("""
						body {
							margin: 0;
						}""".getBytes()) : null;
			}
		};
		f.setPaths(() -> Stream.of(Path.of("style.css")).iterator());

		String s;
		try (var i = f.apply(URI.create("/style.css"))) {
			s = new String(i.readAllBytes());
		}
		System.out.println(s);
		assert s.equals("""
				body {
					margin: 0;
				}""") : s;
	}

	protected Iterable<Path> paths;

	Supplier<Set<String>> resources = Lazy.of(() -> {
		var s = new HashSet<String>();
		for (var p : paths) {
			var n = toName(p);
			if (n != null)
				s.add(n);
		}

//		System.out.println(Thread.currentThread().getName() + " ToResourceStream resources " + resources);

		return s;
	});

	public void setPaths(Iterable<Path> paths) {
		this.paths = paths;
	}

	@Override
	public InputStream apply(URI t) {
		var p = t.getPath();
		var n = p.startsWith("/") ? p.substring(1) : null;
		return resources.get().contains(n) ? newInputStream(n) : null;
	}

	protected String toName(Path path) {
		var s = path.toString();
		if (s.startsWith(File.separator))
			s = s.substring(1);
		return s.endsWith(".css") || s.endsWith(".ico") || s.endsWith(".jpg") || s.endsWith(".js") || s.endsWith(".png")
				|| s.endsWith(".svg") || s.endsWith(".ttf") || s.endsWith(".woff") ? s : null;
	}

	protected abstract InputStream newInputStream(String name);

	public static class Simple extends ToResourceStream {

		Supplier<Map<String, String>> m = Lazy.of(() -> StreamSupport.stream(paths.spliterator(), false)
				.collect(Collectors.toMap(p -> p.getFileName().toString(),
						p -> p.getParent().toString().replace(File.separatorChar, '/'))));

		@Override
		protected String toName(Path path) {
			var n = super.toName(path);
			if (n == null)
				return null;
			var i = n.lastIndexOf(File.separatorChar);
			return n.substring(i + 1);
		}

		@Override
		protected InputStream newInputStream(String name) {
			var l = Thread.currentThread().getContextClassLoader();
			var n = m.get().get(name) + "/" + name;
			return l.getResourceAsStream(n);
		}
	}
}

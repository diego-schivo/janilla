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
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.janilla.io.IO;
import com.janilla.util.Lazy;

public abstract class ToResourceStream implements Function<URI, InputStream> {

	public static void main(String[] args) throws Exception {
		var f = new ToResourceStream() {

			@Override
			protected InputStream getResourceAsStream(String name) {
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
		for (var p : paths)
			toNames(p).forEach(s::add);
//		System.out.println("s=" + s);
		return s;
	});

	public void setPaths(Iterable<Path> paths) {
		this.paths = paths;
	}

	@Override
	public InputStream apply(URI t) {
		var p = t.getPath();
		var n = p.startsWith("/") ? p.substring(1) : null;
//		System.out.println("n=" + n);
		return resources.get().contains(n) ? getResourceAsStream(n) : null;
	}

	static Set<String> extensions = Set.of("avif", "css", "ico", "jpg", "js", "png", "svg", "ttf", "webp", "woff",
			"woff2");

	protected Stream<String> toNames(Path path) {
		var s = path.toString().replace(File.separatorChar, '/');
		if (s.startsWith("/"))
			s = s.substring(1);
		if (s.endsWith(".zip")) {
			var l = Thread.currentThread().getContextClassLoader();
			URI u;
			try {
				u = l.getResource(s).toURI();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
//			System.out.println("u=" + u);
			var b = Stream.<String>builder();
			var p = s.substring(0, s.length() - 4);
			var v = u.toString();
			u = URI.create(v.startsWith("jar:") ? v : "jar:" + v);
			try {
				var t = IO.zipFileSystem(u);
				Files.walkFileTree(t.getPath("/"), new SimpleFileVisitor<>() {

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//						System.out.println("file=" + file);
						var n = file.getFileName().toString();
						var i = n.lastIndexOf('.');
						var e = i >= 0 ? n.substring(i + 1) : null;
						if (e != null && extensions.contains(e.toLowerCase()))
							b.add(p + file);
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return b.build();
		}
		var n = path.getFileName().toString();
		var i = n.lastIndexOf('.');
		var e = i >= 0 ? n.substring(i + 1) : null;
		return e != null && extensions.contains(e.toLowerCase()) ? Stream.of(s) : Stream.empty();
	}

	protected abstract InputStream getResourceAsStream(String name);

	public static class Simple extends ToResourceStream {

		Supplier<Map<String, String>> resourcePrefixes = Lazy
				.of(() -> StreamSupport.stream(paths.spliterator(), false).flatMap(p -> {
					var s = p.toString().replace(File.separatorChar, '/');
					if (s.endsWith(".zip")) {
						var l = Thread.currentThread().getContextClassLoader();
						URI u;
						try {
							u = l.getResource(s).toURI();
						} catch (URISyntaxException e) {
							throw new RuntimeException(e);
						}
//						System.out.println("u=" + u);
						var b = Stream.<Map.Entry<String, String>>builder();
						var n = p.getFileName().toString();
						var q = n.substring(0, n.length() - 4);
						var v = u.toString();
						try {
							var t = IO.zipFileSystem(URI.create(v.startsWith("jar:") ? v : "jar:" + v));
							Files.walkFileTree(t.getPath("/"), new SimpleFileVisitor<>() {

								@Override
								public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
										throws IOException {
//									System.out.println("file=" + file);
									var e = Map.entry(q + file, s);
//									System.out.println("e=" + e);
									b.add(e);
									return FileVisitResult.CONTINUE;
								}
							});
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
						return b.build();
					}
					var i = s.lastIndexOf('/');
					var e = Map.entry(s.substring(i + 1), s.substring(0, i));
//					System.out.println("e=" + e);
					return Stream.of(e);
				}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

		@Override
		protected Stream<String> toNames(Path path) {
			var p = path.getParent().toString().replace(File.separatorChar, '/') + '/';
			return super.toNames(path).map(n -> n.substring(p.length()));
		}

		@Override
		protected InputStream getResourceAsStream(String name) {
			var l = Thread.currentThread().getContextClassLoader();
			var p = resourcePrefixes.get().get(name);
			if (p.endsWith(".zip")) {
				URI u;
				try {
					u = l.getResource(p).toURI();
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
				var v = u.toString();
				try {
					var s = IO.zipFileSystem(URI.create(v.startsWith("jar:") ? v : "jar:" + v));
					return Files.newInputStream(s.getPath(name.substring(name.indexOf('/'))));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			var n = p + "/" + name;
//			System.out.println("n=" + n);
			return l.getResourceAsStream(n);
		}
	}
}

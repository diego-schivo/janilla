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
package com.janilla.java;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Java {

	private static final Pattern JAR_URI_PATTERN = Pattern.compile("(jar:.*)!(.*)");

	private static final Map<String, List<Class<?>>> PACKAGE_CLASSES = new ConcurrentHashMap<>();

	private static final Map<String, List<Path>> PACKAGE_PATHS = new ConcurrentHashMap<>();

	private static final Map<String, FileSystem> ZIP_FILE_SYSTEMS = new ConcurrentHashMap<>();

	private Java() {
		throw new Error("no instances");
	}

	public static List<Class<?>> getPackageClasses(String package1) {
		return PACKAGE_CLASSES.computeIfAbsent(package1, k -> {
			Stream<Class<?>> cc = getPackagePaths(k).stream().map(x -> {
//				IO.println("Java.getPackageClasses, x=" + x);
				var d = x.getFileSystem() == FileSystems.getDefault()
						? Stream.iterate(x, y -> y.getParent())
								.dropWhile(y -> !y.getFileName().toString().equals("classes")).findFirst().get()
						: x.getFileSystem().getRootDirectories().iterator().next();
				x = d.relativize(x);
				var n = x.toString();
				n = n.endsWith(".class") ? n.substring(0, n.length() - ".class".length()) : null;
				n = n != null ? n.replace(File.separatorChar, '.') : null;
				Class<?> c;
				try {
					c = n != null ? Class.forName(n) : null;
				} catch (ClassNotFoundException e) {
					c = null;
				}
//				IO.println("Java.getPackageClasses, c=" + c);
				return c;
			});
			return cc.filter(Objects::nonNull).toList();
		});
	}

	public static List<Path> getPackagePaths(String package1) {
//		IO.println("Java.getPackagePaths, package1=" + package1);
		return PACKAGE_PATHS.computeIfAbsent(package1, k -> {
			var n = k.replace('.', '/');
			return ModuleLayer.boot().configuration().modules().stream().map(ResolvedModule::reference).flatMap(x -> {
				try (var r = x.open()) {
					return r.find(n).stream();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).map(u -> {
//				IO.println("Java.getPackagePaths, u=" + u);
				var m = JAR_URI_PATTERN.matcher(u.toString());
				var d = m.matches() ? zipFileSystem(URI.create(m.group(1))).getPath(m.group(2)) : Path.of(u);
//				IO.println("Java.getPackagePaths, d=" + d);
				return d;
			}).flatMap(x -> {
//				IO.println("Java.getPackagePaths, x=" + x);
				try (var yy = Files.walk(x)) {
					var pp = yy.toList();
//					IO.println("Java.getPackagePaths, pp=" + pp);
					return pp.stream();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).toList();
		});
	}

	public static <K, V> Map<K, V> hashMap(K k1, V v1, K k2, V v2) {
		var x = HashMap.<K, V>newHashMap(2);
		x.put(k1, v1);
		x.put(k2, v2);
		return x;
	}

	public static <K, V> Map<K, V> hashMap(K k1, V v1, K k2, V v2, K k3, V v3) {
		var x = HashMap.<K, V>newHashMap(3);
		x.put(k1, v1);
		x.put(k2, v2);
		x.put(k3, v3);
		return x;
	}

	public static <K, V> Map<K, V> hashMap(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
		var x = HashMap.<K, V>newHashMap(4);
		x.put(k1, v1);
		x.put(k2, v2);
		x.put(k3, v3);
		x.put(k4, v4);
		return x;
	}

	public static <K, V> Entry<K, V> mapEntry(K k, V v) {
		return new AbstractMap.SimpleImmutableEntry<>(k, v);
	}

	public static FileSystem zipFileSystem(URI uri) {
		return ZIP_FILE_SYSTEMS.computeIfAbsent(uri.toString(), k -> {
			try {
				var i = k.lastIndexOf('!');
				if (i == -1)
					try {
						return FileSystems.getFileSystem(uri);
					} catch (FileSystemNotFoundException _) {
						return FileSystems.newFileSystem(uri, Map.of());
					}
				var p = zipFileSystem(URI.create(k.substring(0, i))).getPath(k.substring(i + 1));
				return FileSystems.newFileSystem(p, Map.of());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	public static class EntryList<K, V> extends ArrayList<Map.Entry<K, V>> {

		private static final long serialVersionUID = 2930611377458857452L;

//		public static void main(String[] args) {
//			var l = new EntryList<String, String>();
//			l.add("foo", "bar");
//			l.set("foo", "baz");
//			IO.println(l);
//			assert l.equals(List.of(Map.entry("foo", "baz"))) : l;
//		}

		public void add(K key, V value) {
			add(new AbstractMap.SimpleEntry<>(key, value));
		}

		public V get(Object key) {
			return stream().filter(x -> Objects.equals(x.getKey(), key)).findFirst().map(Map.Entry::getValue)
					.orElse(null);
		}

		public Stream<V> stream(Object key) {
			return stream().filter(x -> Objects.equals(x.getKey(), key)).map(Map.Entry::getValue);
		}

		public void set(K key, V value) {
			stream().filter(x -> Objects.equals(x.getKey(), key)).findFirst().ifPresentOrElse(x -> x.setValue(value),
					() -> add(key, value));
		}
	}
}

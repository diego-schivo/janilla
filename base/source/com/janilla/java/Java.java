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
package com.janilla.java;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ResolvedModule;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Java {

	private Java() {
		throw new Error("no instances");
	}

	public static List<Class<?>> getPackageClasses(String package1) {
//		IO.println("Java.getPackageClasses, package1=" + package1);
		class A {
			private static final Map<String, List<Class<?>>> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(package1, k -> {
			Stream<Class<?>> cs = getPackagePaths(k).stream().map(x -> {
//				IO.println("Java.getPackageClasses, x=" + x);
				var d = x.getFileSystem() == FileSystems.getDefault()
						? Stream.iterate(x, y -> y.getParent())
								.dropWhile(y -> !y.getFileName().toString().equals("classes")).findFirst().get()
						: x.getFileSystem().getRootDirectories().iterator().next();
				x = d.relativize(x);
				var n = x.toString();
				n = n.endsWith(".class") ? n.substring(0, n.length() - ".class".length()) : null;
				n = n != null ? n.replace(File.separatorChar, '/') : null;
				n = n != null ? n.replace('/', '.') : null;
				Class<?> c;
				try {
					c = n != null ? Class.forName(n) : null;
				} catch (ClassNotFoundException e) {
					c = null;
				}
//				IO.println("Java.getPackageClasses, c=" + c);
				return c;
			});
			var cc = cs.filter(Objects::nonNull).toList();
//			IO.println("Java.getPackageClasses, cc=" + cc);
			return cc;
		});
	}

	public static List<Path> getPackagePaths(String package1) {
//		IO.println("Java.getPackagePaths, package1=" + package1);
		class A {
			private static final Map<String, List<Path>> RESULTS = new ConcurrentHashMap<>();
			private static final Pattern JAR_URI_PATTERN = Pattern.compile("(jar:.*)!(.*)");
		}
		return A.RESULTS.computeIfAbsent(package1, k -> {
			var n = k.replace('.', '/');
			var uu = Java.class.getModule().isNamed() ? ModuleLayer.boot().configuration().modules().stream()
					.map(ResolvedModule::reference).flatMap(x -> {
						try (var r = x.open()) {
							return r.find(n).stream();
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}) : Thread.currentThread().getContextClassLoader().resources(n).map(x -> {
						try {
							return x.toURI();
						} catch (URISyntaxException e) {
							throw new RuntimeException(e);
						}
					});
			return uu.map(x -> {
//				IO.println("Java.getPackagePaths, u=" + u);
				var m = A.JAR_URI_PATTERN.matcher(x.toString());
				var d = m.matches() ? zipFileSystem(URI.create(m.group(1))).getPath(m.group(2)) : Path.of(x);
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

	public static Class<?> toClass(Type type) {
//		IO.println("Java.toClass, type=" + type);
		return switch (type) {
		case Class<?> x -> x;
		case ParameterizedType x -> (Class<?>) x.getRawType();
		case TypeVariable<?> _ -> Object.class;
		case WildcardType _ -> Object.class;
		default -> throw new IllegalArgumentException();
		};
	}

	public static FileSystem zipFileSystem(URI uri) {
		class A {
			private static final Map<String, FileSystem> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(uri.toString(), k -> {
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
}

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
import java.io.InputStream;
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
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public final class Java {

	private Java() {
		throw new Error("no instances");
	}

	public static <E> ArrayList<E> arrayList(E e) {
		var x = new ArrayList<E>(1);
		x.add(e);
		return x;
	}

	public static List<Class<?>> getPackageClasses(String package1, boolean recursive) {
//		IO.println("Java.getPackageClasses, package1=" + package1 + ", recursive=" + recursive);
		class A {
			private static final Map<String, List<Class<?>>> RESULTS = new ConcurrentHashMap<>();
		}
		var pp = getPackagePaths(package1, false).toList();
		return A.RESULTS.computeIfAbsent(package1, _ -> {
			Stream<Class<?>> s = pp.stream().map(x -> {
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
			s = s.filter(Objects::nonNull);
			if (recursive)
				s = Stream.concat(s, pp.stream().filter(Java::isDirectory)
						.flatMap(x -> getPackageClasses(package1 + '.' + x.getFileName().toString(), true).stream()));
			var cc = s.toList();
//			IO.println("Java.getPackageClasses, cc=" + cc);
			return cc;
		});
	}

	public static Stream<Path> getPackagePaths(String package1, boolean recursive) {
//		IO.println("Java.getPackagePaths, package1=" + package1 + ", recursive=" + recursive);
		class A {
			private static final Map<String, Map<Path, List<Path>>> RESULTS = new ConcurrentHashMap<>();
			private static final Pattern JAR_URI_PATTERN = Pattern.compile("(jar:.*)!(.*)");
		}
		var r = A.RESULTS.computeIfAbsent(package1, k -> {
//			IO.println("Java.getPackagePaths, k=" + k);
			var n = k.replace('.', '/');
			var uu = Java.class.getModule().isNamed() ? ModuleLayer.boot().configuration().modules().stream()
					.map(ResolvedModule::reference).flatMap(x -> {
						try (var y = x.open()) {
							return y.find(n).stream();
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
//				IO.println("Java.getPackagePaths, x=" + x);
				var m = A.JAR_URI_PATTERN.matcher(x.toString());
				var d = m.matches() ? zipFileSystem(URI.create(m.group(1))).getPath(m.group(2)) : Path.of(x);
//				IO.println("Java.getPackagePaths, d=" + d);
				return d;
			}).collect(Collectors.toMap(x -> x, x -> {
//				IO.println("Java.getPackagePaths, x=" + x);
//				try (var yy = Files.walk(x)) {
				try (var yy = Files.list(x)) {
					var pp = yy.toList();
//					IO.println("Java.getPackagePaths, pp=" + pp);
					return pp;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}));
		});
//		IO.println("Java.getPackagePaths, r=" + r);
		if (recursive) {
			var ss = Stream.<String>builder();
			return IntStream.range(0, 2).mapToObj(i -> i == 0 ? r.entrySet().stream()
					.flatMap(x -> Stream.concat(Stream.of(x.getKey()), x.getValue().stream().filter(y -> {
						if (isDirectory(y)) {
							ss.add(y.getFileName().toString());
							return false;
						}
						return true;
					}))) : ss.build().distinct().flatMap(x -> getPackagePaths(package1 + "." + x, true)))
					.flatMap(x -> x);
		}
		return r.values().stream().flatMap(List::stream);
	}

	protected static boolean isDirectory(Path path) {
		class A {
			private static final Map<Path, Boolean> RESULTS = new ConcurrentHashMap<>();
		}
		return A.RESULTS.computeIfAbsent(path, Files::isDirectory);
	}

	public static <K, V> HashMap<K, V> hashMap(K k1, V v1, K k2, V v2) {
		var x = HashMap.<K, V>newHashMap(2);
		x.put(k1, v1);
		x.put(k2, v2);
		return x;
	}

	public static <K, V> HashMap<K, V> hashMap(K k1, V v1, K k2, V v2, K k3, V v3) {
		var x = HashMap.<K, V>newHashMap(3);
		x.put(k1, v1);
		x.put(k2, v2);
		x.put(k3, v3);
		return x;
	}

	public static <K, V> HashMap<K, V> hashMap(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
		var x = HashMap.<K, V>newHashMap(4);
		x.put(k1, v1);
		x.put(k2, v2);
		x.put(k3, v3);
		x.put(k4, v4);
		return x;
	}

	public static <E> HashSet<E> hashSet(E e) {
		var x = new HashSet<E>(1);
		x.add(e);
		return x;
	}

	public static <E> LinkedHashSet<E> linkedHashSet(E e) {
		var x = new LinkedHashSet<E>(1);
		x.add(e);
		return x;
	}

	public static <K, V> Map.Entry<K, V> mapEntry(K k, V v) {
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

	public static void generateKeyPair(Path keyStore, String password) {
		try {
			new ProcessBuilder("keytool",
//			new ProcessBuilder("/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home/bin/keytool",
					"-genkeypair", "-dname", "cn=localhost", "-keyalg", "rsa", "-keystore", keyStore.toString(),
					"-storepass", password, "-ext", "san=dns:localhost,ip:127.0.0.1").start().waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static SSLContext sslContext(InputStream keyStore, char[] password) {
		try {
			var ks = KeyStore.getInstance("PKCS12");
			ks.load(keyStore, password);
			var kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, password);
			var tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);
			var c = SSLContext.getInstance("TLSv1.3");
			c.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			return c;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

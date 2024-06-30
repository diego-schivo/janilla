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
package com.janilla.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class IO {

	public static int DEFAULT_BUFFER_CAPACITY = 8192;

	public static Pattern JAR_URI_PATTERN = Pattern.compile("(jar:.+)!(.+)");

	public static void emptyDirectory(Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
				if (!dir.equals(directory))
					Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public static Stream<Path> getPackageFiles(String package1) {
		var b = Stream.<Path>builder();
		IO.acceptPackageFiles(package1, b::add);
		return b.build();
	}

	public static void acceptPackageFiles(String package1, java.util.function.Consumer<Path> consumer) {
		var c = IntStream.iterate(package1.indexOf('.'), i -> i >= 0, i -> package1.indexOf('.', i + 1)).count() + 1;

		acceptPackagePaths(package1, p -> {
//			System.out.println("p=" + p);
			var r = Stream.iterate(p, Path::getParent).limit(c + 1).reduce((a, b) -> b).orElse(null);
			try {
				Files.walkFileTree(p, new SimpleFileVisitor<Path>() {

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						var f = r.relativize(file);
						consumer.accept(f);
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	public static void acceptPackagePaths(String package1, java.util.function.Consumer<Path> consumer) {
		var s = package1.replace('.', '/');
		var s1 = IntStream.iterate(s.indexOf('/'), i -> i >= 0, i -> s.indexOf('/', i + 1));
		var s2 = IntStream.concat(s1, IntStream.of(s.length())).mapToObj(i -> s.substring(0, i));

		try (var s3 = s2.flatMap(n -> {
//			System.out.println("n=" + n);
			return Thread.currentThread().getContextClassLoader().resources(n).map(r -> {
//				System.out.println("r=" + r);
				URI u;
				try {
					u = r.toURI();
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}

				var m = JAR_URI_PATTERN.matcher(u.toString());
				Path p;
				try {
					p = m.matches() ? zipFileSystem(URI.create(m.group(1))).getPath(m.group(2)) : Path.of(u);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}

				if (n.length() < s.length())
					p = p.resolve(s.substring(n.length() + 1));

				return p;
			});
		}).distinct().filter(Files::exists)) {
			s3.forEach(consumer);
		}
	}

	public static int put(ByteBuffer source, ByteBuffer destination) {
		var r1 = source.remaining();
		var r2 = destination.remaining();
		var n = Math.min(r1, r2);
		if (n == 0)
			return 0;
		if (n < r1)
			source.limit(source.position() + n);
		destination.put(source);
		if (n < r1)
			source.limit(source.limit() + r1 - n);
		return n;
	}

	public static int put(ByteBuffer source, ByteBuffer destination, int length) {
		if (length < 0)
			throw new IllegalArgumentException("length=" + length);
		if (length == 0)
			return 0;
		var r = source.remaining();
		var l = 0;
		if (length < r) {
			l = source.limit();
			source.limit(source.position() + length);
		}
		var n = put(source, destination);
		if (length < r)
			source.limit(l);
		return n;
	}

	public static int read(ReadableByteChannel source, byte[] destination) throws IOException {
		if (destination.length == 0)
			return 0;
		var b = ByteBuffer.wrap(destination);
		return read(source, b);
	}

	public static int read(ReadableByteChannel source, ByteBuffer destination) throws IOException {
		var n = 0;
		while (destination.hasRemaining()) {
			var r = source.read(destination);
			if (r < 0)
				break;
			n += r;
		}
		return n;
	}

	public static void transfer(ReadableByteChannel source, WritableByteChannel destination) throws IOException {
		var b = ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY);
		repeat(x -> {
			b.clear();
			var n = source.read(b);
			if (n > 0) {
				b.flip();
				repeat(y -> destination.write(b), n);
			}
			return n;
		}, Integer.MAX_VALUE);
	}

	public static int write(byte[] source, WritableByteChannel destination) throws IOException {
		return write(ByteBuffer.wrap(source), destination);
	}

	public static int write(ByteBuffer source, WritableByteChannel destination) throws IOException {
		return repeat(x -> destination.write(source), source.remaining());
	}

	public static int repeat(IntUnaryOperator operation, int target) throws IOException {
		var n = 0;
		while (n < target) {
			var i = operation.applyAsInt(target - n);
			if (i < 0)
				break;
			n += i;
		}
		return n;
	}

	public static byte[] readAllBytes(ReadableByteChannel channel) throws IOException {
		return Channels.newInputStream(channel).readAllBytes();
	}

	static Map<String, FileSystem> zipFileSystems = new ConcurrentHashMap<>();

	public static FileSystem zipFileSystem(URI uri) throws IOException {
		try {
			var s = uri.toString();
			return zipFileSystems.computeIfAbsent(s, k -> {
//				System.out.println("k=" + k);
				try {
					var i = k.lastIndexOf('!');
					if (i < 0)
						try {
							return FileSystems.getFileSystem(uri);
						} catch (FileSystemNotFoundException e) {
							return FileSystems.newFileSystem(uri, Map.of());
						}
					var t = zipFileSystem(URI.create(k.substring(0, i)));
					var p = t.getPath(k.substring(i + 1));
					return FileSystems.newFileSystem(p, Map.of());
				} catch (IOException f) {
					throw new UncheckedIOException(f);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	public static IntStream toIntStream(ReadableByteChannel channel) {
		var bb = ByteBuffer.allocate(1);
		return IntStream.generate(() -> {
			try {
				bb.position(0);
				for (;;) {
					var r = channel.read(bb);
					if (r > 0)
						return Byte.toUnsignedInt(bb.get(0));
					if (r < 0)
						return r;
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).takeWhile(x -> x >= 0);
	}

	public static ReadableByteChannel toReadableByteChannel(ByteBuffer buffer) {
		return new ReadableByteChannel() {

			boolean closed;

			@Override
			public boolean isOpen() {
				return !closed;
			}

			@Override
			public void close() throws IOException {
				closed = true;
			}

			@Override
			public int read(ByteBuffer dst) throws IOException {
				if (closed)
					throw new IOException("closed");
				var r = buffer.remaining();
				if (r == 0)
					return -1;
				var l = Math.min(r, dst.remaining());
				put(buffer, dst, l);
				return l;
			}
		};
	}

	public interface Runnable {

		void run() throws IOException;
	}

	public interface Consumer<T> {

		void accept(T t) throws IOException;
	}

	public interface BiConsumer<T, U> {

		void accept(T t, U u) throws IOException;
	}

	public interface Function<T, R> {

		R apply(T t) throws IOException;
	}

	public interface BiFunction<T, U, R> {

		R apply(T t, U u) throws IOException;
	}

	public interface Predicate<T> {

		boolean test(T t) throws IOException;
	}

	public interface Supplier<T> {

		T get() throws IOException;
	}

	public interface BooleanSupplier {

		boolean getAsBoolean() throws IOException;
	}

	public interface IntSupplier {

		int getAsInt() throws IOException;
	}

	public interface LongSupplier {

		long getAsLong() throws IOException;
	}

	public interface UnaryOperator<T> extends Function<T, T> {
	}

	public interface IntUnaryOperator {

		int applyAsInt(int operand) throws IOException;
	}

	public static class Lazy<T> implements Supplier<T> {

		public static <T> Lazy<T> of(Supplier<T> supplier) {
			var l = new Lazy<T>();
			l.supplier = supplier;
			return l;
		}

		Supplier<T> supplier;

		boolean got;

		T result;

		private Lazy() {
		}

		@Override
		public T get() throws IOException {
			if (!got)
				synchronized (this) {
					if (!got) {
						result = supplier.get();
						got = true;
					}
				}
			return result;
		}
	}
}

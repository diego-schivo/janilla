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
package com.janilla.sqlite;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import com.janilla.io.TransactionalByteChannel;

public class Foo2 {

	private static final String[] WORDS = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
			.split("[^\\w]+");

	public static void main(String[] args) {
//		var ss = Stream.concat(IntStream.range(0, 800).mapToObj(x -> "I" + (x + 1)),
//				IntStream.range(0, 750).mapToObj(x -> "D" + (x + 1))).toArray(String[]::new);
		var r = ThreadLocalRandom.current();
		var b1 = 10;
		var ss = IntStream.range(0, b1)
//				.mapToObj(x -> (new String[] { "I", "D" })[(int) (r.nextDouble((double) (x + 1) / b1) * 2)]
				.mapToObj(x -> (new String[] { "I", "D" })[(int) r.nextDouble(1 + (double) x / b1)]
						+ (r.nextInt(10) + 1))
				.toArray(String[]::new);
//		ss = new String[0];
//		ss = ""
//				.replaceAll("[\\[\\]]", "").split(", ");
		IO.println(Arrays.toString(ss));

//		try {
//			Files.deleteIfExists(Path.of("ex1a"));
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//
//		{
//			var b = new ProcessBuilder("/bin/bash", "-c", ss.length == 0 ? """
//					sqlite3 <<EOF
//					PRAGMA page_size = 512;
//					.save ex1a
//					EOF
//					""" : """
//					sqlite3 ex1a <<EOF
//					PRAGMA page_size = 512;
//					CREATE TABLE t1(a INTEGER PRIMARY KEY, b TEXT) WITHOUT ROWID;
//					EOF
//					""").inheritIO();
//			Process p;
//			try {
//				p = b.start();
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
//			try {
//				p.waitFor();
//			} catch (InterruptedException e) {
//				throw new RuntimeException(e);
//			}
//		}
//
//		{
//			var pb = new ProcessBuilder("/bin/bash", "-c", "sqlite3 ex1a <<EOF\n" + Arrays.stream(ss).map(x -> {
//				var i = Integer.parseInt(x.substring(1));
//				var a = (long) i;
//				var b = WORDS[(i - 1) % WORDS.length];
//				var s = switch (x.charAt(0)) {
//				case 'I' -> "insert into t1 values(" + a + ", '" + b + "');";
//				case 'D' -> "delete from t1 where a=" + a + ";";
//				default -> throw new RuntimeException();
//				};
//				IO.println(s);
//				return s;
//			}).collect(Collectors.joining("\n")) + "\nEOF").inheritIO();
//			Process p;
//			try {
//				p = pb.start();
//			} catch (IOException e) {
//				throw new UncheckedIOException(e);
//			}
//			try {
//				p.waitFor();
//			} catch (InterruptedException e) {
//				throw new RuntimeException(e);
//			}
//		}

		try (var ch = new TransactionalByteChannel(
				FileChannel.open(Path.of("ex1b"), StandardOpenOption.CREATE, StandardOpenOption.READ,
						StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE),
				FileChannel.open(Path.of("ex1b.transaction"), StandardOpenOption.CREATE, StandardOpenOption.READ,
						StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
			var d = new SQLiteDatabase(ch);
			d.perform(() -> {
				d.createTable("t1", new Column[] { new Column("a", "INTEGER"), new Column("b", "TEXT") }, false);
				return null;
			}, true);
			for (var x : ss) {
				IO.println(x);
				var i = Integer.parseInt(x.substring(1));
				var a = (long) i;
				var b = WORDS[(i - 1) % WORDS.length].repeat(i % 5 + 1);
				d.perform(() -> {
					switch (x.charAt(0)) {
					case 'I':
						d.tableBTree("t1").insert(new Object[] { a, b }, new Object[0]);
						break;
					case 'D':
						d.tableBTree("t1").delete(a);
						break;
					default:
						throw new RuntimeException();
					}
					return null;
				}, true);
			}
			d.perform(() -> {
				var t = d.tableBTree("t1");
				var p = t.new Path();
				while (p.next()) {
					IO.print(Arrays.toString(p.stream().mapToInt(BTree.Position::index).toArray()));
					var c = p.getLast().cell();
					IO.println(c instanceof PayloadCell ? Arrays.toString(t.row(c)) : "-");
				}
				return null;
			}, false);
			d.perform(() -> {
				try {
					Files.write(Path.of("ex1b.txt"), d.tableBTree("t1").rows().map(Arrays::toString).toList());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				return null;
			}, false);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		var ts = new TreeSet<Object[]>((a, b) -> {
			for (var i = 0; i < a.length; i++) {
				var x = ((Comparable) a[i]).compareTo(b[i]);
				if (x != 0)
					return x;
			}
			return 0;
		});
		for (var x : ss) {
			var i = Integer.parseInt(x.substring(1));
			var a = (long) i;
			var b = WORDS[(i - 1) % WORDS.length].repeat(i % 5 + 1);
			var e = new Object[] { a, b };
			switch (x.charAt(0)) {
			case 'I':
				ts.add(e);
				break;
			case 'D':
				ts.remove(e);
				break;
			default:
				throw new RuntimeException();
			}
		}
		try {
			Files.write(Path.of("ex1c.txt"), ts.stream().map(Arrays::toString).toList());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		{
//			var b = new ProcessBuilder("/bin/bash", "-c", "diff --width=140 --side-by-side <(xxd ex1a) <(xxd ex1b)")
//			var b = new ProcessBuilder("/bin/bash", "-c", "hexdump -C -v ex1b").inheritIO();
			var b = new ProcessBuilder("/bin/bash", "-c", "diff --side-by-side ex1c.txt ex1b.txt").inheritIO();
			Process p;
			try {
				p = b.start();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

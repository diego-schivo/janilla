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

//import java.io.IO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import com.janilla.io.TransactionalByteChannel;

public class Foo5 {

	private static final String[] WORDS = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
			.split("[^\\w]+");

	public static void main(String[] args) {
		var r = ThreadLocalRandom.current();
		var ss = IntStream.range(0, 149)
				.mapToObj(x -> (new String[] { "I", "U", "D" })[r.nextInt(1)] + (/* r.nextInt(10) */x + 1))
				.toArray(String[]::new);
//		ss = new String[0];
//		ss = "[I28, I2, I18, I16, I10, I12, I17, I20, I11, I20, I19, I19, I10, I29, I4, I8, I13, I7, I23, I6, I4, I30, I8, I7, I25, I29, I1, I18, I12, I27]".replaceAll("[\\[\\]]", "").split(", ");
//		ss = "[I1, I2, I3, I4, I5, I6, I7, I8, I9, I10]".replaceAll("[\\[\\]]", "").split(", ");

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
//					CREATE TABLE words(content TEXT PRIMARY KEY, id INTEGER) WITHOUT ROWID;
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
//			IO.println(Arrays.toString(ss));
//			var b = new ProcessBuilder("/bin/bash", "-c", "sqlite3 ex1a <<EOF\n" + Arrays.stream(ss).map(x -> {
//				var v1 = Integer.parseInt(x.substring(1));
//				var v2 = (WORDS[(v1 - 1) % /* WORDS.length */5] + v1); // .repeat(y);
//				return switch (x.charAt(0)) {
//				case 'I' -> "insert into words values('" + v2 + "', " + v1 + ");";
		////				case 'U' -> {
////					var yb = b0 - y + 1;
////					var v2b = (WORDS[(yb - 1) % WORDS.length] + yb).repeat(yb);
////					yield "update tbl1 set content='" + v2b + "' where id='" + v1 + "';";
////				}
////				case 'D' -> "delete from tbl1 where id='" + v1 + "';";
//				default -> throw new RuntimeException();
//				};
//			}).collect(Collectors.joining("\n")) + "\nEOF").inheritIO();
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

		try {
			Files.deleteIfExists(Path.of("ex1b"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try (var ch = new TransactionalByteChannel(
				FileChannel.open(Path.of("ex1b"), StandardOpenOption.CREATE, StandardOpenOption.READ,
						StandardOpenOption.WRITE),
				FileChannel.open(Path.of("ex1b.transaction"), StandardOpenOption.CREATE, StandardOpenOption.READ,
						StandardOpenOption.WRITE))) {
			var d = new SQLiteDatabase(ch);
			d.perform(() -> {
//				var t = d.createTable("Words", new Column[] { new Column("Word", "TEXT") }, false);
				var t = d.createIndex("idx", "words", "content", "id");
//				for (var i = 0; i < 40; i++) {
				////					t.insert(x -> new Object[] { x, WORDS[r.nextInt(WORDS.length)] });
////					var oo = new Object[] { WORDS[i % WORDS.length] + (i + 1) };
//					var oo = new Object[] { WORDS[i % WORDS.length], 1l + i };
				////					var oo = new Object[] { WORDS[r.nextInt(WORDS.length)], 1l + i };
////					IO.println(Arrays.toString(oo));
//					t.insert(oo);
//				}
				return null;
			}, true);
			for (var x : ss) {
				IO.println(x);
				var k1 = Integer.parseInt(x.substring(1));
				var k2 = WORDS[(k1 - 1) % WORDS.length];// + k1);// .repeat(y);
				IO.println("k1=" + k1 + ", k2=" + k2);
				switch (x.charAt(0)) {
				case 'I': {
					d.perform(() -> {
						d.indexBTree("idx").insert(k2, (long) k1);
//						IO.println(y + " " + k);
						return null;
					}, true);
				}
					break;
//					case 'U': {
//						var yb = b0 - y + 1;
//						var k2b = (WORDS[(yb - 1) % WORDS.length] + yb).repeat(yb);
//							// IO.println(y + " " + k);
//							d.indexBTree("tbl1").update(new Object[] { k1 }, _ -> new Object[] { k1, k2b });
//					}
//						break;
//					case 'D': {
//							// IO.println(y + " " + k);
//							d.indexBTree("tbl1").delete(k1);
//					}
//						break;
				default:
					throw new RuntimeException();
				}
				;
			}
			d.perform(() -> {
//				var t = d.tableBTree("Words");
				var t = d.indexBTree("idx");
				var p = t.new Path();
				while (p.next()) {
					IO.print(Arrays.toString(p.stream().mapToInt(BTree.Position::index).toArray()));
					var c = p.getLast().cell();
					IO.println(c instanceof PayloadCell ? Arrays.toString(t.row(c)) : "-");
				}

//				t.select("dolor").forEach(x -> IO.println(Arrays.toString(x)));
				return null;
			}, false);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		{
			var b = new ProcessBuilder("/bin/bash", "-c", "hexdump -C -v ex1b").inheritIO();
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

//		{
//			var b = new ProcessBuilder("/bin/bash", "-c", "diff --width=140 --side-by-side <(xxd ex1a) <(xxd ex1b)")
//					.inheritIO();
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
	}
}

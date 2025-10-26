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
//		var ss = Stream.concat(IntStream.range(0, 15).mapToObj(x -> "I" + (x + 1)),
//				IntStream.range(13, 14).mapToObj(x -> "D" + (x + 1))).toArray(String[]::new);
		var r = ThreadLocalRandom.current();
		var b1 = 2000;
		var ss = IntStream.range(0, b1).mapToObj(
				x -> (new String[] { "I", "D" })[(int) r.nextDouble(1 + (double) x / b1)] + (r.nextInt(1000) + 1))
				.toArray(String[]::new);
//		ss = new String[0];
//		ss = "[I59, I5, I34, I59, I23, I65, I37, I75, I34, I62, I8, I65, I8, I47, I27, I17, D79, I82, I9, I12, I36, I14, I47, I35, I49, D43, I51, D65, I53, I52, D9, I74, I44, I93, I100, I8, I8, I12, I36, I21, I18, I40, D1, I41, I23, D25, I30, I52, I40, I90, I96, I48, D54, I61, I34, I2, I39, I18, I80, I100, I38, D58, I44, I67, I6, I66, I97, D56, I10, D9, I53, D35, I70, D34, D55, D25, I58, I31, I78, I38, I46, D17, I45, D69, I44, I20, I6, I55, D62, D45, I53, I37, I76, D18, D77, D92, D44, I98, I11, I68, I56, D94, I62, I94, D95, I39, I21, D16, I83, D62, I45, I100, I66, I61, I22, I47, I12, I48, I83, I96, I55, I60, I68, I64, I44, I95, I54, D5, I97, I62, I81, I90, I96, D73, I57, D69, D15, D60, D96, I27, I64, D69, I95, I97, D22, I41, I27, I82, I54, D1, D28, I6, I70, D100, I40, I54, I39, I74, I26, D49, I69, D72, D26, D4, I67, D9, I79, I19, I41, D80, D21, D31, I44, I70, D8, D16, I89, D76, I47, I93, D58, D46, I80, I87, I51, D10, I95, D47, D75, I65, I6, I47, I23, D41, I56, D41, I79, I20, I88, I22]"
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
//					CREATE TABLE t1(a INTEGER PRIMARY KEY, b TEXT);
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
//				var b = WORDS[(i - 1) % WORDS.length].repeat(i % 10 + 1);
//				var s = switch (x.charAt(0)) {
//				case 'I' -> "insert into t1 values(" + a + ", '" + b + "');";
//				case 'D' -> "delete from t1 where a=" + a + ";";
//				default -> throw new RuntimeException();
//				};
		////				IO.println(s);
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
				d.createTable("t1", new Column[] { new Column("a", "INTEGER", true), new Column("b", "TEXT", false) },
						false);
				return null;
			}, true);
			for (var x : ss) {
				IO.println(x);
				var i = Integer.parseInt(x.substring(1));
				var a = (long) i;
				var b = WORDS[(i - 1) % WORDS.length].repeat(i % 10 + 1);
				d.perform(() -> {
					switch (x.charAt(0)) {
					case 'I':
//						d.tableBTree("t1").insert(new Object[] { a, b }, new Object[0]);
						d.tableBTree("t1").insert(_ -> new Object[] { a, null, b });
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
					IO.println(c instanceof PayloadCell x ? Arrays.toString(t.row(x)) : ((KeyCell) c).key());
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
			var b = WORDS[(i - 1) % WORDS.length].repeat(i % 10 + 1);
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
//					.inheritIO();
//			var b = new ProcessBuilder("/bin/bash", "-c", "hexdump -C -v ex1b").inheritIO();
			var b = new ProcessBuilder("/bin/bash", "-c", "diff ex1c.txt ex1b.txt").inheritIO();
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

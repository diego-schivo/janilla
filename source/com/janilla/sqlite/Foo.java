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

import java.io.IO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.janilla.io.TransactionalByteChannel;

public class Foo {

	private static final String[] FOO = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
			.split("[^\\w]+");

	public static void main(String[] args) {
		var r = ThreadLocalRandom.current();
		var b0 = 20;
		var ss = IntStream.range(0, 10)
				.mapToObj(_ -> (new String[] { "I", "U", "D" })[r.nextInt(3)] + (r.nextInt(b0) + 1))
				.toArray(String[]::new);
//		ss = new String[0];
		ss = "[I12, U12]".replaceAll("[\\[\\]]", "").split(", ");

		try {
			Files.deleteIfExists(Path.of("ex1a"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		{
			var b = new ProcessBuilder("/bin/bash", "-c", ss.length == 0 ? """
					sqlite3 <<EOF
					PRAGMA page_size = 512;
					.save ex1a
					EOF
					""" : """
					sqlite3 ex1a <<EOF
					PRAGMA page_size = 512;
					CREATE TABLE tbl1(content TEXT);
					CREATE INDEX tbl1content ON tbl1(content);
					EOF
					""").inheritIO();
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

		{
			IO.println(Arrays.toString(ss));
			var b = new ProcessBuilder("/bin/bash", "-c", "sqlite3 ex1a <<EOF\n" + Arrays.stream(ss).map(x -> {
				var y = Integer.parseInt(x.substring(1));
				var z = (FOO[(y - 1) % FOO.length] + y).repeat(y);
				return switch (x.charAt(0)) {
				case 'I' -> "insert into tbl1 values('" + z + "');";
				case 'U' -> {
					var y2 = b0 - y + 1;
					var z2 = (FOO[(y2 - 1) % FOO.length] + y2).repeat(y2);
					yield "update tbl1 set content='" + z2 + "' where content='" + z + "';";
				}
				case 'D' -> "delete from tbl1 where content='" + z + "';";
				default -> throw new RuntimeException();
				};
			}).collect(Collectors.joining("\n")) + "\nEOF").inheritIO();
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

		try {
			Files.deleteIfExists(Path.of("ex1b"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		{
			try (var ch = new TransactionalByteChannel(
					FileChannel.open(Path.of("ex1b"), StandardOpenOption.CREATE, StandardOpenOption.READ,
							StandardOpenOption.WRITE),
					FileChannel.open(Path.of("ex1b.transaction"), StandardOpenOption.CREATE, StandardOpenOption.READ,
							StandardOpenOption.WRITE))) {
				var d = new SQLiteDatabase(ch);

				if (ss.length != 0)
					d.perform(() -> {
						d.createTable("tbl1");
						d.createIndex("tbl1content", "tbl1");
						return null;
					}, true);

				var m = new HashMap<Integer, Set<Long>>();
				for (var x : ss) {
					IO.println(x);
					var y = Integer.parseInt(x.substring(1));
					var z = (FOO[(y - 1) % FOO.length] + y).repeat(y);
					switch (x.charAt(0)) {
					case 'I': {
						d.perform(() -> {
							var k = d.tableBTree("tbl1").insert(_ -> new Object[] { z });
//						IO.println(y + " " + k);
							m.computeIfAbsent(y, _ -> new HashSet<>()).add(k);
							d.indexBTree("tbl1content").insert(z, k);
							return null;
						}, true);
					}
						break;
					case 'U': {
						var kk = m.get(y);
						var y2 = b0 - y + 1;
						var z2 = (FOO[(y2 - 1) % FOO.length] + y2).repeat(y2);
						if (kk != null)
							for (var k : kk)
								d.perform(() -> {
//								IO.println(y + " " + k);
									d.tableBTree("tbl1").update(k, _ -> new Object[] { z2 });
									d.indexBTree("tbl1content").delete(z, k);
									d.indexBTree("tbl1content").insert(z2, k);
									return null;
								}, true);
					}
						break;
					case 'D': {
						var kk = m.remove(y);
						if (kk != null)
							for (var k : kk)
								d.perform(() -> {
//								IO.println(y + " " + k);
									d.tableBTree("tbl1").delete(k);
									d.indexBTree("tbl1content").delete(z, k);
									return null;
								}, true);
					}
						break;
					default:
						throw new RuntimeException();
					}
					;
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		{
			var b = new ProcessBuilder("/bin/bash", "-c", "diff --width=140 --side-by-side <(xxd ex1a) <(xxd ex1b)")
					.inheritIO();
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

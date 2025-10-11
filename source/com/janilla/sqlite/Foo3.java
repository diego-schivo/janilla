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
import java.util.concurrent.ThreadLocalRandom;

import com.janilla.io.TransactionalByteChannel;

public class Foo3 {

	private static final String[] WORDS = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
			.split("[^\\w]+");

	public static void main(String[] args) {
		try {
			Files.deleteIfExists(Path.of("ex1"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try (var ch = new TransactionalByteChannel(
				FileChannel.open(Path.of("ex1"), StandardOpenOption.CREATE, StandardOpenOption.READ,
						StandardOpenOption.WRITE),
				FileChannel.open(Path.of("ex1.transaction"), StandardOpenOption.CREATE, StandardOpenOption.READ,
						StandardOpenOption.WRITE))) {
			var d = new SQLiteDatabase(ch);
			d.perform(() -> {
//				var t = d.createTable("Words", new Column[] { new Column("Word", "TEXT") }, false);
				var t = d.createTable("Words", new Column[] { new Column("Word", "TEXT") }, true);
				var r = ThreadLocalRandom.current();
				for (var i = 0; i < 100; i++) {
//					t.insert(x -> new Object[] { x, WORDS[r.nextInt(WORDS.length)] });
//					var oo = new Object[] { WORDS[i % WORDS.length] + (i + 1) };
					var oo = new Object[] { WORDS[r.nextInt(WORDS.length)] + (i + 1) };
//					IO.println(Arrays.toString(oo));
					t.insert(oo);
				}
				return null;
			}, true);
			d.perform(() -> {
//				var t = d.tableBTree("Words");
				var t = d.indexBTree("Words", "table");
				var p = t.new Path();
				while (p.next()) {
					IO.print(Arrays.toString(p.stream().mapToInt(BTree.Position::index).toArray()));
					var c = p.getLast().cell();
					IO.println(c instanceof PayloadCell ? Arrays.toString(t.row(c)) : "-");
				}
				return null;
			}, false);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

//		{
//			var b = new ProcessBuilder("/bin/bash", "-c", "hexdump -C -v ex1").inheritIO();
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

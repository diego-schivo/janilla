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

import com.janilla.io.TransactionalByteChannel;

public class FruitsForSale9 {

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
				var t = d.createTable("FruitsForSale", new Column[] { new Column("Fruit", "TEXT"),
						new Column("State", "TEXT"), new Column("Price", "REAL") }, false);
				var i = d.createIndex("Idx3", "fruitsforsale", "fruit", "state");

				t.insert(_ -> new Object[] { 1l, "Orange", "FL", 0.85 });
				i.insert(new Object[] { "Orange", "FL", 1l });
				t.insert(_ -> new Object[] { 2l, "Apple", "NC", 0.45 });
				i.insert(new Object[] { "Apple", "NC", 2l });
				t.insert(_ -> new Object[] { 4l, "Peach", "SC", 0.60 });
				i.insert(new Object[] { "Peach", "SC", 4l });
				t.insert(_ -> new Object[] { 5l, "Grape", "CA", 0.80 });
				i.insert(new Object[] { "Grape", "CA", 5l });
				t.insert(_ -> new Object[] { 18l, "Lemon", "FL", 1.25 });
				i.insert(new Object[] { "Lemon", "FL", 18l });
				t.insert(_ -> new Object[] { 19l, "Strawberry", "NC", 2.45 });
				i.insert(new Object[] { "Strawberry", "NC", 19l });
				t.insert(_ -> new Object[] { 23l, "Orange", "CA", 1.05 });
				i.insert(new Object[] { "Orange", "CA", 23l });

				IO.println(Arrays.toString(i.select("Orange").mapToLong(x -> (long) x[2]).peek(IO::println)
						.mapToObj(x -> t.select(x).findFirst().get()).mapToDouble(x -> (double) x[3]).toArray()));

				return null;
			}, true);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		{
			var b = new ProcessBuilder("/bin/bash", "-c", "hexdump -C -v ex1").inheritIO();
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

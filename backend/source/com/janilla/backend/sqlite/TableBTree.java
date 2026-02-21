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
package com.janilla.backend.sqlite;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class TableBTree extends BTree<TableLeafPage, TableLeafCell> {

	public TableBTree(SqliteDatabase database, long rootNumber) {
		super(database, rootNumber);
	}

	@Override
	public boolean select(Object[] key, boolean reverse, Consumer<Stream<Stream<Object>>> rowsOperation) {
		var k = (long) key[0];
		var s = search(k);
		if (s.found() != s.path().size() - 1)
			return false;
		if (rowsOperation != null)
			rowsOperation.accept(Stream.<Stream<Object>>of(row((PayloadCell) s.path().getLast().cell())));
		return true;
	}

	@Override
	public long count(Object[] keys) {
		if (keys.length == 0)
			return count(false);
		throw new RuntimeException();
	}

	@Override
	public boolean insert(Object[] key, Object[] data) {
		var l = insert(_ -> {
			var oo = Arrays.copyOf(key, key.length + data.length);
			System.arraycopy(data, 0, oo, key.length, data.length);
			return oo;
		});
		return l != -1;
	}

	public long insert(LongFunction<Object[]> row) {
		var n = rootNumber;
		var pp = new BTreePath(this);
		for (;;) {
			var p = BTreePage.read(n, database);
			pp.add(new BTreePosition(p, p.getCellCount()));
			if (p instanceof TableInteriorPage p2)
				n = p2.getRightMostPointer();
			else
				break;
		}

		var pi = pp.removeLast();
		var p = (TableLeafPage) pi.page();
		var i = pi.index();
		var k = (p.getCellCount() != 0 ? p.getCells().getLast().key() : 0) + 1;
		var oo = row.apply(k);
//		IO.println("TableBTree.insert, k=" + k + ", oo=" + Arrays.toString(oo));
		var l = (long) oo[0];
		if (l < k) {
			var s = search(l);
			if (s.found() == s.path().size() - 1)
				return -1;
			pp = s.path();
			pi = pp.removeLast();
			p = (TableLeafPage) pi.page();
			i = pi.index();
		}
		k = l;
		var bb = Record.toBytes(Arrays.stream(oo).skip(1).map(RecordColumn::of).toArray(RecordColumn[]::new));
		var is = database.computePayloadInitialSize(bb.length, false);
		var c = is == bb.length ? new SimpleTableLeafCell(bb.length, k, bb, 0)
				: new SimpleTableLeafCell(bb.length, k, Arrays.copyOf(bb, is), addOverflowPages(bb, is));
		if (p.getCellCount() == 0)
			p.setCellContentAreaStart(database.usableSize());

		try {
			p.getCells().add(i, c);
			database.writePageBuffer(pp.pointer(pp.size()), p.buffer());
		} catch (IllegalStateException e) {
			var cc = new ArrayList<>(p.getCells());
			cc.add(i, c);
			new TableRebalancer(pp, p, cc).run();
		}

		database.updateHeader();
		return c.key();
	}

	@Override
	public boolean delete(Object[] key, Consumer<Stream<Stream<Object>>> rowsOperation) {
//		IO.println("TableBTree.delete, key=" + Arrays.toString(key));

		var k = (long) key[0];
		var s = search(k);
		if (s.found() != s.path().size() - 1)
			return false;

		{
			var pi = s.path().removeLast();
			if (rowsOperation != null)
				rowsOperation.accept(Stream.<Stream<Object>>of(row((PayloadCell) pi.cell())));

			var p = (TableLeafPage) pi.page();
			var i = pi.index();
			p.getCells().remove(i);

			if (!s.path().isEmpty() && 3
					* (8 + p.getCells().stream().mapToInt(x -> Short.BYTES + x.size()).sum()) < database.usableSize())
				new TableRebalancer(s.path(), p, p.getCells()).run();
			else
				database.writePageBuffer(s.path().pointer(s.path().size()), p.buffer());
		}

		database.updateHeader();

		return true;
	}

	public LongStream keys(boolean reverse) {
		return cells(reverse).mapToLong(x -> ((TableLeafCell) x).key());
	}

	@Override
	public Stream<? extends Cell> cells(boolean reverse) {
		return cells(false, reverse);
	}

	@Override
	public Stream<Object> row(PayloadCell cell) {
		var oo = super.row(cell);
//		if (oo[0] == null)
//			oo[0] = ((TableLeafCell) cell).key();
//		else {
//			var oo2 = new Object[oo.length + 1];
//			oo2[0] = ((TableLeafCell) cell).key();
//			System.arraycopy(oo, 0, oo2, 1, oo.length);
//			oo = oo2;
//		}
		var i = new int[1];
		oo = oo.flatMap(x -> {
			if (i[0]++ == 0) {
				var k = ((TableLeafCell) cell).key();
				return x != null ? Stream.of(k, x) : Stream.of(k);
			}
			return Stream.of(x);
		});
		return oo;
	}

	protected Search search(long key) {
//		IO.println("TableBTree.search, key=" + key);
		var n = rootNumber;
		var f = -1;
		var pp = new BTreePath(this);
		f: for (var i = 0;; i++) {
			var p = BTreePage.read(n, database);
			var ci = 0;
			for (var c : p.getCells()) {
				var d = Long.compare(((KeyCell) c).key(), key);
				if (d == 0)
					f = i;
				if (d >= 0) {
					pp.add(new BTreePosition(p, ci));
					if (c instanceof InteriorCell x) {
						n = x.leftChildPointer();
						continue f;
					} else
						break f;
				}
				ci++;
			}
			pp.add(new BTreePosition(p, ci));
			if (p instanceof InteriorPage x)
				n = x.getRightMostPointer();
			else
				break;
		}
//		IO.println("TableBTree.search, f=" + f);
		return new Search(pp, f, null);
	}

	public static void main(String[] args) {
		var ss = Stream.concat(IntStream.range(0, 1).mapToObj(x -> "I" + (x + 1)),
				IntStream.range(0, 0).mapToObj(x -> "D" + (x + 1))).toArray(String[]::new);
//		var r = ThreadLocalRandom.current();
//		var b1 = 100;
//		var ss = IntStream.range(0, b1).mapToObj(
//				x -> (new String[] { "I", "D" })[(int) r.nextDouble(1 + (double) x / b1)] + (r.nextInt(50) + 1))
//				.toArray(String[]::new);
//		ss = new String[0];
//		ss = "[]".replaceAll("[\\[\\]]", "").split(", ");
		IO.println(Arrays.toString(ss));

		var ww = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
				.split("[^\\w]+");

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
//				var b = WORDS[(i - 1) % WORDS.length].repeat((i - 1) % 20 + 1);
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
			var d = new SqliteDatabase(ch, 512, 12, 100);
			d.perform(() -> {
				d.createTable("t1", new TableColumn[] { new TableColumn("a", "INTEGER", true),
						new TableColumn("b", "TEXT", false) }, false);
				return null;
			}, true);
			for (var x : ss) {
				IO.println(x);
				var i = Integer.parseInt(x.substring(1));
				var a = (long) i;
				var b = ww[(i - 1) % ww.length].repeat((i - 1) % 20 + 1);
				d.perform(() -> {
					switch (x.charAt(0)) {
					case 'I':
						d.table("t1").insert(_ -> new Object[] { a, null, b });
						break;
					case 'D':
						d.table("t1").delete(new Object[] { a }, null);
						break;
					default:
						throw new RuntimeException();
					}
					return null;
				}, true);
			}
			d.perform(() -> {
				var t = d.table("t1");
				var p = new BTreePath(t);
				while (p.next()) {
					IO.print(Arrays.toString(p.stream().mapToInt(BTreePosition::index).toArray()));
					var c = p.getLast().cell();
					IO.println(c instanceof PayloadCell x ? Arrays.toString(t.row(x).toArray()) : ((KeyCell) c).key());
				}
				return null;
			}, false);
			d.perform(() -> {
				try {
					Files.write(Path.of("ex1b.txt"),
							d.table("t1").rows().map(x -> Arrays.toString(x.toArray())).toList());
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
				@SuppressWarnings({ "rawtypes", "unchecked" })
				var x = ((Comparable) a[i]).compareTo(b[i]);
				if (x != 0)
					return x;
			}
			return 0;
		});
		for (var x : ss) {
			var i = Integer.parseInt(x.substring(1));
			var a = (long) i;
			var b = ww[(i - 1) % ww.length].repeat((i - 1) % 20 + 1);
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
			var b = new ProcessBuilder("/bin/bash", "-c", "hexdump -C -v ex1b").inheritIO();
//			var b = new ProcessBuilder("/bin/bash", "-c", "diff ex1c.txt ex1b.txt").inheritIO();
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

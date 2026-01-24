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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SqliteDatabase {

	public static void main(String[] args) {
		var ss = Stream.concat(IntStream.range(0, 1).mapToObj(x -> "I" + (x + 1)),
				IntStream.range(0, 0).mapToObj(x -> "D" + (x + 1))).toArray(String[]::new);
//		var r = ThreadLocalRandom.current();
//		var b1 = 1;
//		var ss = IntStream.range(0, b1)
//				.mapToObj(
//						x -> (new String[] { "I", "D" })[(int) r.nextDouble(1 + (double) x / b1)] + (r.nextInt(10) + 1))
//				.toArray(String[]::new);
//		ss = new String[0];
//		ss = "[]".replaceAll("[\\[\\]]", "").split(", ");
		IO.println(Arrays.toString(ss));

		var ww = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
				.split("[^\\w]+");

		try {
			Files.deleteIfExists(Path.of("ex1a"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		{
			var pb = new ProcessBuilder("/bin/bash", "-c", ss.length == 0 ? """
					sqlite3 <<EOF
					PRAGMA page_size = 512;
					.save ex1a
					EOF
					""" : """
					sqlite3 ex1a <<EOF
					PRAGMA page_size = 512;

					""" + Arrays.stream(ss).map(x -> {
				var i = Integer.parseInt(x.substring(1));
				var a = (long) i;
				var b = ww[(i - 1) % ww.length];// .repeat((i - 1) % 10 + 1);
				var s = switch (x.charAt(0)) {
				case 'I' -> "CREATE TABLE " + b + a + "(a INTEGER PRIMARY KEY, b TEXT);";
				case 'D' -> "DROP TABLE " + b + a + ";";
				default -> throw new RuntimeException();
				};
				// IO.println(s);
				return s;
			}).collect(Collectors.joining("\n")) + "\nEOF").inheritIO();
			Process p;
			try {
				p = pb.start();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		try (var ch = new TransactionalByteChannel(
				FileChannel.open(Path.of("ex1b"), StandardOpenOption.CREATE, StandardOpenOption.READ,
						StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE),
				FileChannel.open(Path.of("ex1b.transaction"), StandardOpenOption.CREATE, StandardOpenOption.READ,
						StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
			var d = new SqliteDatabase(ch, 512, 12);
			for (var x : ss) {
				IO.println(x);
				var i = Integer.parseInt(x.substring(1));
				var a = (long) i;
				var b = ww[(i - 1) % ww.length];// .repeat((i - 1) % 10 + 1);
				d.perform(() -> {
					switch (x.charAt(0)) {
					case 'I':
						d.createTable(b + a, new TableColumn[] { new TableColumn("a", "INTEGER", true),
								new TableColumn("b", "TEXT", false) }, false);
						break;
					case 'D':
						d.dropTable(b + a);
						break;
					default:
						throw new RuntimeException();
					}
					return null;
				}, true);
			}
//			d.perform(() -> {
//				var t = d.table("sqlite_schema");
//				var p = new BTreePath(t);
//				while (p.next()) {
//					IO.print(Arrays.toString(p.stream().mapToInt(BTreePosition::index).toArray()));
//					var c = p.getLast().cell();
//					IO.println(c instanceof PayloadCell x ? Arrays.toString(t.row(x)) : ((KeyCell) c).key());
//				}
//				return null;
//			}, false);
//			d.perform(() -> {
//				try {
//					Files.write(Path.of("ex1b.txt"), d.table("sqlite_schema").rows().map(Arrays::toString).toList());
//				} catch (IOException e) {
//					throw new UncheckedIOException(e);
//				}
//				return null;
//			}, false);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

//		var ts = new TreeSet<Object[]>((a, b) -> {
//			for (var i = 0; i < a.length; i++) {
//				var x = ((Comparable) a[i]).compareTo(b[i]);
//				if (x != 0)
//					return x;
//			}
//			return 0;
//		});
//		for (var x : ss) {
//			var i = Integer.parseInt(x.substring(1));
//			var a = (long) i;
//			var b = WORDS[(i - 1) % WORDS.length];//.repeat((i - 1) % 10 + 1);
//			var e = new Object[] { a, b };
//			switch (x.charAt(0)) {
//			case 'I':
//				ts.add(e);
//				break;
//			case 'D':
//				ts.remove(e);
//				break;
//			default:
//				throw new RuntimeException();
//			}
//		}
//		try {
//			Files.write(Path.of("ex1c.txt"), ts.stream().map(Arrays::toString).toList());
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}

		{
//			var b = new ProcessBuilder("/bin/bash", "-c", "hexdump -C -v ex1b").inheritIO();
			var b = new ProcessBuilder("/bin/bash", "-c", "diff --width=140 --side-by-side <(xxd ex1a) <(xxd ex1b)")
					.inheritIO();
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

	protected final TransactionalByteChannel channel;

	protected final int pageSize;

	protected final int reservedBytes;

	protected final DatabaseHeader header = new DatabaseHeader(ByteBuffer.allocate(100));

	protected final Lock performLock = new ReentrantLock();

	protected final AtomicBoolean performing = new AtomicBoolean();

	public SqliteDatabase(TransactionalByteChannel channel, int pageSize, int reservedBytes) {
		this.channel = channel;
		this.pageSize = pageSize;
		this.reservedBytes = reservedBytes;

		try {
			if (channel.size() == 0)
				perform(() -> {
					header.setPageSize(pageSize);

					var b = newPageBuffer();
					b.position(100);
					var p = new TableLeafPage(this, b);
					p.setCellContentAreaStart(header.getPageSize());
					writePageBuffer(1, b);

					header.setString("SQLite format 3\0");
					header.setWriteVersion(1);
					header.setReadVersion(1);
					header.setReservedBytes(0);
					header.setMaximumFraction(64);
					header.setMinimumFraction(32);
					header.setLeafFraction(32);
					header.setChangeCounter(0);
					header.setSize(1);
					header.setFreelistStart(0);
					header.setFreelistSize(0);
					header.setSchemaCookie(0);
					header.setSchemaFormat(0);
					header.setCacheSize(0);
					header.setVersion(0);
					header.setLibraryVersion(0x002e6eba);
					writeHeader();
					return null;
				}, true);
			else
				readHeader();
			if (header.getSize() == 0)
				header.setSize(channel.size() / header.getPageSize());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public TransactionalByteChannel channel() {
		return channel;
	}

	public DatabaseHeader header() {
		return header;
	}

	public int usableSize() {
		return header.getPageSize() - header.getReservedBytes();
	}

	public int computePayloadInitialSize(int payloadSize, boolean index) {
		var p = payloadSize;
		var u = usableSize();
		var x = index ? (u - 12) * 64 / 255 - 23 : u - 35;
		if (p <= x)
			return p;
		var m = (u - 12) * 32 / 255 - 23;
		var k = m + (p - m) % (u - 4);
		return k <= x ? k : m;
	}

	public ByteBuffer newPageBuffer() {
		return ByteBuffer.allocate(header.getPageSize());
	}

	public void readHeader() {
		try {
			header.buffer().clear();
			channel.position(0);
			if (channel.read(header.buffer()) != 100)
				throw new IOException();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void writeHeader() {
		try {
			header.buffer().clear();
			channel.position(0);
			if (channel.write(header.buffer()) != 100)
				throw new IOException();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void readPageBuffer(long number, ByteBuffer buffer) {
		buffer.clear();
		try {
			var s = header.getPageSize();
			channel.position((number - 1) * s);
			if (channel.read(buffer) != s)
				throw new IOException();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		buffer.position(number == 1 ? 100 : 0);
	}

	public void writePageBuffer(long number, ByteBuffer buffer) {
		buffer.clear();
		try {
			var s = header.getPageSize();
			channel.position((number - 1) * s);
			if (channel.write(buffer) != s)
				throw new IOException();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		buffer.position(number == 1 ? 100 : 0);

		if (number > header.getSize()) {
			header.setSize(number);
			writeHeader();
		}
	}

	public long newPage() {
		var s = header.getFreelistSize();
		if (s == 0) {
			var x = header.getSize() + 1;
			header.setSize(x);
			return x;
		}
		var n = header.getFreelistStart();
		var p = new FreelistTrunkPage(this, n);
		long x;
		if (!p.getLeafPointers().isEmpty()) {
			x = p.getLeafPointers().removeLast();
			writePageBuffer(n, p.buffer());
		} else {
			x = n;
			header.setFreelistStart(p.getNext());
		}
		header.setFreelistSize(header.getFreelistSize() - 1);
		return x;
	}

	public void freePage(long number, ByteBuffer buffer) {
		var s = header.getFreelistSize();
		if (s == 0) {
			var p = new FreelistTrunkPage(this, buffer);
			p.setNext(0);
			p.getLeafPointers().clear();
			writePageBuffer(number, buffer);
			header.setFreelistStart(number);
		} else {
			var n = header.getFreelistStart();
			var b = newPageBuffer();
			for (;;) {
				readPageBuffer(n, b);
				var p = new FreelistTrunkPage(this, b);
				try {
					p.getLeafPointers().add(number);
					writePageBuffer(n, b);
					break;
				} catch (IllegalStateException e) {
					if (p.getNext() == 0) {
						var p2 = new FreelistTrunkPage(this, buffer);
						p2.setNext(0);
						p2.getLeafPointers().clear();
						writePageBuffer(number, buffer);
						p.setNext(number);
						writePageBuffer(n, b);
						break;
					}
					n = p.getNext();
				}
			}
		}
		header.setFreelistSize(s + 1);
	}

	public BTree<?, ?> createTable(String name, TableColumn[] columns, boolean withoutRowid) {
//		IO.println("SqliteDatabase.createTable, name=" + name + ", columns=" + Arrays.toString(columns)
//				+ ", withoutRowid=" + withoutRowid);
		if (header.getReservedBytes() == 0 && reservedBytes != 0)
			header.setReservedBytes(reservedBytes);

		var n = newPage();
		var p = withoutRowid ? new IndexLeafPage(this, newPageBuffer()) : new TableLeafPage(this, newPageBuffer());
		p.setCellContentAreaStart(usableSize());
		writePageBuffer(n, p.buffer());

		var t = new TableBTree(this, 1);
		t.insert(
				k -> new Object[] { k, "table", name, name, n,
						"CREATE TABLE \"" + name + "\"(" + Arrays.stream(columns)
								.map(x -> "\"" + x.name() + "\" " + x.type() + (x.primaryKey() ? " PRIMARY KEY" : ""))
								.collect(Collectors.joining(", ")) + ")" + (withoutRowid ? " WITHOUT ROWID" : "") });

		updateHeader();
		return withoutRowid ? new IndexBTree(this, n) : new TableBTree(this, n);
	}

	public void dropTable(String name) {
		var t = new TableBTree(this, 1);
		var o = schema().stream().filter(x -> x.type().equals("table") && x.name().equals(name)).findFirst().get();
		t.delete(new Object[] { o.id() }, null);

		updateHeader();
	}

	public IndexBTree createIndex(String name, String table, String... columns) {
		if (header.getReservedBytes() == 0 && reservedBytes != 0)
			header.setReservedBytes(reservedBytes);

		var n = newPage();
		var p = new IndexLeafPage(this, newPageBuffer());
		p.setCellContentAreaStart(usableSize());
		writePageBuffer(n, p.buffer());

		var t = new TableBTree(this, 1);
		t.insert(k -> new Object[] { k, "index", name, table, n, "CREATE INDEX \"" + name + "\" ON \"" + table + "\"("
				+ Arrays.stream(columns).map(x -> "\"" + x + "\"").collect(Collectors.joining(", ")) + ")" });

		updateHeader();
		return new IndexBTree(this, n);
	}

	public void dropIndex(String name) {
		dropIndex(name, "index");
	}

	public void dropIndex(String name, String type) {
		var t = new TableBTree(this, 1);
		var o = schema().stream().filter(x -> x.type().equals(type) && x.name().equals(name)).findFirst().get();
		t.delete(new Object[] { o.id() }, null);

		updateHeader();
	}

	public TableBTree table(String name) {
//		IO.println("SQLiteDatabase.tableBTree, name=" + name);
		return new TableBTree(this,
				name.equals("sqlite_schema") ? 1
						: schema().stream().filter(x -> x.type().equals("table") && x.name().equals(name)).findFirst()
								.get().root());
	}

	public IndexBTree index(String name) {
		return index(name, null);
	}

	public IndexBTree index(String name, String type) {
//		IO.println("SqliteDatabase.index, name=" + name + ", type=" + type);
		var o = schema().stream().filter(x -> (type == null || x.type().equals(type)) && x.name().equals(name))
				.findFirst().get();
		return new IndexBTree(this, o.root());
	}

	public <T> T perform(Supplier<T> operation, boolean write) {
		performLock.lock();
		try {
//			if (write)
//				IO.println("1 " + LocalDateTime.now());
			var p = performing.getAndSet(true);
			var fl = !p ? ((FileChannel) channel.channel()).lock() : null;
			try {
				T t = null;
				var c = false;
				if (!p && write)
					channel.startTransaction();
				try {
					t = operation.get();
					c = true;
				} finally {
					if (!p && write) {
						if (c)
							channel.commitTransaction();
						else
							channel.rollbackTransaction();
					}
				}
				return t;
			} finally {
				if (!p) {
					fl.release();
					performing.set(false);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
//			if (write)
//				IO.println("2 " + LocalDateTime.now());
			performLock.unlock();
		}
	}

	protected void updateHeader() {
		if (header.getReservedBytes() == 0 && reservedBytes != 0)
			header.setReservedBytes(reservedBytes);
		header.setChangeCounter(header.getChangeCounter() + 1);
		header.setSchemaCookie(header.getSchemaCookie() + 1);
		header.setSchemaFormat(4);
		header.setTextEncoding(1);
		header.setVersion(header.getVersion() + 1);
		writeHeader();
	}

	public List<SchemaObject> schema() {
		var t = new TableBTree(this, 1);
		return t.rows().map(x -> new SchemaObject((Long) x[0], (String) x[1], (String) x[2], (String) x[3], (Long) x[4],
				(String) x[5])).toList();
	}
}

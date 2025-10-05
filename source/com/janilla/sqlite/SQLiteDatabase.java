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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import com.janilla.io.TransactionalByteChannel;

public class SQLiteDatabase {

	protected final TransactionalByteChannel channel;

	protected final Header header = new Header(ByteBuffer.allocate(100));

	protected final Lock performLock = new ReentrantLock();

	protected final AtomicBoolean performing = new AtomicBoolean();

	public SQLiteDatabase(TransactionalByteChannel channel) {
		this.channel = channel;
		try {
			if (channel.size() == 0)
				perform(() -> {
					header.setPageSize(512);
//					header.setPageSize(4096);

					var b = allocatePage();
					b.position(100);
					var p = new LeafTablePage(this, b);
					p.setCellContentAreaStart(header.getPageSize());
					write(1, b);

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
//		IO.println(header.getSize());
	}

	public TransactionalByteChannel channel() {
		return channel;
	}

	public Header header() {
		return header;
	}

	public int usableSize() {
		return header.getPageSize() - header.getReservedBytes();
	}

	public int initialSize(int p, boolean index) {
		var u = usableSize();
		var x = index ? (u - 12) * 64 / 255 - 23 : u - 35;
		if (p <= x)
			return p;
		var m = (u - 12) * 32 / 255 - 23;
		var k = m + (p - m) % (u - 4);
		return k <= x ? k : m;
	}

	public ByteBuffer allocatePage() {
		return ByteBuffer.allocate(header.getPageSize());
	}

	public BTreePage<?> readBTreePage(long number, ByteBuffer buffer) {
		read(number, buffer);
		var t = Byte.toUnsignedInt(buffer.get(buffer.position()));
//		IO.println("SQLiteDatabase.readBTreePage, number=" + number + ", t=" + t);
		return switch (t) {
		case 0x05 -> new InteriorTablePage(this, buffer);
		case 0x0d -> new LeafTablePage(this, buffer);
		case 0x02 -> new InteriorIndexPage(this, buffer);
		case 0x0a -> new LeafIndexPage(this, buffer);
		default -> throw new RuntimeException();
		};
	}

	public void read(long number, ByteBuffer buffer) {
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

	public void write(Page.Numbered<?> page) {
		write(page.number(), page.content().buffer());
	}

	public void write(long number, ByteBuffer buffer) {
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

	public long nextPageNumber() {
		var fn = header.getFreelistStart();
		long n;
		if (fn != 0) {
//			var fp = new FreelistPage(allocatePage(), true);
//			read(fn, fp);
//			if (!fp.getLeafPointers().isEmpty()) {
//				n = fp.getLeafPointers().removeLast();
//				write(fn, fp);
//			} else {
//				n = fn;
//				header.setFreelistStart(0);
//			}
//			header.setFreelistSize(header.getFreelistSize() - 1);
			throw new RuntimeException();
		} else {
			n = header.getSize() + 1;
			header.setSize(n);
		}
//		writeHeader();
		return n;
	}

	public long createTable(String name) {
		return createTable(name, false);
	}

	public long createTable(String name, boolean withoutRowid) {
		header.setReservedBytes(12);

		var n = nextPageNumber();
		var p = withoutRowid ? new LeafIndexPage(this, allocatePage()) : new LeafTablePage(this, allocatePage());
		p.setCellContentAreaStart(usableSize());
		write(n, p.buffer());

		var t = new TableBTree(this, 1);
		t.insert(_ -> new Object[] { "table", name, name, n,
				withoutRowid ? "CREATE TABLE " + name + "(id TEXT PRIMARY KEY, content TEXT) WITHOUT ROWID"
						: "CREATE TABLE " + name + "(content TEXT)" });

		updateHeader();
		return n;
	}

	public long createIndex(String name, String table) {
		header.setReservedBytes(12);

		var n = nextPageNumber();
		var p = new LeafIndexPage(this, allocatePage());
		p.setCellContentAreaStart(usableSize());
		write(n, p.buffer());

		var t = new TableBTree(this, 1);
		t.insert(_ -> new Object[] { "index", name, table, n, "CREATE INDEX " + name + " ON " + table + "(content)" });

		updateHeader();
		return n;
	}

	public TableBTree tableBTree(String name) {
		var o = schema().stream().filter(x -> x.type().equals("table") && x.name().equals(name)).findFirst().get();
		return new TableBTree(this, o.root());
	}

	public IndexBTree indexBTree(String name) {
		var o = schema().stream().filter(x -> /* x.type().equals("index") && */ x.name().equals(name)).findFirst()
				.get();
		return new IndexBTree(this, o.root());
	}

	public <R> R performOnTable(String name, Function<TableBTree, R> operation) {
		var o = schema().stream().filter(x -> x.type().equals("table") && x.name().equals(name)).findFirst().get();
		var t = new TableBTree(this, o.root());
		return operation.apply(t);
	}

	public <R> R performOnIndex(String name, Function<IndexBTree, R> operation) {
		var o = schema().stream().filter(x -> x.type().equals("index") && x.name().equals(name)).findFirst().get();
		var t = new IndexBTree(this, o.root());
		return operation.apply(t);
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
		header.setReservedBytes(12);
		header.setChangeCounter(header.getChangeCounter() + 1);
		header.setSchemaCookie(header.getSchemaCookie() + 1);
		header.setSchemaFormat(4);
		header.setTextEncoding(1);
		header.setVersion(header.getVersion() + 1);
		writeHeader();
	}

	protected List<SchemaObject> schema() {
		var t = new TableBTree(this, 1);
		return t.rows()
				.map(x -> new SchemaObject((String) x[0], (String) x[1], (String) x[2], (Long) x[3], (String) x[4]))
				.toList();
	}

	protected record SchemaObject(String type, String name, String table, long root, String sql) {
	}
}

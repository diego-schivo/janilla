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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.janilla.io.TransactionalByteChannel;

public class SqliteDatabase {

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
		if (header.getReservedBytes() == 0 && reservedBytes != 0)
			header.setReservedBytes(reservedBytes);

		var n = newPage();
		var p = withoutRowid ? new IndexLeafPage(this, newPageBuffer()) : new TableLeafPage(this, newPageBuffer());
		p.setCellContentAreaStart(usableSize());
		writePageBuffer(n, p.buffer());

		var t = new TableBTree(this, 1);
		t.insert(k -> new Object[] { k, "table", name, name, n,
				withoutRowid
						? "CREATE TABLE " + name + "("
								+ IntStream.range(0, columns.length)
										.mapToObj(x -> columns[x].name() + " " + columns[x].type()
												+ (x == 0 ? " PRIMARY KEY" : ""))
										.collect(Collectors.joining(", "))
								+ ") WITHOUT ROWID"
						: "CREATE TABLE " + name + "("
								+ Arrays.stream(columns)
										.map(x -> x.name() + " " + x.type() + (x.primaryKey() ? " PRIMARY KEY" : ""))
										.collect(Collectors.joining(", "))
								+ ")" });

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
		t.insert(k -> new Object[] { k, "index", name, table, n,
				"CREATE INDEX " + name + " ON " + table + "(" + String.join(", ", columns) + ")" });

		updateHeader();
		return new IndexBTree(this, n);
	}

	public TableBTree table(String name) {
//		IO.println("SQLiteDatabase.tableBTree, name=" + name);
		return new TableBTree(this,
				name.equals("sqlite_schema") ? 1
						: schema().stream().filter(x -> x.type().equals("table") && x.name().equals(name)).findFirst()
								.get().root());
	}

	public IndexBTree index(String name) {
		return index(name, "index");
	}

	public IndexBTree index(String name, String type) {
//		IO.println("SQLiteDatabase.indexBTree, name=" + name + ", type=" + type);
		var o = schema().stream().filter(x -> x.type().equals(type) && x.name().equals(name)).findFirst().get();
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

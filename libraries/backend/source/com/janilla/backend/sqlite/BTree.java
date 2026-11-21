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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.janilla.java.Java;

public abstract class BTree<LP extends BTreePage<LC>, LC extends Cell> {

	protected final SqliteDatabase database;

	protected final long rootNumber;

	protected BTree(SqliteDatabase database, long rootNumber) {
		this.database = database;
		this.rootNumber = rootNumber;
	}

	public long rootNumber() {
		return rootNumber;
	}

	public abstract boolean select(Object[] key, Consumer<Stream<Stream<Object>>> rowsOperation, TraverseOption... options);

	public abstract long count(Object[] key);

	public abstract boolean insert(Object[] key, Object[] data);

	public abstract boolean delete(Object[] key, Consumer<Stream<Stream<Object>>> rowsOperation);

//	public Stream<? extends Cell> cells() {
//		return cells(false);
//	}
//
//	public abstract Stream<? extends Cell> cells(boolean reverse);
	
	public abstract Stream<? extends Cell> payloadCells(TraverseOption... options);

//	public Stream<Stream<Object>> rows() {
//		return rows(false);
//	}

	public Stream<Stream<Object>> rows(TraverseOption... options) {
		return payloadCells(options).filter(x -> x instanceof PayloadCell).map(x -> row((PayloadCell) x));
	}

	public Stream<Object> row(PayloadCell cell) {
//		IO.println("cell.start()=" + cell.start());
		return Record.fromBytes(payloadBuffers(cell)).map(RecordColumn::toObject);
	}

	public Stream<ByteBuffer> payloadBuffers(PayloadCell cell) {
//		IO.println("cell.payloadSize()=" + cell.payloadSize());
		var b = ByteBuffer.wrap(cell.toInitialPayloadBytes());
		var d = cell.payloadSize() - b.limit();
		if (d == 0)
			return Stream.of(b);
		class A {
			int r = d;
			long on = cell.firstOverflow();
		}
		var a = new A();
		return Stream.iterate(b, x -> {
			if (a.on == 0)
				return null;
			var op = new OverflowPage(database, a.on);
			var m = Math.min(database.usableSize() - Integer.BYTES, a.r);
			var y = ByteBuffer.allocate(x.remaining() + m);
			y.put(x);
			op.getContent(y);
			y.flip();
			a.r -= m;
			a.on = op.getNext();
			return y;
		}).takeWhile(Objects::nonNull);
	}

	protected long addOverflowPages(byte[] bytes, int start) {
		return addOverflowPages(bytes, start, LongStream.generate(database::newPage));
	}

	protected long addOverflowPages(byte[] bytes, int start, LongStream nextPageNumber) {
		var ni = nextPageNumber.iterator();
		var b = ByteBuffer.wrap(bytes);
		b.position(start);
		var n = ni.nextLong();
		var n0 = n;
		ByteBuffer pb = null;
		do {
			if (pb == null)
				pb = database.newPageBuffer();
			else
				Arrays.fill(pb.array(), 0, pb.capacity(), (byte) 0);
			var p = new OverflowPage(database, pb);
			p.setContent(b);
			p.setNext(!b.hasRemaining() ? 0 : ni.nextLong());
			database.writePageBuffer(n, pb);
			n = p.getNext();
		} while (n != 0);
		return n0;
	}

//	protected void removeOverflowPages(long number) {
//		if (database.header.getFreelistSize() != 0)
//			throw new RuntimeException();
//		else {
//			var b = database.newPageBuffer();
//			database.readPageBuffer(number, b);
//			var p = new FreelistTrunkPage(database, b);
//			p.setNext(0);
//			p.getLeafPointers().clear();
//			database.writePageBuffer(number, b);
//			database.header.setFreelistSize(1);
//			database.header.setFreelistStart(number);
//		}
//	}

	protected Stream<? extends BTreePage<?>> pages(TraverseOption... options) {
		return pages(rootNumber, options);
	}

	protected Stream<? extends BTreePage<?>> pages(long startPage, TraverseOption... options) {
		var p = BTreePage.read(startPage, database);
		// IO.println("p=" + p + ", " + p.getCellCount());

		var pp = Stream.of(p);
		if (p instanceof InteriorPage<?> ip) {
			var s = ip.getCellCount();
			var l = ip.getCells();
			var r = Java.contains(options, TraverseOption.REVERSE_ORDER);
			var nn = IntStream.rangeClosed(0, s).map(i -> r ? s - i : i)
					.mapToLong(i -> i == s ? ip.getRightMostPointer() : l.get(i).leftChildPointer());
			pp = Stream.concat(pp, nn.boxed().flatMap(n -> pages(n, options)));
		}

		if (Java.contains(options, TraverseOption.LEAF_ONLY))
			pp = pp.filter(x -> !(x instanceof InteriorPage));

		return pp;
	}

//	protected Stream<LP> leafPages(boolean reverse) {
//		return pages(reverse).filter(x -> !(x instanceof InteriorPage)).map(x -> {
//			@SuppressWarnings("unchecked")
//			var y = (LP) x;
//			return y;
//		});
//	}

	protected long count(TraverseOption... options) {
		return pages(options).mapToLong(BTreePage::getCellCount).sum();
	}

	protected Stream<? extends Cell> cells(long startPage, TraverseOption... options) {
		var r = Java.contains(options, TraverseOption.REVERSE_ORDER);

		if (Java.contains(options, TraverseOption.LEAF_ONLY))
			return pages(startPage, options).flatMap(p -> {
				var s = p.getCellCount();
				var l = p.getCells();
				return IntStream.range(0, s).map(i -> r ? s - 1 - i : i).mapToObj(l::get);
			});

		var p = BTreePage.read(startPage, database);
		var s = p.getCellCount();
		var l = p.getCells();
		var cc = IntStream.range(0, s).map(i -> r ? s - 1 - i : i).mapToObj(l::get).flatMap(c -> {
			var cc1 = Stream.of(c);
			if (c instanceof InteriorCell x) {
				var cc2 = cells(x.leftChildPointer(), options);
				cc1 = r ? Stream.concat(cc1, cc2) : Stream.concat(cc2, cc1);
			}
			return cc1;
		});

		if (p instanceof InteriorPage ip) {
			var cc2 = cells(ip.getRightMostPointer(), options);
			cc = r ? Stream.concat(cc2, cc) : Stream.concat(cc, cc2);
		}

		return cc;
	}
}

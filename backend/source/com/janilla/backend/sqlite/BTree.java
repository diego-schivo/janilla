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

	public abstract boolean select(Object[] key, boolean reverse, Consumer<Stream<Stream<Object>>> rowsOperation);

	public abstract long count(Object[] key);

	public abstract boolean insert(Object[] key, Object[] data);

	public abstract boolean delete(Object[] key, Consumer<Stream<Stream<Object>>> rowsOperation);

	public Stream<? extends Cell> cells() {
		return cells(false);
	}

	public abstract Stream<? extends Cell> cells(boolean reverse);

	public Stream<Stream<Object>> rows() {
		return rows(false);
	}

	public Stream<Stream<Object>> rows(boolean reverse) {
		return cells(reverse).filter(x -> x instanceof PayloadCell).map(x -> row((PayloadCell) x));
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

	protected Stream<? extends BTreePage<?>> pages(boolean reverse) {
		return pages(rootNumber, reverse);
	}

	protected Stream<? extends BTreePage<?>> pages(long number, boolean reverse) {
		var p = BTreePage.read(number, database);
		// IO.println("p=" + p + ", " + p.getCellCount());
		var s = Stream.of(p);
		if (p instanceof InteriorPage<?> p2) {
			var l = p2.getCellCount();
			var pp = IntStream.rangeClosed(0, l).map(x -> reverse ? l - x : x)
					.mapToLong(x -> x == l ? p2.getRightMostPointer() : p2.getCells().get(x).leftChildPointer());
			s = Stream.concat(s, pp.boxed().flatMap(x -> pages(x, reverse)));
		}
		return s;
	}

	protected Stream<LP> leafPages(boolean reverse) {
		return pages(reverse).filter(x -> !(x instanceof InteriorPage)).map(x -> {
			@SuppressWarnings("unchecked")
			var y = (LP) x;
			return y;
		});
	}

	protected long count(boolean all) {
		return (all ? pages(false) : leafPages(false)).mapToLong(BTreePage::getCellCount).sum();
	}

	protected Stream<? extends Cell> cells(boolean all, boolean reverse) {
		return all ? cells(rootNumber, reverse) : leafPages(reverse).flatMap(p -> {
			var l = p.getCellCount();
			return IntStream.range(0, l).map(x -> reverse ? l - 1 - x : x).mapToObj(p.getCells()::get);
		});
	}

	protected Stream<? extends Cell> cells(long number, boolean reverse) {
		var p = BTreePage.read(number, database);
		var l = p.getCellCount();
		var s = IntStream.range(0, l).map(x -> reverse ? l - 1 - x : x).mapToObj(p.getCells()::get).flatMap(c -> {
			var s1 = Stream.of(c);
			if (c instanceof InteriorCell x) {
				var s2 = cells(x.leftChildPointer(), reverse);
				s1 = reverse ? Stream.concat(s1, s2) : Stream.concat(s2, s1);
			}
			return s1;
		});
		if (p instanceof InteriorPage x) {
			var s2 = cells(x.getRightMostPointer(), reverse);
			s = reverse ? Stream.concat(s2, s) : Stream.concat(s, s2);
		}
		return s;
	}
}

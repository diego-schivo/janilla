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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.LongFunction;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public abstract class BTree<LP extends BTreePage<LC>, LC extends Cell> {

	protected final SQLiteDatabase database;

	protected final long rootNumber;

	protected BTree(SQLiteDatabase database, long rootNumber) {
		this.database = database;
		this.rootNumber = rootNumber;
	}

	public abstract long insert(LongFunction<Object[]> values);

	public abstract boolean insert(Object[] key, Object[] data);

	public abstract Stream<Object[]> select(Object... key);

	public abstract void delete(Object... key);

	public abstract Stream<? extends Cell> cells();

	public abstract long count(Object... key);

	public Stream<Object[]> rows() {
		return cells().map(this::row);
	}

	public Object[] row(Cell cell) {
//		IO.println("cell.start()=" + cell.start());
		return Record.fromBytes(payloadBuffers((PayloadCell) cell)).map(Record.Element::toObject).toArray();
	}

	public Stream<ByteBuffer> payloadBuffers(PayloadCell cell) {
//		IO.println("cell.payloadSize()=" + cell.payloadSize());
		var b0 = cell.initialPayload(database);
		class A {

			int r = cell.payloadSize() - b0.remaining();

			long n = cell.firstOverflow();
		}
		var a = new A();
		return Stream.iterate(b0, b -> {
			if (a.n == 0)
				return null;
			var ob = database.allocatePage();
			database.read(a.n, ob);
			var op = new OverflowPage(database, ob);
			var m = Math.min(database.usableSize() - Integer.BYTES, a.r);
			var b2 = ByteBuffer.allocate(b.remaining() + m);
			b2.put(b);
			op.getContent(b2);
			b2.flip();
			a.r -= m;
			a.n = op.getNext();
			return b2;
		}).takeWhile(Objects::nonNull);
	}

	protected long addOverflowPages(byte[] bytes, int start) {
		return addOverflowPages(bytes, start, LongStream.iterate(0, _ -> database.newPage()).skip(1));
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
				pb = database.allocatePage();
			else
				Arrays.fill(pb.array(), 0, pb.capacity(), (byte) 0);
			var p = new OverflowPage(database, pb);
			p.setContent(b);
			p.setNext(!b.hasRemaining() ? 0 : ni.nextLong());
			database.write(n, pb);
			n = p.getNext();
		} while (n != 0);
		return n0;
	}

	protected void removeOverflowPages(long number) {
		if (database.header.getFreelistSize() != 0)
			throw new RuntimeException();
		else {
			var b = database.allocatePage();
			database.read(number, b);
			var p = new FreelistPage(database, b);
			p.setNext(0);
			p.getLeafPointers().clear();
			database.write(number, b);
			database.header.setFreelistSize(1);
			database.header.setFreelistStart(number);
		}
	}

	protected Stream<? extends BTreePage<?>> pages() {
		return pages(rootNumber);
	}

	protected Stream<? extends BTreePage<?>> pages(long number) {
		var p = database.readBTreePage(number, database.allocatePage());
		// IO.println("p=" + p + ", " + p.getCellCount());
		var s = Stream.of(p);
		if (p instanceof InteriorPage<?> x)
			s = Stream.concat(s, LongStream.concat(x.getCells().stream().mapToLong(InteriorCell::leftChildPointer),
					LongStream.of(x.getRightMostPointer())).boxed().flatMap(this::pages));
		return s;
	}

	protected Stream<LP> leafPages() {
		return pages().filter(x -> !(x instanceof InteriorPage)).map(x -> {
			@SuppressWarnings("unchecked")
			var y = (LP) x;
			return y;
		});
	}

	protected long count(boolean all) {
		return (all ? pages() : leafPages()).mapToLong(BTreePage::getCellCount).sum();
	}

	protected Stream<? extends Cell> cells(boolean all) {
		return all ? cells(rootNumber) : leafPages().flatMap(p -> p.getCells().stream());
	}

	protected Stream<? extends Cell> cells(long number) {
		var p = database.readBTreePage(number, database.allocatePage());
		var s = p.getCells().stream().flatMap(c -> {
			var s2 = Stream.of(c);
			if (c instanceof InteriorCell x)
				s2 = Stream.concat(cells(x.leftChildPointer()), s2);
			return s2;
		});
		if (p instanceof InteriorPage x)
			s = Stream.concat(s, cells(x.getRightMostPointer()));
		return s;
	}

	public record Position(BTreePage<?> page, int index) {

		public Cell cell() {
			return index != page.getCellCount() ? page.getCells().get(index) : null;
		}
	};

	public class Path extends ArrayList<Position> {

		private static final long serialVersionUID = 8714727091836519387L;

		public Path() {
		}

		public Path(Collection<Position> pp) {
			super(pp);
		}

		public long number(int index) {
			if (index == 0)
				return rootNumber;
			var x = get(index - 1);
			return ((InteriorPage<?>) x.page()).childPointer(x.index());
		}

		public boolean next() {
			long n;
			if (isEmpty())
				n = rootNumber;
			else {
				for (;;) {
					var p = removeLast();
					var i = p.index() + 1;
					if (i < p.page().getCellCount() + (p.page() instanceof InteriorPage ? 1 : 0)) {
						p = new Position(p.page(), i);
						add(p);
						n = p.page() instanceof InteriorPage ip
								? p.index() == ip.getCellCount() ? ip.getRightMostPointer()
										: ((InteriorCell) p.cell()).leftChildPointer()
								: 0;
						break;
					} else if (isEmpty())
						return false;
					else {
						p = getLast();
						if (p.index() < p.page().getCellCount())
							return true;
					}
				}
			}

			while (n != 0) {
				var p = database.readBTreePage(n, database.allocatePage());
				add(new Position(p, 0));
				n = p instanceof InteriorPage<?> ip
						? ip.getCellCount() != 0 ? ip.getCells().getFirst().leftChildPointer()
								: ip.getRightMostPointer()
						: 0;
			}

			return true;
		}

//		public Path foo(int l) {
//			return new Path(subList(0, l));
//		}
	}
}

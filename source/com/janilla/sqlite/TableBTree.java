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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongFunction;
import java.util.function.UnaryOperator;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class TableBTree extends BTree<LeafTablePage, LeafTableCell> {

	public TableBTree(SQLiteDatabase database, long rootNumber) {
		super(database, rootNumber);
	}

	@Override
	public long insert(LongFunction<Object[]> values) {
		List<Page.Numbered<BTreePage<?>>> rr = new ArrayList<>();
		{
			var n = rootNumber;
			f: for (;;) {
				var b = database.allocatePage();
				var p = (BTreePage<?>) database.readBTreePage(n, b);
				rr.add(new Page.Numbered<>(n, p));
				switch (p) {
				case LeafTablePage _:
					break f;
				case InteriorTablePage x:
					n = x.getRightMostPointer();
					break;
				default:
					throw new RuntimeException();
				}
			}
		}

		var r = rr.removeLast();
		var p = (LeafTablePage) r.content();
		LeafTableCell c;
		{
			var cc = p.getCells();
			var k = (!cc.isEmpty() ? ((LeafTableCell) cc.getLast()).key() : 0) + 1;
			var vv = values.apply(k);
//			IO.println("TableBTree.insert, k=" + k + ", vv=" + Arrays.toString(vv));
			var l = (long) vv[0];
			if (l >= k)
				k = l;
			else
				throw new RuntimeException();
			var bb = Record.toBytes(Arrays.stream(vv).skip(1).map(Record.Element::of).toArray(Record.Element[]::new));
			var is = database.initialSize(bb.length, false);
			c = is == bb.length ? new LeafTableCell.New(bb.length, k, bb, 0)
					: new LeafTableCell.New(bb.length, k, Arrays.copyOf(bb, is), addOverflowPages(bb, is));
		}

		if (p.getCellCount() == 0)
			p.setCellContentAreaStart(database.usableSize());

		try {
			p.getCells().add(c);
			database.write(r);
		} catch (IllegalStateException e) {
			if (!rr.isEmpty()) {
				var n1 = rr.getLast().number();
				var p1 = (InteriorTablePage) rr.getLast().content();
				var n2 = database.nextPageNumber();
				var p2 = new LeafTablePage(database, database.allocatePage());

				p1.getCells().add(new InteriorTableCell.New(p1.getRightMostPointer(), c.key() - 1));
				p1.setRightMostPointer(n2);

				p2.setCellContentAreaStart(database.usableSize());
				p2.getCells().add(c);

				database.write(n1, p1.buffer());
				database.write(n2, p2.buffer());
			} else {
				if (r.number() == 1) {
					System.arraycopy(p.buffer().array(), 100, p.buffer().array(), 0, p.getCellContentAreaStart() - 100);
					p.buffer().position(0);
				}

				var n1 = r.number();
				var b1 = database.allocatePage();
				b1.position(n1 == 1 ? 100 : 0);
				var p1 = new InteriorTablePage(database, b1);
				var n2 = database.nextPageNumber();
				var p2 = p;
				var n3 = database.nextPageNumber();
				var p3 = new LeafTablePage(database, database.allocatePage());

				p1.setCellContentAreaStart(database.usableSize());
				p1.getCells().add(new InteriorTableCell.New(n2, c.key() - 1));
				p1.setRightMostPointer(n3);

				Arrays.fill(p2.buffer().array(), p2.buffer().position() + 8 + p2.getCellCount() * Short.BYTES,
						p2.getCellContentAreaStart(), (byte) 0);

				p3.setCellContentAreaStart(database.usableSize());
				p3.getCells().add(c);

				database.write(n1, p1.buffer());
				database.write(n2, p2.buffer());
				database.write(n3, p3.buffer());
			}
		}
		database.updateHeader();
		return c.key();
	}

	@Override
	public boolean insert(Object... keys) {
		insert(_ -> keys);
		return true;
	}

	@Override
	public Stream<Object[]> select(Object... keys) {
		var k = (long) keys[0];
//		IO.println("TableBTree.select, k=" + k);
		List<Page.Numbered<BTreePage<?>>> rr = new ArrayList<>();
		{
			var n = rootNumber;
			for (;;) {
				var b = database.allocatePage();
				var p = (BTreePage<?>) database.readBTreePage(n, b);
				rr.add(new Page.Numbered<>(n, p));
				if (p instanceof InteriorTablePage x)
					n = x.getCells().stream().filter(y -> k <= y.key()).findFirst().map(y -> y.leftChildPointer())
							.orElse(x.getRightMostPointer());
				else
					break;
			}
		}

		var p = (LeafTablePage) rr.removeLast().content();
		var c = p.getCells().stream().filter(x -> x.key() == k).findFirst().orElse(null);
		return c != null ? Stream.<Object[]>of(row(c)) : Stream.empty();
	}

	@Override
	public void update(Object[] keys, UnaryOperator<Object[]> operator) {
		var k = (long) keys[0];
		List<Page.Numbered<BTreePage<?>>> rr = new ArrayList<>();
		{
			var n = rootNumber;
			f: for (;;) {
				var b = database.allocatePage();
				var p = (BTreePage<?>) database.readBTreePage(n, b);
				rr.add(new Page.Numbered<>(n, p));
				switch (p) {
				case LeafTablePage _:
					break f;
				case InteriorTablePage x:
					n = x.getCells().stream().filter(y -> k <= y.key()).findFirst().map(y -> y.leftChildPointer())
							.orElse(x.getRightMostPointer());
					break;
				default:
					throw new RuntimeException();
				}
			}
		}

		var r = rr.removeLast();
		List<LeafTableCell> cc;
		LeafTableCell c;
		int ci;
		{
			var p = (LeafTablePage) r.content();
			cc = p.getCells();
			var cii = new int[] { -1 };
			c = cc.stream().peek(_ -> cii[0]++).filter(x -> x.key() == k).findFirst().orElse(null);
			ci = cii[0];
		}
		if (c == null)
			return;

		var oo1 = Record.fromBytes(payloadBuffers(c)).map(Record.Element::toObject).toArray();
		var oo2 = operator.apply(oo1);
		var bb = Record.toBytes(Arrays.stream(oo2).map(Record.Element::of).toArray(Record.Element[]::new));
		var is = database.initialSize(bb.length, false);
		if (is != bb.length)
			throw new RuntimeException();
		c = is == bb.length ? new LeafTableCell.New(bb.length, k, bb, 0)
				: new LeafTableCell.New(bb.length, k, Arrays.copyOf(bb, is), addOverflowPages(bb, is));
		cc.set(ci, c);
		database.write(r);
		database.updateHeader();
	}

	@Override
	public Object[] delete(Object... keys) {
		var k = (long) keys[0];
//		IO.println("key=" + key);
		List<Page.Numbered<BTreePage<?>>> rr = new ArrayList<>();
		{
			var n = rootNumber;
			f: for (;;) {
				var b = database.allocatePage();
				var p = (BTreePage<?>) database.readBTreePage(n, b);
				rr.add(new Page.Numbered<>(n, p));
				switch (p) {
				case LeafTablePage _:
					break f;
				case InteriorTablePage x:
					n = x.getCells().stream().filter(y -> k <= y.key()).findFirst().map(y -> y.leftChildPointer())
							.orElse(x.getRightMostPointer());
					break;
				default:
					throw new RuntimeException();
				}
			}
		}

		var r = rr.removeLast();
		List<LeafTableCell> cc;
		LeafTableCell c;
		int ci;
		{
			var p = (LeafTablePage) r.content();
			cc = p.getCells();
			var cii = new int[] { -1 };
			c = cc.stream().peek(_ -> cii[0]++).filter(x -> x.key() == k).findFirst().orElse(null);
			ci = cii[0];
		}
		if (c == null)
			return null;
		var oo = Record.fromBytes(payloadBuffers(c)).toArray();

		cc.remove(ci);
		if (cc.isEmpty() && !rr.isEmpty()) {
			var n1 = rr.getLast().number();
			var p1 = (InteriorTablePage) rr.getLast().content();
			var n2 = r.number();
//			IO.println(n2);
			var p2 = r.content();
			var nn0 = LongStream.concat(p1.getCells().stream().mapToLong(InteriorTableCell::leftChildPointer),
					LongStream.of(p1.getRightMostPointer())).toArray();
//			IO.println(Arrays.toString(nn0));
			var ni = (int) Arrays.stream(nn0).takeWhile(x -> x != n2).count();
			var ni0 = Math.max(ni - (ni == nn0.length - 1 ? 2 : 1), 0);
			var nn = Arrays.copyOfRange(nn0, ni0, Math.min(ni0 + 3, nn0.length));
//			IO.println("nn=" + Arrays.toString(nn));
			var pp = Arrays.stream(nn)
					.mapToObj(x -> x == n2 ? p2 : (LeafTablePage) database.readBTreePage(x, database.allocatePage()))
					.toArray(LeafTablePage[]::new);

			for (int si = 1, di = 0; si < pp.length;)
				if (pp[si].getCellCount() == 0)
					si++;
				else {
					try {
						pp[di].getCells().add(pp[si].getCells().getFirst());
					} catch (IllegalStateException e) {
						di++;
						if (si == di)
							si++;
						continue;
					}
					pp[si].getCells().removeFirst();
				}

			for (var i = 0; i < nn.length; i++) {
				if (pp[i].getCellCount() == 0) {
					var f = new FreelistPage(database, pp[i].buffer());
					f.setNext(0);
					f.getLeafPointers().clear();
					database.write(nn[i], f.buffer());
					p1.getCells().remove(ni0); // + i
				} else {
					database.write(nn[i], pp[i].buffer());
					p1.getCells().set(ni0 + i, new InteriorTableCell.New(nn[i], pp[i].getCells().getLast().key()));
				}
			}
			database.write(n1, p1.buffer());
		} else
			database.write(r);

		return oo;
	}

	public LongStream keys() {
		return cells().mapToLong(x -> ((LeafTableCell) x).key());
	}

	@Override
	public long count(Object... keys) {
		if (keys.length == 0)
			return count(false);
		throw new RuntimeException();
	}

	@Override
	public Stream<? extends Cell> cells() {
		return cells(false);
	}

	@Override
	public Object[] row(Cell cell) {
		var oo = super.row(cell);
		var oo2 = new Object[oo.length + 1];
		oo2[0] = ((LeafTableCell) cell).key();
		System.arraycopy(oo, 0, oo2, 1, oo.length);
		return oo2;
	}
}

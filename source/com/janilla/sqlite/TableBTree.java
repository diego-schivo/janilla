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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongFunction;
import java.util.function.UnaryOperator;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class TableBTree extends BTree {

	public TableBTree(SQLiteDatabase database, long rootNumber) {
		super(database, rootNumber);
	}

	public long insert(LongFunction<Object[]> values) {
//		IO.println(Arrays.toString(values));
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
//		IO.println("k=" + k);
			var bb = Record.toBytes(values.apply(k));
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

				p2.setType(0x0d);
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
				var p1 = new InteriorTablePage(database, database.allocatePage());
				p1.buffer().position(n1 == 1 ? 100 : 0);
				var n2 = database.nextPageNumber();
				var p2 = p;
				var n3 = database.nextPageNumber();
				var p3 = new LeafTablePage(database, database.allocatePage());

				p1.setType(0x05);
				p1.setCellContentAreaStart(database.usableSize());
				p1.getCells().add(new InteriorTableCell.New(n2, c.key() - 1));
				p1.setRightMostPointer(n3);

				Arrays.fill(p2.buffer().array(), p2.buffer().position() + 8 + p2.getCellCount() * Short.BYTES,
						p2.getCellContentAreaStart(), (byte) 0);

				p3.setType(0x0d);
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

	public Object[] select(long key) {
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
					n = x.getCells().stream().filter(y -> key <= y.key()).findFirst().map(y -> y.leftChildPointer())
							.orElse(p.getRightMostPointer());
					break;
				default:
					throw new RuntimeException();
				}
			}
		}

		var p = (LeafTablePage) rr.removeLast().content();
		var c = p.getCells().stream().filter(x -> x.key() == key).findFirst().orElse(null);
		if (c == null)
			return null;

		var bb = c.initialPayload();
		var r2 = c.payloadSize() - bb.length;
		if (r2 != 0) {
			bb = Arrays.copyOf(bb, c.payloadSize());
			var n = c.firstOverflow();
			var b = database.allocatePage();
			do {
				database.read(n, b);
				var p2 = new OverflowPage(database, b);
				var c2 = p2.getContent();
				var l = Math.min(c2.length, r2);
				System.arraycopy(c2, 0, bb, bb.length - r2, l);
				r2 -= l;
				n = p2.getNext();
			} while (r2 != 0);
		}
		return Record.fromBytes(bb);
	}

	public void update(long key, UnaryOperator<Object[]> operator) {
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
					n = x.getCells().stream().filter(y -> key <= y.key()).findFirst().map(y -> y.leftChildPointer())
							.orElse(p.getRightMostPointer());
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
			c = cc.stream().peek(_ -> cii[0]++).filter(x -> x.key() == key).findFirst().orElse(null);
			ci = cii[0];
		}
		if (c == null)
			return;

		var bb = c.initialPayload();
		if (c.payloadSize() != bb.length)
			throw new RuntimeException();

		bb = Record.toBytes(operator.apply(Record.fromBytes(bb)));
		var is = database.initialSize(bb.length, false);
		if (is != bb.length)
			throw new RuntimeException();
		c = is == bb.length ? new LeafTableCell.New(bb.length, key, bb, 0)
				: new LeafTableCell.New(bb.length, key, Arrays.copyOf(bb, is), addOverflowPages(bb, is));
		cc.set(ci, c);
		database.write(r);
		database.updateHeader();
	}

	public Object[] delete(long key) {
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
					n = x.getCells().stream().filter(y -> key <= y.key()).findFirst().map(y -> y.leftChildPointer())
							.orElse(p.getRightMostPointer());
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
			c = cc.stream().peek(_ -> cii[0]++).filter(x -> x.key() == key).findFirst().orElse(null);
			ci = cii[0];
		}
		if (c == null)
			return null;

		var bb = c.initialPayload();
		var r2 = c.payloadSize() - bb.length;
		if (r2 != 0) {
			bb = Arrays.copyOf(bb, c.payloadSize());
			var n = c.firstOverflow();
			var b = database.allocatePage();
			do {
				database.read(n, b);
				var p2 = new OverflowPage(database, b);
				var c2 = p2.getContent();
				var l = Math.min(c2.length, r2);
				System.arraycopy(c2, 0, bb, bb.length - r2, l);
				r2 -= l;
				n = p2.getNext();
			} while (r2 != 0);
		}

		cc.remove(ci);

		if (cc.isEmpty() && !rr.isEmpty()) {
			var n1 = rr.getLast().number();
			var p1 = (InteriorTablePage) rr.getLast().content();
			var n2 = r.number();
			IO.println(n2);
			var p2 = r.content();
			var nn0 = LongStream.concat(p1.getCells().stream().mapToLong(InteriorTableCell::leftChildPointer),
					LongStream.of(p1.getRightMostPointer())).toArray();
			IO.println(Arrays.toString(nn0));
			var ni = (int) Arrays.stream(nn0).takeWhile(x -> x != n2).count();
			var ni0 = Math.max(ni - (ni == nn0.length - 1 ? 2 : 1), 0);
			var nn = Arrays.copyOfRange(nn0, ni0, Math.min(ni0 + 3, nn0.length));
			IO.println("nn=" + Arrays.toString(nn));
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
					p1.getCells().remove(ni0 + i);
				} else {
					database.write(nn[i], pp[i].buffer());
					p1.getCells().set(ni0 + i, new InteriorTableCell.New(nn[i], pp[i].getCells().getLast().key()));
				}
			}
			database.write(n1, p1.buffer());
		} else
			database.write(r);

		return Record.fromBytes(bb);
	}

	public long count() {
		return leafPages().mapToLong(LeafTablePage::getCellCount).sum();
	}

	public LongStream keys() {
		return leafPages().flatMap(x -> x.getCells().stream()).mapToLong(LeafTableCell::key);
	}

	public Stream<Object[]> rows() {
		return leafPages().flatMap(x -> x.getCells().stream()).map(LeafTableCell::initialPayload)
				.map(Record::fromBytes);
	}

	protected Stream<LeafTablePage> leafPages() {
		var p = database.readBTreePage(rootNumber, database.allocatePage());
		return switch (p) {
		case InteriorTablePage x -> LongStream
				.concat(x.getCells().stream().mapToLong(InteriorTableCell::leftChildPointer),
						LongStream.of(x.getRightMostPointer()))
				.boxed().map(y -> (LeafTablePage) database.readBTreePage(y, database.allocatePage()));
		case LeafTablePage x -> Stream.of(x);
		default -> throw new RuntimeException();
		};
	}
}

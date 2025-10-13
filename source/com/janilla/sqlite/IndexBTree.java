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
import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class IndexBTree extends BTree<LeafIndexPage, LeafIndexCell> {

	public IndexBTree(SQLiteDatabase database, long rootNumber) {
		super(database, rootNumber);
	}

	@Override
	public long insert(LongFunction<Object[]> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean insert(Object... keys) {
		IO.println("keys=" + Arrays.toString(keys));

		var n = rootNumber;
		List<Page.Numbered<BTreePage<?>>> rr = new ArrayList<>();
		for (;;) {
			var b = database.allocatePage();
			var p = (BTreePage<?>) database.readBTreePage(n, b);
			rr.add(new Page.Numbered<>(n, p));
			if (p instanceof InteriorIndexPage p2) {
				try {
					n = p2.getCells().stream().filter(c -> {
						var d = compare(c, keys);
						if (d == 0)
							throw new IllegalArgumentException();
						return d > 0;
					}).mapToLong(InteriorCell::leftChildPointer).findFirst().orElseGet(p2::getRightMostPointer);
				} catch (IllegalArgumentException e) {
					return false;
				}
			} else
				break;
		}

		var r = rr.removeLast();
		var p = (LeafIndexPage) r.content();

		int ci;
		try {
			ci = (int) p.getCells().stream().takeWhile(c -> {
				var d = compare(c, keys);
				if (d == 0)
					throw new IllegalArgumentException();
				return d < 0;
			}).count();
		} catch (IllegalArgumentException e) {
			return false;
		}

		LeafIndexCell c;
		{
			var bb = Record.toBytes(Arrays.stream(keys).map(Record.Element::of).toArray(Record.Element[]::new));
			var is = database.initialSize(bb.length, true);
			c = is == bb.length ? new LeafIndexCell.New(bb.length, bb, 0)
					: new LeafIndexCell.New(bb.length, Arrays.copyOf(bb, is), addOverflowPages(bb, is));
		}

		try {
			p.getCells().add(ci, c);
			database.write(r.number(), p.buffer());
		} catch (IllegalStateException e) {
			long n1;
			InteriorIndexPage p1;
			if (!rr.isEmpty()) {
				n1 = rr.getLast().number();
				p1 = (InteriorIndexPage) rr.getLast().content();
			} else {
				n1 = r.number();
				p1 = new InteriorIndexPage(database, database.allocatePage());
				p1.setCellContentAreaStart(database.usableSize());
			}
			var n2 = !rr.isEmpty() ? r.number() : database.nextPageNumber();
			var p2 = p;
			var n3 = database.nextPageNumber();
			var p3 = new LeafIndexPage(database, database.allocatePage());
			p3.setCellContentAreaStart(database.usableSize());

			var i = p2.getCellCount();
			{
				var s = p2.getCells().stream().mapToInt(Cell::size).sum() + c.size();
				var s2 = s;
				for (; i >= 0; i--) {
					var x = i == ci ? c : p2.getCells().get(i < ci ? i : i - 1);
					s2 -= x.size();
					if (s2 < s / 2)
						break;
				}
				i++;
			}

			var n2i = (int) p1.getCells().stream().takeWhile(x -> x.leftChildPointer() != n2).count();
			var i2 = i < ci ? i : i == ci ? -1 : i - 1;
			var x = i2 != -1 ? p2.getCells().get(i2) : c;

			if (n2i < p1.getCellCount()) {
				var c0 = p1.getCells().get(n2i);
				var c2 = new InteriorIndexCell.New(n2, x.payloadSize(), x.initialPayload(database).array(),
						x.firstOverflow());
				var c3 = new InteriorIndexCell.New(n3, c0.payloadSize(), c0.initialPayload(database).array(),
						c0.firstOverflow());
				p1.getCells().set(n2i, c2);
				p1.getCells().add(n2i + 1, c3);
			} else {
				p1.getCells().add(new InteriorIndexCell.New(n2, x.payloadSize(), x.initialPayload(database).array(),
						x.firstOverflow()));
				p1.setRightMostPointer(n3);
			}

			if (i2 != -1)
				p2.getCells().remove(i2);

			var l2 = i - (i <= ci ? 0 : 1);
			while (p2.getCellCount() > l2) {
				x = p2.getCells().get(l2);
				p3.getCells().add(x);
				p2.getCells().remove(l2);
			}

			if (ci < i)
				p2.getCells().add(ci, c);
			else if (ci > i)
				p3.getCells().add(ci - i - 1, c);

			Arrays.fill(p2.buffer().array(), p2.buffer().position() + 12 + p2.getCellCount() * Short.BYTES,
					p2.getCellContentAreaStart(), (byte) 0);

			database.write(n1, p1.buffer());
			database.write(n2, p2.buffer());
			database.write(n3, p3.buffer());
		}
		database.updateHeader();
		return true;
	}

	@Override
	public Stream<Object[]> select(Object... keys) {
		return search(keys, s -> {
			if (!s.found())
				return Stream.empty();
//			var p = s.path().getLast();
//			return IntStream.range(p.index(), p.page().getCellCount()).mapToObj(ci -> {
//				var c = (PayloadCell) p.page().getCells().get(ci);
//				return compare(c, keys) == 0 ? row(c) : null;
//			}).takeWhile(Objects::nonNull);
			return Stream.iterate(s.path(), x -> x.next() ? x : null).takeWhile(Objects::nonNull).map(x -> {
				var p = x.getLast();
				var c = (PayloadCell) p.page().getCells().get(p.index());
				return compare(c, keys) == 0 ? row(c) : null;
			}).takeWhile(Objects::nonNull);
		});
	}

	public void update(Object[] keys, UnaryOperator<Object[]> operator) {
		search(keys, x -> {
			if (!x.found())
				return null;

			var p = x.path().getLast();
			var c = (PayloadCell) p.page().getCells().get(p.index());

			var oo1 = Record.fromBytes(payloadBuffers(c)).map(Record.Element::toObject).toArray();
			var oo2 = operator.apply(oo1);
			var bb = Record.toBytes(Arrays.stream(oo2).map(Record.Element::of).toArray(Record.Element[]::new));
			var is = database.initialSize(bb.length, true);
			c = is == bb.length ? new LeafIndexCell.New(bb.length, bb, 0)
					: new LeafIndexCell.New(bb.length, Arrays.copyOf(bb, is),
							addOverflowPages(bb, is, LongStream.iterate(c.firstOverflow(), n -> {
								var b = database.allocatePage();
								database.read(n, b);
								return new OverflowPage(database, b).getNext();
							}).takeWhile(n -> n != 0)));
			((List<Cell>) p.page().getCells()).set(p.index(), c);
			database.write(rootNumber, p.page().buffer());
			database.updateHeader();
			return null;
		});
	}

	@Override
	public Object[] delete(Object... keys) {
		var p = (LeafIndexPage) database.readBTreePage(rootNumber, database.allocatePage());
		var ci = IntStream.range(0, p.getCellCount()).filter(x -> {
			var ii = new int[] { -1 };
			var k = Record.fromBytes(payloadBuffers(p.getCells().get(x))).limit(keys.length)
					.filter(y -> !Objects.equals(y, keys[++ii[0]])).findFirst();
			return k.isEmpty();
		}).findFirst().orElse(-1);
		if (ci == -1)
			return null;

		var c = p.getCells().get(ci);
		var oo = Record.fromBytes(payloadBuffers(c)).toArray();

		var fo = c.firstOverflow();
		if (fo != 0)
			removeOverflowPages(fo);
		p.getCells().remove(ci);
		database.write(rootNumber, p.buffer());
		database.updateHeader();

		return oo;
	}

	public LongStream ids(Object... keys) {
		if (keys.length == 0)
			return cells().mapToLong(
					c -> (long) Record.fromBytes(payloadBuffers((PayloadCell) c)).reduce((_, x) -> x).get().toObject());
		return search(keys, s -> {
			if (s.found()) {
				var p = s.path().getLast();
				return IntStream.range(p.index(), p.page().getCellCount()).mapToLong(ci -> {
					var c = p.page().getCells().get(ci);
					return compare(c, keys) == 0
							? (long) Record.fromBytes(payloadBuffers((PayloadCell) c)).reduce((_, x) -> x).get()
									.toObject()
							: 0;
				}).takeWhile(x -> x != 0);
			}
			return LongStream.empty();
		});
	}

	@Override
	public long count(Object... keys) {
		if (keys.length == 0)
			return count(true);
		return search(keys, x -> {
			if (!x.found())
				return 0l;
			var p = x.path().getLast();
			return 1 + (p.page()).getCells().stream().skip(p.index()).filter(c -> {
				var ii = new int[] { -1 };
				return Record.fromBytes(payloadBuffers((PayloadCell) c)).limit(keys.length)
						.allMatch(k -> Objects.equals(k, keys[++ii[0]]));
			}).count();
		});
	}

	@Override
	public Stream<? extends Cell> cells() {
		return cells(true);
	}

	protected <R> R search(Object[] keys, Function<Search, R> function) {
//		IO.println("keys=" + Arrays.toString(keys));
		var n = rootNumber;
		var f = false;
		var pp = new Path();
		f: for (;;) {
			var p = (BTreePage<?>) database.readBTreePage(n, database.allocatePage());
			var ci = 0;
			for (var c : p.getCells()) {
				var d = compare(c, keys);
//				if (d == 0) {
//					f = true;
//					pp.add(new Position(p, ci));
//					break f;
//				}
				if (c instanceof InteriorCell c2) {
					if (d >= 0) {
						pp.add(new Position(p, ci));
						n = c2.leftChildPointer();
						continue f;
					}
				}
				if (d == 0)
					f = true;
				if (d >= 0) {
					pp.add(new Position(p, ci));
					if (c instanceof InteriorCell c2) {
						n = c2.leftChildPointer();
						continue f;
					} else {
						break f;
					}
				}
				ci++;
			}
			if (p instanceof InteriorIndexPage p2) {
				pp.add(new Position(p, ci));
				n = p2.getRightMostPointer();
			} else
				break;
		}
		return function.apply(new Search(f, pp));
	}

	protected int compare(Cell cell, Object[] keys) {
		var a = Record.fromBytes(payloadBuffers((PayloadCell) cell)).iterator();
		var b = Arrays.stream(keys).map(Record.Element::of).iterator();
		return Record.compare(a, b);
	}

	public record Search(boolean found, Path path) {
	}
}

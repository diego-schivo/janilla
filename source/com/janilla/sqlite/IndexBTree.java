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
import java.util.Objects;
import java.util.function.LongFunction;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class IndexBTree extends BTree<IndexLeafPage, IndexLeafCell> {

	public IndexBTree(SQLiteDatabase database, long rootNumber) {
		super(database, rootNumber);
	}

	@Override
	public long insert(LongFunction<Object[]> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean insert(Object[] key, Object[] data) {
//		IO.println("key=" + Arrays.toString(key) + ", data=" + Arrays.toString(data));

		var s = search(key);
		if (s.found() != -1)
			return false;

		var pi = s.path().removeLast();
		var p = (IndexLeafPage) pi.page();
		var i = pi.index();
		IndexLeafCell c;
		{
			var bb = Record.toBytes(Stream.concat(Arrays.stream(key), Arrays.stream(data)).map(Record.Element::of)
					.toArray(Record.Element[]::new));
			var is = database.initialSize(bb.length, true);
			c = is == bb.length ? new IndexLeafCell.New(bb.length, bb, 0)
					: new IndexLeafCell.New(bb.length, Arrays.copyOf(bb, is), addOverflowPages(bb, is));
		}

		try {
			p.getCells().add(i, c);
			database.write(s.path().number(s.path().size()), p.buffer());
		} catch (IllegalStateException e) {
			var cc = new ArrayList<>(p.getCells());
			cc.add(i, c);
			new IndexRebalance(s.path(), p, cc, database);
		}
		database.updateHeader();
		return true;
	}

	@Override
	public Stream<Object[]> select(Object... keys) {
		var s = search(keys);
		if (s.found() == -1)
			return Stream.empty();
		return Stream.iterate(s.path(), x -> x.next() ? x : null).takeWhile(Objects::nonNull).map(x -> {
			var p = x.getLast();
			var c = (PayloadCell) p.page().getCells().get(p.index());
			return compare(c, keys) == 0 ? row(c) : null;
		}).takeWhile(Objects::nonNull);
	}

	@Override
	public void delete(Object... key) {
		IO.println("key=" + Arrays.toString(key));

		var s = search(key);
		if (s.found() == -1)
			return;

		IndexLeafCell c;
		if (s.found() == s.path().size() - 1)
			c = null;
		else {
			var pi = s.path().removeLast();
			var p = (IndexLeafPage) pi.page();
			var i = pi.index() - 1;
			pi = new Position(p, i);
			s.path().add(pi);
			c = p.getCells().get(i);
			c = new IndexLeafCell.New(c.payloadSize(), c.initialPayload(database).array(), c.firstOverflow());
		}

		{
			var pi = s.path().removeLast();
			var p = (IndexLeafPage) pi.page();
			var i = pi.index();
			p.getCells().remove(i);

			if (!s.path().isEmpty() && 3
					* (8 + p.getCells().stream().mapToInt(x -> Short.BYTES + x.size()).sum()) < database.usableSize()) {
				new IndexRebalance(s.path(), p, p.getCells(), database);
				if (c != null)
					s = search(key);
			} else
				database.write(s.path().number(s.path().size()), p.buffer());
		}

		if (c != null) {
			while (s.path().size() != s.found() + 1)
				s.path().removeLast();
			var n = s.path().number(s.found());
			var pi = s.path().removeLast();
			@SuppressWarnings("unchecked")
			var p = (BTreePage<PayloadCell>) pi.page();
			var i = pi.index();
			var c1 = (PayloadCell) p.getCells().get(i);
			c1 = c1 instanceof IndexInteriorCell x ? new IndexInteriorCell.New(x.leftChildPointer(), c.payloadSize(), c.initialPayload(database).array(),
					c.firstOverflow()) :new IndexLeafCell.New(c.payloadSize(), c.initialPayload(database).array(),
							c.firstOverflow()) ;
			p.getCells().remove(i);
			try {
				p.getCells().add(i, c1);
				database.write(n, p.buffer());
			} catch (IllegalStateException e) {
				var cc = new ArrayList<>(p.getCells());
				cc.add(i, c1);
				new IndexRebalance(s.path(), p, cc, database);
//				throw new RuntimeException();
			}
		}

		database.updateHeader();
	}

	public LongStream ids(Object... keys) {
		if (keys.length == 0)
			return cells().mapToLong(
					c -> (long) Record.fromBytes(payloadBuffers((PayloadCell) c)).reduce((_, x) -> x).get().toObject());
		var s = search(keys);
		if (s.found() == -1)
			return LongStream.empty();
		var p = s.path().getLast();
		return IntStream.range(p.index(), p.page().getCellCount()).mapToLong(ci -> {
			var c = p.page().getCells().get(ci);
			return compare(c, keys) == 0
					? (long) Record.fromBytes(payloadBuffers((PayloadCell) c)).reduce((_, x) -> x).get().toObject()
					: 0;
		}).takeWhile(x -> x != 0);
	}

	@Override
	public long count(Object... keys) {
		if (keys.length == 0)
			return count(true);
		var x = search(keys);
		if (x.found() == -1)
			return 0l;
		var p = x.path().getLast();
		return 1 + (p.page()).getCells().stream().skip(p.index()).filter(c -> {
			var ii = new int[] { -1 };
			return Record.fromBytes(payloadBuffers((PayloadCell) c)).limit(keys.length)
					.allMatch(k -> Objects.equals(k, keys[++ii[0]]));
		}).count();
	}

	@Override
	public Stream<? extends Cell> cells() {
		return cells(true);
	}

	protected Search search(Object[] keys) {
//		IO.println("keys=" + Arrays.toString(keys));
		var n = rootNumber;
		var f = -1;
		var pp = new Path();
		f: for (var i = 0;; i++) {
			var p = (BTreePage<?>) database.readBTreePage(n, database.allocatePage());
			var ci = 0;
			for (var c : p.getCells()) {
				var d = compare(c, keys);
				if (d == 0)
					f = i;
//				if (c instanceof InteriorCell c2) {
//					if (d >= 0) {
//						pp.add(new Position(p, ci));
//						n = c2.leftChildPointer();
//						continue f;
//					}
//				}
				if (d >= 0) {
					pp.add(new Position(p, ci));
					if (c instanceof InteriorCell c2) {
						n = c2.leftChildPointer();
						continue f;
					} else
						break f;
				}
				ci++;
			}
			pp.add(new Position(p, ci));
			if (p instanceof IndexInteriorPage p2)
				n = p2.getRightMostPointer();
			else
				break;
		}
		return new Search(pp, f);
	}

	protected int compare(Cell cell, Object[] keys) {
		var a = Record.fromBytes(payloadBuffers((PayloadCell) cell)).iterator();
		var b = Arrays.stream(keys).map(Record.Element::of).iterator();
		return Record.compare(a, b);
	}

//	protected int[] balance(int[] ss, int u) {
//		IO.println("ss=" + Arrays.toString(ss) + " " + ss.length);
//		var s = Arrays.stream(ss).sum();
	////		IO.println("s=" + s + ", u=" + u);

//		var l = Math.ceilDiv(s, u);
//		var d = (double) s / l;
//		var ii = new int[l];
//		s = 0;
//		var i = 0;
//		for (var si = 0; si < ss.length; si++) {
//			s += ss[si];
//			if (s > (i + 1) * d) {
	////				IO.println("s=" + s + ", d2=" + d2);
//				i++;
//			} else
//				ii[i]++;
//		}
//		IO.println("ii=" + Arrays.toString(ii));
//		return ii;
//	}

	public record Search(Path path, int found) {
	}
}

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
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class IndexBTree extends BTree<IndexLeafPage, IndexLeafCell> {

	public IndexBTree(SqliteDatabase database, long rootNumber) {
		super(database, rootNumber);
	}

	@Override
	public boolean select(Object[] key, Consumer<Stream<Object[]>> rowsOperation) {
		var s = search(key);
		if (s.found() == -1)
			return false;

		if (rowsOperation != null)
			rowsOperation.accept(s.rows());

		return true;
	}

	@Override
	public long count(Object... key) {
		if (key.length == 0)
			return count(true);
		var x = search(key);
		if (x.found() == -1)
			return 0l;
		var p = x.path().getLast();
		return 1 + (p.page()).getCells().stream().skip(p.index()).filter(c -> {
			var ii = new int[] { -1 };
			return Record.fromBytes(payloadBuffers((PayloadCell) c)).limit(key.length)
					.allMatch(k -> Objects.equals(k, key[++ii[0]]));
		}).count();
	}

	@Override
	public boolean insert(Object[] key, Object[] data) {
//		IO.println("IndexBTree.insert, key=" + Arrays.toString(key) + ", data=" + Arrays.toString(data));

		var s = search(key);
		if (s.found() != -1)
			return false;

		var pi = s.path().removeLast();
		var p = (IndexLeafPage) pi.page();
		var i = pi.index();
		IndexLeafCell c;
		{
			var bb = Record
					.toBytes(Stream.concat(Arrays.stream(key), data != null ? Arrays.stream(data) : Stream.empty())
							.map(RecordColumn::of).toArray(RecordColumn[]::new));
			var is = database.computePayloadInitialSize(bb.length, true);
			c = is == bb.length ? new SimpleIndexLeafCell(bb.length, bb, 0)
					: new SimpleIndexLeafCell(bb.length, Arrays.copyOf(bb, is), addOverflowPages(bb, is));
		}

		try {
			p.getCells().add(i, c);
			database.writePageBuffer(s.path().pointer(s.path().size()), p.buffer());
		} catch (IllegalStateException e) {
			var cc = new ArrayList<>(p.getCells());
			cc.add(i, c);
			new IndexRebalancer(s.path(), p, cc).run();
		}

		database.updateHeader();
		return true;
	}

	@Override
	public boolean delete(Object[] key, Consumer<Stream<Object[]>> rowsOperation) {
//		IO.println("key=" + Arrays.toString(key));

		var s = search(key);
		if (s.found() == -1)
			return false;

		if (rowsOperation != null)
			rowsOperation.accept(s.rows());

		IndexLeafCell c;
		if (s.found() == s.path().size() - 1)
			c = null;
		else {
			var pi = s.path().removeLast();
			var p = (IndexLeafPage) pi.page();
			var i = pi.index() - 1;
			pi = new BTreePosition(p, i);
			s.path().add(pi);
			c = p.getCells().get(i);
			c = new SimpleIndexLeafCell(c.payloadSize(), c.toInitialPayloadBytes(), c.firstOverflow());
		}

		{
			var pi = s.path().removeLast();
			var p = (IndexLeafPage) pi.page();
			var i = pi.index();
			p.getCells().remove(i);

			if (!s.path().isEmpty() && 3
					* (8 + p.getCells().stream().mapToInt(x -> Short.BYTES + x.size()).sum()) < database.usableSize()) {
				new IndexRebalancer(s.path(), p, p.getCells()).run();
				if (c != null)
					s = search(key);
			} else
				database.writePageBuffer(s.path().pointer(s.path().size()), p.buffer());
		}

		if (c != null) {
			while (s.path().size() != s.found() + 1)
				s.path().removeLast();
			var n = s.path().pointer(s.found());
			var pi = s.path().removeLast();
			@SuppressWarnings("unchecked")
			var p = (BTreePage<PayloadCell>) pi.page();
			var i = pi.index();
			var c1 = (PayloadCell) p.getCells().get(i);
			c1 = c1 instanceof IndexInteriorCell x
					? new SimpleIndexInteriorCell(x.leftChildPointer(), c.payloadSize(),
							c.toInitialPayloadBytes(), c.firstOverflow())
					: new SimpleIndexLeafCell(c.payloadSize(), c.toInitialPayloadBytes(), c.firstOverflow());
			p.getCells().remove(i);
			try {
				p.getCells().add(i, c1);
				database.writePageBuffer(n, p.buffer());
			} catch (IllegalStateException e) {
				var cc = new ArrayList<>(p.getCells());
				cc.add(i, c1);
				new IndexRebalancer(s.path(), p, cc).run();
//				throw new RuntimeException();
			}
		}

		database.updateHeader();

		return true;
	}

	public LongStream ids(Object... key) {
		if (key.length == 0)
			return cells().mapToLong(
					c -> (long) Record.fromBytes(payloadBuffers((PayloadCell) c)).reduce((_, x) -> x).get().toObject());
		var s = search(key);
		if (s.found() == -1)
			return LongStream.empty();
		var p = s.path().getLast();
		return IntStream.range(p.index(), p.page().getCellCount()).mapToLong(ci -> {
			var c = p.page().getCells().get(ci);
			return compare(c, key) == 0
					? (long) Record.fromBytes(payloadBuffers((PayloadCell) c)).reduce((_, x) -> x).get().toObject()
					: 0;
		}).takeWhile(x -> x != 0);
	}

	@Override
	public Stream<? extends Cell> cells() {
		return cells(true);
	}

	protected Search search(Object... key) {
//		IO.println("IndexBTree.search, key=" + Arrays.toString(key));
		var n = rootNumber;
		var f = -1;
		var pp = new BTreePath(this);
		f: for (var i = 0;; i++) {
			var p = BTreePage.read(n, database);
			var ci = 0;
			for (var c : p.getCells()) {
				var d = compare(c, key);
				if (d == 0)
					f = i;
				if (d >= 0) {
					pp.add(new BTreePosition(p, ci));
					if (c instanceof InteriorCell c2) {
						n = c2.leftChildPointer();
						continue f;
					} else
						break f;
				}
				ci++;
			}
			pp.add(new BTreePosition(p, ci));
			if (p instanceof InteriorPage p2)
				n = p2.getRightMostPointer();
			else
				break;
		}
//		IO.println("IndexBTree.search, f=" + f);
		var rr = f != -1 ? Stream.iterate(pp, x -> x.next() ? x : null).takeWhile(Objects::nonNull).filter(x -> {
			var pi = x.getLast();
			return pi.index() != pi.page().getCellCount();
		}).map(x -> {
			var c = x.getLast().cell();
			return compare(c, key) == 0 ? row((PayloadCell) c) : null;
		}).takeWhile(Objects::nonNull) : Stream.<Object[]>empty();
		return new Search(pp, f, rr);
	}

	public record Search(BTreePath path, int found, Stream<Object[]> rows) {
	}

	protected int compare(Cell cell, Object[] keys) {
		var a = Record.fromBytes(payloadBuffers((PayloadCell) cell)).iterator();
		var b = Arrays.stream(keys).map(RecordColumn::of).iterator();
		return Record.compare(a, b);
	}
}

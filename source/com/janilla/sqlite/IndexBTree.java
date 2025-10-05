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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class IndexBTree extends BTree {

	private static final Comparator<?> NULLS_FIRST = Comparator.nullsFirst(Comparator.naturalOrder());

	public IndexBTree(SQLiteDatabase database, long rootNumber) {
		super(database, rootNumber);
	}

	public void insert(Object... keys) {
		var p = (LeafIndexPage) database.readBTreePage(rootNumber, database.allocatePage());

		int ci;
		try {
			ci = (int) p.getCells().stream().takeWhile(x -> {
				var kk = Record.fromBytes(payload(x));
				var i = IntStream.range(0, kk.length).filter(y -> !Objects.equals(kk[y], keys[y])).findFirst()
						.orElse(-1);
				if (i == -1)
					throw new IllegalStateException();
				@SuppressWarnings({ "rawtypes", "unchecked" })
				var y = ((Comparator) NULLS_FIRST).compare(kk[i], keys[i]);
				return y < 0;
			}).count();
		} catch (IllegalStateException e) {
			throw new UniqueConstraintFailedException();
		}

		LeafIndexCell c;
		{
			var bb = Record.toBytes(keys);
			var is = database.initialSize(bb.length, true);
			c = is == bb.length ? new LeafIndexCell.New(bb.length, bb, 0)
					: new LeafIndexCell.New(bb.length, Arrays.copyOf(bb, is), addOverflowPages(bb, is));
		}

		try {
			p.getCells().add(ci, c);
			database.write(rootNumber, p.buffer());
		} catch (IllegalStateException e) {
			var n1 = rootNumber;
			var p1 = new InteriorIndexPage(database, database.allocatePage());
			var n2 = database.nextPageNumber();
			var p2 = p;
			var n3 = database.nextPageNumber();
			var p3 = new LeafIndexPage(database, database.allocatePage());

			p3.setType(0x0a);
			p3.setCellContentAreaStart(database.usableSize());

			var s2 = p2.getCells().stream().mapToInt(Cell::size).sum();
			var s3 = 0;
			for (;;) {
				var c2 = p2.getCells().getLast();
				if (s3 + c2.size() >= s2 - c.size())
					break;
				p3.getCells().add(0, c2);
				p2.getCells().removeLast();
			}

			p1.setType(0x02);
			p1.setCellContentAreaStart(database.usableSize());
			var z = (LeafIndexCell) p2.getCells().getLast();
			p1.getCells().add(new InteriorIndexCell.New(n2, z.payloadSize(), z.initialPayload(), z.firstOverflow()));
			p1.setRightMostPointer(n3);

			Arrays.fill(p2.buffer().array(), p2.buffer().position() + 12 + p2.getCellCount() * Short.BYTES,
					p2.getCellContentAreaStart(), (byte) 0);

			p3.setType(0x0a);
			p3.setCellContentAreaStart(database.usableSize());
			p3.getCells().add(c);

			database.write(n1, p1.buffer());
			database.write(n2, p2.buffer());
			database.write(n3, p3.buffer());
		}
		database.updateHeader();
	}

	public Object[] select(Object... keys) {
		var p = (LeafIndexPage) database.readBTreePage(rootNumber, database.allocatePage());
		var ci = IntStream.range(0, p.getCellCount()).filter(x -> {
			var kk = Record.fromBytes(payload(p.getCells().get(x)));
			return IntStream.range(0, keys.length).allMatch(y -> Objects.equals(kk[y], keys[y]));
		}).findFirst().orElse(-1);
		return ci != -1 ? Record.fromBytes(payload(p.getCells().get(ci))) : null;
	}

	public void update(Object[] keys, UnaryOperator<Object[]> operator) {
		var p = (LeafIndexPage) database.readBTreePage(rootNumber, database.allocatePage());
		var ci = IntStream.range(0, p.getCellCount()).filter(x -> {
			var kk = Record.fromBytes(payload(p.getCells().get(x)));
			return IntStream.range(0, keys.length).allMatch(y -> Objects.equals(kk[y], keys[y]));
		}).findFirst().orElse(-1);
		if (ci == -1)
			return;

		var c = p.getCells().get(ci);
		var fo = c.firstOverflow();
		if (fo != 0)
			throw new RuntimeException();

		var bb = Record.toBytes(operator.apply(Record.fromBytes(payload(c))));
		var is = database.initialSize(bb.length, true);
		if (is != bb.length)
			throw new RuntimeException();
		c = is == bb.length ? new LeafIndexCell.New(bb.length, bb, 0)
				: new LeafIndexCell.New(bb.length, Arrays.copyOf(bb, is), addOverflowPages(bb, is));
		p.getCells().set(ci, c);
		database.write(rootNumber, p.buffer());
		database.updateHeader();
	}

	public void delete(Object... keys) {
		var p = (LeafIndexPage) database.readBTreePage(rootNumber, database.allocatePage());
		var ci = IntStream.range(0, p.getCellCount()).filter(x -> {
			var kk = Record.fromBytes(payload(p.getCells().get(x)));
			return IntStream.range(0, keys.length).allMatch(y -> Objects.equals(kk[y], keys[y]));
		}).findFirst().orElse(-1);
		if (ci == -1)
			return;

		var fo = p.getCells().get(ci).firstOverflow();
		if (fo != 0)
			removeOverflowPages(fo);

		p.getCells().remove(ci);
		database.write(rootNumber, p.buffer());
		database.updateHeader();
	}

	public LongStream foo() {
		var p = (LeafIndexPage) database.readBTreePage(rootNumber, database.allocatePage());
		return p.getCells().stream().map(LeafIndexCell::initialPayload).map(Record::fromBytes)
				.mapToLong(x -> (long) x[1]);
	}

	public LongStream foo(Object k1) {
		var p = (LeafIndexPage) database.readBTreePage(rootNumber, database.allocatePage());
		return p.getCells().stream().map(LeafIndexCell::initialPayload).map(Record::fromBytes)
				.filter(x -> Objects.equals(x[0], k1)).mapToLong(x -> (long) x[1]);
	}
}

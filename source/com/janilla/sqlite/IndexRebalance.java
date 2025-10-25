package com.janilla.sqlite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IndexRebalance {

	private IndexInteriorPage p1;

	public IndexRebalance(BTree<?, ?>.Path path, BTreePage<? extends PayloadCell> page,
			List<? extends PayloadCell> cells, SQLiteDatabase database) {
		p1 = !path.isEmpty() ? (IndexInteriorPage) path.getLast().page() : null;

		record R(int i, long n, BTreePage<? extends PayloadCell> p) {
		}
		R[] rr;
		if (path.isEmpty())
			rr = new R[] { new R(0, path.number(0), page) };
		else {
			var i = path.getLast().index();
			var l = p1.getCellCount() + 1;
			var i1 = Math.max(0, i == l - 1 ? l - 3 : i - 1);
			var i2 = Math.min(i1 + 2, l - 1);
			rr = IntStream.rangeClosed(i1, i2).mapToObj(x -> {
				var n = p1.childPointer(x);
				@SuppressWarnings("unchecked")
				var p = x == i ? page
						: (BTreePage<? extends PayloadCell>) database.readBTreePage(n, database.allocatePage());
				return new R(x, n, p);
			}).toArray(R[]::new);
		}
		var cc2 = Arrays.stream(rr)
				.flatMap(x -> Stream.concat(x.p == page ? cells.stream() : x.p.getCells().stream(),
						Optional.ofNullable(x != rr[rr.length - 1] ? p1.getCells().get(x.i) : null)
								.map(y -> page instanceof IndexInteriorPage
										? new IndexInteriorCell.New(((IndexInteriorPage) x.p).getRightMostPointer(),
												y.payloadSize(), y.initialPayload(database).array(), y.firstOverflow())
										: new IndexLeafCell.New(y.payloadSize(), y.initialPayload(database).array(),
												y.firstOverflow()))
								.stream()))
				.toList();

		var ll = database.balance(cc2.stream().mapToInt(x -> Short.BYTES + x.size()).toArray(),
				page instanceof IndexInteriorPage);
		if (p1 == null && ll.length != 1) {
			p1 = new IndexInteriorPage(database, database.allocatePage());
			p1.setCellContentAreaStart(database.usableSize());
		}
		var cc1 = p1 != null ? p1.getCells() : null;

		var i2 = 0;
		for (var li = 0; li < ll.length; li++) {
//			var n = !path.isEmpty() && li < rr.length ? rr[li].n : database.newPage();
			var n = li < rr.length && (!path.isEmpty() || p1 == null) ? rr[li].n : database.newPage();
			{
				var x = page instanceof IndexInteriorPage ? new IndexInteriorPage(database, database.allocatePage())
						: new IndexLeafPage(database, database.allocatePage());
				x.setCellContentAreaStart(database.usableSize());
				x.getCells().addAll(cc2.subList(i2, i2 += ll[li]));
				if (x instanceof IndexInteriorPage y)
					y.setRightMostPointer(i2 != cc2.size() ? ((IndexInteriorCell) cc2.get(i2)).leftChildPointer()
							: ((IndexInteriorPage) rr[rr.length - 1].p).getRightMostPointer());
				database.write(n, x.buffer());
			}
			if (i2 != cc2.size()) {
				var x = cc2.get(i2++);
				var y = new IndexInteriorCell.New(n, x.payloadSize(), x.initialPayload(database).array(),
						x.firstOverflow());
				if (!path.isEmpty() && li < rr.length - 1)
					cc1.remove(rr[li].i);
				try {
					cc1.add(rr[0].i + li, y);
				} catch (IllegalStateException e) {
					cc1 = new ArrayList<>(cc1);
					cc1.add(rr[0].i + li, y);
				}
			} else if (ll.length != rr.length) {
				for (var ri = ll.length; ri < rr.length; ri++) {
					cc1.remove(rr[ll.length - 1].i);
					database.freePage(rr[ri].n, rr[ri].p.buffer());
				}
				if (!path.isEmpty() && rr[0].i + li != cc1.size()) {
					var x = cc1.get(rr[0].i + li);
					var y = new IndexInteriorCell.New(n, x.payloadSize(), x.initialPayload(database).array(),
							x.firstOverflow());
					cc1.remove(rr[0].i + li);
					cc1.add(rr[0].i + li, y);
				} else
					p1.setRightMostPointer(n);
			}
		}
//		for (var ri = ll.length; ri < rr.length; ri++) {
//			cc1.remove(rr[ll.length - 1].i);
//			database.freePage(rr[ri].n, rr[ri].p.buffer());
//		}
//		if (rr[ll.length - 1].i == p1.getCellCount())
//			p1.setRightMostPointer(rr[ll.length - 1].n);
//		else {
//			var x = p1.getCells().get(rr[ll.length - 1].i);
//			p1.getCells().set(rr[ll.length - 1].i, new IndexInteriorCell.New(rr[ll.length - 1].n, x.payloadSize(),
//					x.initialPayload(database).array(), x.firstOverflow()));
//		}

		if (p1 == null)
			;
		else if (cc1 == p1.getCells() && !(ll.length < rr.length && path.size() > 1
				&& 3 * (12 + cc1.stream().mapToInt(x -> Short.BYTES + x.size()).sum()) < database.usableSize()))
			database.write(path.number(!path.isEmpty() ? path.size() - 1 : 0), p1.buffer());
		else {
			path.removeLast();
			new IndexRebalance(path, p1, cc1, database);
		}
	}
}

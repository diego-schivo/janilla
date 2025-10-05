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
import java.util.AbstractList;
import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.stream.IntStream;

public abstract class BTreePage<C extends Cell> extends Page {

	private FreeblockList freeblocks = new FreeblockList();

	private CellPointerList cellPointers = new CellPointerList();

	private CellList cells = new CellList();

	protected BTreePage(SQLiteDatabase database, ByteBuffer buffer) {
		super(database, buffer);
	}

	public int getType() {
		return Byte.toUnsignedInt(buffer.get(buffer.position()));
	}

	public void setType(int type) {
		buffer.put(buffer.position(), (byte) type);
	}

	public List<Freeblock> getFreeblocks() {
		return freeblocks;
	}

	public int getCellCount() {
		return Short.toUnsignedInt(buffer.getShort(buffer.position() + 3));
	}

	public void setCellCount(int cellCount) {
		buffer.putShort(buffer.position() + 3, (short) cellCount);
	}

	public int getCellContentAreaStart() {
		return Short.toUnsignedInt(buffer.getShort(buffer.position() + 5));
	}

	public void setCellContentAreaStart(int cellContentAreaStart) {
		buffer.putShort(buffer.position() + 5, (short) cellContentAreaStart);
	}

	public int getFragmentedSize() {
		return Byte.toUnsignedInt(buffer.get(buffer.position() + 7));
	}

	public void setFragmentedSize(int fragmentedSize) {
		buffer.put(buffer.position() + 7, (byte) fragmentedSize);
	}

	public long getRightMostPointer() {
		return Integer.toUnsignedLong(buffer.getInt(buffer.position() + 8));
	}

	public void setRightMostPointer(long rightMostPointer) {
		buffer.putInt(buffer.position() + 8, (int) rightMostPointer);
	}

	public List<C> getCells() {
		return cells;
	}

	protected abstract C cell(int index);

	private class FreeblockList extends AbstractSequentialList<Freeblock> {

		public void insert(Freeblock freeblock) {
			if (freeblock.start() == getCellContentAreaStart()) {
				var s = freeblock.end();
				Arrays.fill(buffer.array(), freeblock.start(), s, (byte) 0);
				var f = !isEmpty() ? getFirst() : null;
				var d = f != null ? f.start() - s : 4;
				if (d < 4) {
					s = f.end();
					removeFirst();
					if (d > 0)
						setFragmentedSize(getFragmentedSize() - d);
				}
				setCellContentAreaStart(s);
			} else {
				var fi = insertionIndex(freeblock);
				var a = true;
				if (!isEmpty()) {
					if (fi != 0) {
						var f1 = get(fi - 1);
						var f2 = freeblock;
						var d = f2.start() - f1.end();
						if (d < 4) {
							freeblock = new Freeblock.New(f1.start(), f2.end() - f1.start());
							set(fi - 1, freeblock);
							if (d > 0)
								setFragmentedSize(getFragmentedSize() - d);
							a = false;
						}
					}
					if (fi != size()) {
						var f1 = freeblock;
						var f2 = get(fi);
						var d = f2.start() - f1.end();
						if (d < 4) {
							freeblock = new Freeblock.New(f1.start(), f2.end() - f1.start());
							set(fi, freeblock);
							if (d > 0)
								setFragmentedSize(getFragmentedSize() - d);
							a = false;
						}
					}
				}
				if (a)
					add(fi, freeblock);
			}
		}

		public int insertionIndex(Freeblock freeblock) {
			return (int) starts().takeWhile(x -> x < freeblock.start()).count();
		}

		public int minSizeIndex(int size) {
			var ss = starts().map(x -> Short.toUnsignedInt(buffer.getShort(x + Short.BYTES)));
			var ii = new int[] { -1 };
			return ss.peek(_ -> ii[0]++).filter(x -> x >= size).findFirst().isPresent() ? ii[0] : -1;
		}

		@Override
		public boolean isEmpty() {
			return !starts().findFirst().isPresent();
		}

		@Override
		public int size() {
			return (int) starts().count();
		}

		@Override
		public ListIterator<Freeblock> listIterator(int index) {
			return new FreeblockIterator(
					index != 0 ? starts().skip(index - 1).findFirst().getAsInt() : buffer.position() + 1);
		}

		private IntStream starts() {
			return IntStream.iterate(buffer.position() + 1, x -> Short.toUnsignedInt(buffer.getShort(x))).skip(1)
					.takeWhile(x -> x != 0);
		}
	}

	private class FreeblockIterator implements ListIterator<Freeblock> {

		private int pointer;

		private int pointer0;

		public FreeblockIterator(int p) {
			this.pointer = p;
		}

		@Override
		public boolean hasNext() {
			return buffer.getShort(pointer) != 0;
		}

		@Override
		public Freeblock next() {
			if (buffer.getShort(pointer) == 0)
				throw new NoSuchElementException();
			var x = new ExistingFreeblock(pointer);
			pointer0 = pointer;
			pointer = x.start();
			return x;
		}

		@Override
		public boolean hasPrevious() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Freeblock previous() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int nextIndex() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int previousIndex() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void remove() {
			buffer.putShort(pointer0, buffer.getShort(pointer));
			Arrays.fill(buffer.array(), pointer, pointer + 2 * Short.BYTES, (byte) 0);
			pointer = pointer0;
		}

		@Override
		public void set(Freeblock e) {
			if (e.start() != pointer) {
				buffer.putShort(pointer0, (short) e.start());
				buffer.putShort(e.start(), buffer.getShort(pointer));
//				Arrays.fill(buffer.array(), e.start() + 2 * Short.BYTES, pointer + 2 * Short.BYTES, (byte) 0);
				pointer = e.start();
			}
			buffer.putShort(pointer + Short.BYTES, (short) e.size());
			Arrays.fill(buffer.array(), e.start() + 2 * Short.BYTES, e.end(), (byte) 0);
		}

		@Override
		public void add(Freeblock e) {
			insert(e, pointer, Short.toUnsignedInt(buffer.getShort(pointer)));
		}

		private void insert(Freeblock e, int previous, int next) {
			buffer.putShort(previous, (short) e.start());
			buffer.putShort(e.start(), (short) next);
			buffer.putShort(e.start() + Short.BYTES, (short) e.size());
			Arrays.fill(buffer.array(), e.start() + 2 * Short.BYTES, e.start() + e.size(), (byte) 0);
		}
	}

	public class ExistingFreeblock implements Freeblock {

		private final int pointer;

		public ExistingFreeblock(int pointer) {
			this.pointer = pointer;
		}

		@Override
		public int start() {
			return Short.toUnsignedInt(buffer.getShort(pointer));
		}

		@Override
		public int size() {
			return Short.toUnsignedInt(buffer.getShort(start() + Short.BYTES));
		}
	}

	private class CellPointerList extends AbstractList<Integer> implements RandomAccess {

		@Override
		public int size() {
			return getCellCount();
		}

		@Override
		public Integer get(int index) {
			return Short.toUnsignedInt(buffer.getShort(p(index)));
		}

		@Override
		public void add(int index, Integer element) {
			var p = p(index);
			var m = size() - index;
			if (m > 0)
				System.arraycopy(buffer.array(), p, buffer.array(), p + Short.BYTES, m * Short.BYTES);
			buffer.putShort(p, element.shortValue());
		}

		@Override
		public Integer remove(int index) {
			var p = p(index);
			var x = Short.toUnsignedInt(buffer.getShort(p));
			var m = size() - index - 1;
			if (m > 0)
				System.arraycopy(buffer.array(), p + Short.BYTES, buffer.array(), p, m * Short.BYTES);
			return x;
		}

		@Override
		public Integer set(int index, Integer element) {
			var p = p(index);
			var x = Short.toUnsignedInt(buffer.getShort(p));
			buffer.putShort(p, element.shortValue());
			return x;
		}

		public int p(int index) {
			return buffer.position() + (buffer.get(buffer.position()) == 0x05 ? 12 : 8) + index * Short.BYTES;
		}
	}

	private class CellList extends AbstractList<C> implements RandomAccess {

		@Override
		public int size() {
			return getCellCount();
		}

		@Override
		public C get(int index) {
			return cell(index);
		}

		@Override
		public void add(int index, C element) {
			var cp = addCell(element);
			if (cp == -1)
				throw new IllegalStateException();
			cellPointers.add(index, cp);
			setCellCount(getCellCount() + 1);
		}

		@Override
		public C remove(int index) {
			var c = get(index);
			removeCell(c);
			cellPointers.remove(index);
			setCellCount(getCellCount() - 1);
			return c;
		}

		@Override
		public C set(int index, C element) {
			var c = cell(index);
			var d = element.size() - c.size();
			int cp;
			if (d > 0) {
				removeCell(c);
				cp = addCell(element);
				if (cp == -1)
					throw new IllegalStateException();
			} else {
				d = -d;
				cp = c.start() + d;
				var p0 = buffer.position();
				buffer.position(cp);
				element.put(buffer);
				buffer.position(p0);
				if (d == 0)
					;
				else if (d < 4)
					setFragmentedSize(getFragmentedSize() + d);
				else
					freeblocks.insert(new Freeblock.New(c.start(), d));
			}
			cellPointers.set(index, cp);
			return c;
		}

		protected int addCell(C cell) {
			var cs = cell.size();
			var fi = freeblocks.minSizeIndex(cs);
			int cp;
			if (fi != -1) {
//				IO.println("fi=" + fi);
				var f = freeblocks.get(fi);
				var d = f.size() - cs;
				if (d >= 4) {
					cp = f.start() + d;
					freeblocks.set(fi, new Freeblock.New(f.start(), d));
				} else {
					cp = f.start();
					freeblocks.remove(fi);
					if (d != 0)
						setFragmentedSize(getFragmentedSize() + d);
				}
			} else {
				var s = getCellContentAreaStart() - (buffer.position() + 8 + getCellCount() * Short.BYTES);
				if (s < Short.BYTES + cs) {
					s += freeblocks.stream().mapToInt(x -> x.size()).sum();
					if (s < Short.BYTES + cs)
						return -1;
					for (var ff = freeblocks.iterator(); ff.hasNext();) {
						var f = ff.next();
						var fs = f.start();
						var d = f.size();
						ff.remove();
						var as1 = getCellContentAreaStart();
						var as2 = as1 + d;
						System.arraycopy(buffer.array(), as1, buffer.array(), as2, fs - as1);
						for (var i = 0; i < cellPointers.size(); i++) {
							var p = cellPointers.get(i).intValue();
							if (p < fs) {
								p += d;
								cellPointers.set(i, p);
							}
						}
						setCellContentAreaStart(as2);
					}
				}
				cp = getCellContentAreaStart() - cs;
				setCellContentAreaStart(cp);
			}
			var p0 = buffer.position();
			buffer.position(cp);
			cell.put(buffer);
			buffer.position(p0);
			return cp;
		}

		protected void removeCell(C cell) {
			freeblocks.insert(new Freeblock.New(cell.start(), cell.size()));
		}
	}
}

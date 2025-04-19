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
package com.janilla.database;

import java.nio.channels.SeekableByteChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.UnaryOperator;

public class BTreeMemory implements Memory {

//	public static void main(String[] args) throws Exception {
//		var o = 3;
//		var f = Files.createTempFile("memory", "");
//		try (var c = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
//				StandardOpenOption.WRITE)) {
//			var m = new BTreeMemory(o, c, BlockReference.read(c, 0), Math.max(BlockReference.BYTES, c.size()));
//
//			record Allocate(int size) {
//			}
//			record Free(int index) {
//			}
//			Object[] p;
//			{
//				var r = ThreadLocalRandom.current();
//				var a = new ArrayList<Allocate>();
//				p = IntStream.range(0, 10).mapToObj(_ -> {
//					if (a.isEmpty() || r.nextInt(5) < 3) {
//						var s = 1 + r.nextInt(1000);
//						var b = new Allocate(s);
//						a.add(b);
//						return b;
//					} else {
//						var i = r.nextInt(a.size());
//						a.remove(i);
//						return new Free(i);
//					}
//				}).toArray();
//			}
//
////			{
////				var s = "[Allocate[size=58], Allocate[size=46], Free[index=1], Allocate[size=20], Free[index=0], Free[index=0], Allocate[size=26]]";
////				p = Arrays.stream(s.substring(1, s.length() - 1).split(", ")).map(x -> {
////					var i = Integer.parseInt(x.substring(x.indexOf('=') + 1, x.length() - 1));
////					return switch (x.substring(0, x.indexOf('['))) {
////					case "Allocate" -> new Allocate(i);
////					case "Free" -> new Free(i);
////					default -> throw new RuntimeException();
////					};
////				}).toArray();
////			}
//
//			System.out.println(Arrays.toString(p));
//
//			var b = new ArrayList<BlockReference>();
//			for (var q : p) {
//				System.out.println(q);
//				BlockReference r;
//				switch (q) {
//				case Allocate a:
//					r = m.allocate(a.size);
//					b.add(r);
//					break;
//				case Free g:
//					r = b.remove(g.index);
//					m.free(r);
//					break;
//				default:
//					throw new RuntimeException();
//				}
//				System.out.println("\t" + r);
//			}
//		}
//	}

	protected final FreeBTree freeBTree;

	protected long appendPosition;

	public BTreeMemory(int order, SeekableByteChannel channel, BlockReference root, long appendPosition) {
		var m = new Memory() {

			@Override
			public void free(BlockReference reference) {
//				System.out.println("free " + reference + "!");
				freeBTree.queue.offer(reference);
			}

			@Override
			public BlockReference allocate(int size) {
				var c = computeCapacity(size);
				var p = BTreeMemory.this.append(c);
				var r = new BlockReference(-1, p, c);
//				System.out.println("allocate " + r + "!");
				return r;
			}
		};
		freeBTree = new FreeBTree(order, channel, m, root);
		this.appendPosition = appendPosition;
	}

	public BTree<BlockReference> freeBTree() {
		return freeBTree;
	}

//	public long getAppendPosition() {
//		return appendPosition;
//	}
//
//	public void setAppendPosition(long appendPosition) {
//		this.appendPosition = appendPosition;
//	}

	@Override
	public BlockReference allocate(int size) {
		var c = computeCapacity(size);
		var r = freeBTree.remove(new BlockReference(-1, -1, c));
		if (r != null) {
//			System.out.println("allocate " + r + "*");
			return r;
		}
		var p = append(c);
		r = new BlockReference(-1, p, c);
//		System.out.println("allocate " + r);
		return r;
	}

	@Override
	public void free(BlockReference reference) {
		var r = reference;
//		System.out.println("free " + r);
		var t = freeBTree;
		t.getOrAdd(r);
	}

	protected long append(int capacity) {
		var p = appendPosition;
		appendPosition += capacity;
		return p;
	}

	protected int computeCapacity(int size) {
		if (size < 0)
			throw new IllegalArgumentException("size=" + size);
		if (size == 0)
			return 0;
		var c = 1;
		while (c < size)
			c <<= 1;
		return c;
	}

	protected static class FreeBTree extends BTree<BlockReference> {

		protected Queue<BlockReference> queue = new ArrayDeque<>();

		public FreeBTree(int order, SeekableByteChannel channel, Memory memory, BlockReference root) {
			super(order, channel, memory, BlockReference.BYTE_CONVERTER, root);
		}

		@Override
		public void add(BlockReference element, UnaryOperator<BlockReference> operator) {
			super.add(element, operator);
			while (!queue.isEmpty())
				super.add(queue.poll());
		}

		@Override
		public BlockReference remove(BlockReference element) {
			var e = super.remove(element);
			while (!queue.isEmpty())
				super.add(queue.poll());
			return e;
		}
	}
}

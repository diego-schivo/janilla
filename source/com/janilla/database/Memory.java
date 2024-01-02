/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.janilla.io.ElementHelper;
import com.janilla.io.IO;
import com.janilla.util.Lazy;

public class Memory {

	public static void main(String[] args) throws IOException {
		var o = 3;
		var f = Files.createTempFile("memory", null);
		try (var c = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {

			var m = new Memory();
			var u = m.getFreeBTree();
			u.order = o;
			u.channel = c;
			u.root = BTree.readReference(c, 0);
			m.appendPosition = Math.max(8 + 4, c.size());

			record Allocate(int size) {
			}
			record Free(int index) {
			}
			Object[] p;
			{
				var r = ThreadLocalRandom.current();
				var a = new ArrayList<Allocate>();
				p = IntStream.range(0, 10).mapToObj(x -> {
					if (a.isEmpty() || r.nextInt(5) < 3) {
						var s = 1 + r.nextInt(1000);
						var b = new Allocate(s);
						a.add(b);
						return b;
					} else {
						var i = r.nextInt(a.size());
						a.remove(i);
						return new Free(i);
					}
				}).toArray();
			}

//			{
//				var s = "[Allocate[size=58], Allocate[size=46], Free[index=1], Allocate[size=20], Free[index=0], Free[index=0], Allocate[size=26]]";
//				p = Arrays.stream(s.substring(1, s.length() - 1).split(", ")).map(x -> {
//					var i = Integer.parseInt(x.substring(x.indexOf('=') + 1, x.length() - 1));
//					return switch (x.substring(0, x.indexOf('['))) {
//					case "Allocate" -> new Allocate(i);
//					case "Free" -> new Free(i);
//					default -> throw new RuntimeException();
//					};
//				}).toArray();
//			}

			System.out.println(Arrays.toString(p));

			var b = new ArrayList<BlockReference>();
			for (var q : p) {
				System.out.println(q);
				BlockReference r;
				switch (q) {
				case Allocate a:
					r = m.allocate(a.size);
					b.add(r);
					break;
				case Free g:
					r = b.remove(g.index);
					m.free(r);
					break;
				default:
					throw new RuntimeException();
				}
				System.out.println("\t" + r);
			}
		}
	}

	Supplier<BTree<BlockReference>> freeBTree = Lazy.of(() -> {
		var t = new FreeBTree();
		var m = this;
		t.memory = new Memory() {

			@Override
			public void free(BlockReference reference) throws IOException {
//				System.out.println("free " + reference + "!");
				t.queue.offer(reference);
			}

			@Override
			public BlockReference allocate(int size) throws IOException {
				var c = computeCapacity(size);
				var p = m.append(c);
				var r = new BlockReference(-1, p, c);
//				System.out.println("allocate " + r + "!");
				return r;
			}
		};
		return t;
	});

	long appendPosition;

	public BTree<BlockReference> getFreeBTree() {
		return freeBTree.get();
	}

	public long getAppendPosition() {
		return appendPosition;
	}

	public void setAppendPosition(long appendPosition) {
		this.appendPosition = appendPosition;
	}

	public BlockReference allocate(int size) throws IOException {
		var c = computeCapacity(size);
		var r = freeBTree.get().remove(new BlockReference(-1, -1, c));
		if (r != null) {
//			System.out.println("allocate " + r + "*");
			return r;
		}
		var p = append(c);
		r = new BlockReference(-1, p, c);
//		System.out.println("allocate " + r);
		return r;
	}

	public void free(BlockReference reference) throws IOException {
		var r = reference;
//		System.out.println("free " + r);
		var t = freeBTree.get();
		t.getOrAdd(r, null);
	}

	long append(int capacity) throws IOException {
		var p = appendPosition;
		appendPosition += capacity;
		return p;
	}

	static int computeCapacity(int size) {
		if (size < 0)
			throw new IllegalArgumentException("size=" + size);
		if (size == 0)
			return 0;
		var c = 1;
		while (c < size)
			c <<= 1;
		return c;
	}

	public record BlockReference(long self, long position, int capacity) {

		static ElementHelper<BlockReference> HELPER = new ElementHelper<BlockReference>() {

			@Override
			public byte[] getBytes(BlockReference element) {
				var b = ByteBuffer.allocate(8 + 4);
				b.putLong(element.position);
				b.putInt(element.capacity);
				return b.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				return 8 + 4;
			}

			@Override
			public BlockReference getElement(ByteBuffer buffer) {
				var p = buffer.getLong();
				var c = buffer.getInt();
				return new BlockReference(-1, p, c);
			}

			@Override
			public int compare(ByteBuffer buffer, BlockReference element) {
				var p = buffer.position();
				var c = buffer.getInt(p + 8);
				var i = Integer.compare(c, element.capacity);
				if (i == 0 && element.position >= 0) {
					var q = buffer.getLong(p);
					i = Long.compare(q, element.position);
				}
				return i;
			}
		};
	}

	static class FreeBTree extends BTree<BlockReference> {

		Queue<BlockReference> queue = new LinkedList<>();

		{
			helper = BlockReference.HELPER;
		}

		@Override
		public void add(BlockReference element, IO.UnaryOperator<BlockReference> operator) throws IOException {
			super.add(element, operator);
			while (!queue.isEmpty())
				super.add(queue.poll(), null);
		}

		@Override
		public BlockReference remove(BlockReference element) throws IOException {
			var e = super.remove(element);
			while (!queue.isEmpty())
				super.add(queue.poll(), null);
			return e;
		}
	}
}

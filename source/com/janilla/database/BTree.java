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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.io.ElementHelper;
import com.janilla.io.IO;
import com.janilla.io.TransactionalByteChannel;

public class BTree<E> {

	public static void main(String[] args) throws Exception {
		var l = 10;
		var o = 3;
		var r = ThreadLocalRandom.current();
		var a = IntStream.range(0, l).map(_ -> r.nextInt(1, l)).toArray();
		System.out.println(Arrays.toString(a));
		var b = IntStream.range(0, r.nextInt(1, a.length + 1)).map(_ -> r.nextInt(a.length)).distinct().toArray();
		System.out.println(Arrays.toString(b));

		var f = Files.createTempFile("btree", "");
		var tf = Files.createTempFile("btree", ".transaction");
		try (var ch = new TransactionalByteChannel(
				FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE),
				FileChannel.open(tf, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE))) {
			Supplier<BTree<Integer>> bts = () -> {
				try {
					var m = new Memory();
					var ft = m.getFreeBTree();
					ft.setOrder(o);
					ft.setChannel(ch);
					ft.setRoot(BlockReference.read(ch, 0));
					m.setAppendPosition(Math.max(2 * BlockReference.BYTES, ch.size()));

					var bt = new BTree<Integer>();
					bt.setOrder(o);
					bt.setChannel(ch);
					bt.setMemory(m);
					bt.setHelper(ElementHelper.INTEGER);
					bt.setRoot(BlockReference.read(ch, BlockReference.BYTES));
					return bt;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			};

			ch.startTransaction();
			{
				var bt = bts.get();
				for (var i : a)
					bt.add(i);
			}
			ch.commitTransaction();

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			ch.startTransaction();
			int[] e;
			{
				var bt = bts.get();
				for (var i : b) {
					bt.remove(a[i]);
					a[i] = -1;
				}
				e = Arrays.stream(a).filter(x -> x >= 0).toArray();
				System.out.println(Arrays.toString(e));
			}
			ch.commitTransaction();

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var bt = bts.get();
				var s = bt.stream().mapToInt(Integer::intValue).toArray();
				System.out.println(Arrays.toString(s));
				Arrays.sort(e);
				assert Arrays.equals(s, e) : "\n\t" + Arrays.toString(s) + "\n\t" + Arrays.toString(e);
			}
		}
	}

	private int order;

	private SeekableByteChannel channel;

	private Memory memory;

	private ElementHelper<E> helper;

	private BlockReference root;

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public SeekableByteChannel getChannel() {
		return channel;
	}

	public void setChannel(SeekableByteChannel channel) {
		this.channel = channel;
	}

	public Memory getMemory() {
		return memory;
	}

	public void setMemory(Memory memory) {
		this.memory = memory;
	}

	public ElementHelper<E> getHelper() {
		return helper;
	}

	public void setHelper(ElementHelper<E> elementHelper) {
		this.helper = elementHelper;
	}

	public BlockReference getRoot() {
		return root;
	}

	public void setRoot(BlockReference root) {
		this.root = root;
	}

	public void add(E element) {
		add(element, null);
	}

	public void add(E element, UnaryOperator<E> operator) {
//		System.out.println("add element=" + element);

		var r = root;
		var n = new Node();
		var err = new ArrayDeque<ElementReference>();
		for (;;) {
			n.setReference(r);
			n.read();
			var i = n.getIndex(element);
			var er = new ElementReference(r, i < 0 ? -i - 1 : i);
			err.push(er);
			r = n.getChild(er.index);
			if (r == null) {
				add(element, operator, n, err);
				return;
			}
		}
	}

	public E get(E element) {
		return get(element, null);
	}

	public E get(E element, UnaryOperator<E> operator) {
//		System.out.println("get element=" + element);

		var r = root;
		var n = new Node();
		var err = new ArrayDeque<ElementReference>();
		for (;;) {
			n.setReference(r);
			n.read();
			var i = n.getIndex(element);
			var er = new ElementReference(r, i < 0 ? -i - 1 : i);
			err.push(er);
			if (i >= 0)
				return get(operator, n, err);
			r = n.getChild(er.index);
			if (r == null)
				return null;
		}
	}

	public E getOrAdd(E element) {
		return getOrAdd(element, null);
	}

	public E getOrAdd(E element, UnaryOperator<E> operator) {
//		System.out.println("getOrAdd element=" + element);

		var r = root;
		var n = new Node();
		var err = new ArrayDeque<ElementReference>();
		for (;;) {
			n.setReference(r);
			n.read();
			var i = n.getIndex(element);
			var er = new ElementReference(r, i < 0 ? -i - 1 : i);
			err.push(er);
			if (i >= 0)
				return get(operator, n, err);
			r = n.getChild(er.index);
			if (r == null) {
				add(element, operator, n, err);
				return null;
			}
		}
	}

	public E remove(E element) {
//		System.out.println("remove element=" + element);

		var r = root;
		var n = new Node();
		var err = new ArrayDeque<ElementReference>();
		for (;;) {
			n.setReference(r);
			n.read();
			var i = n.getIndex(element);
			var er = new ElementReference(r, i < 0 ? -i - 1 : i);
			err.push(er);
			if (i >= 0)
				return remove(n, err);
			r = n.getChild(er.index);
			if (r == null)
				return null;
		}
	}

	public Stream<E> stream() {
		var r = root;
		var n = new Node();
		class A {

			BlockReference r;

			int i;
		}
		var aa = new ArrayDeque<A>();
		do {
			n.setReference(r);
			n.read();
			var a = new A();
			a.r = r;
			aa.push(a);
			r = n.getChild(0);
		} while (r != null);

		class B {

			boolean t;

			boolean hn() {
				if (t)
					return false;
				t = aa.isEmpty();
				return true;
			}

			E n() {
				var a = aa.peek();
				if (a == null) {
					t = true;
					return null;
				}
				var s = n.getSize();
				if (a.i <= s) {
					if (a.i < s) {
						var e = n.getElement(a.i);
						a.i++;
						var c = n.getChild(a.i);
						if (c != null) {
							a = new A();
							a.r = c;
							aa.push(a);
							n.setReference(c);
							n.read();
							for (;;) {
								c = n.getChild(0);
								if (c == null)
									break;
								a = new A();
								a.r = c;
								aa.push(a);
								n.setReference(c);
								n.read();
							}
						} else
							while (a.i == s) {
								aa.pop();
								a = aa.peek();
								if (a == null)
									break;
								n.setReference(a.r);
								n.read();
								s = n.getSize();
							}
						return e;
					}
					a.i++;
				}
				t = true;
				return null;
			}
		}

		var b = new B();
		return Stream.iterate(b.n(), _ -> b.hn(), _ -> b.n());
	}

	void add(E element, UnaryOperator<E> operator, Node node, Deque<ElementReference> stack) {
//		System.out.println("add element=" + element);
		BlockReference r = null;
		var e = element;
		E e2 = null;
		for (;;) {
			var er = stack.pop();
			node.reset();
			node.setReference(er.node);
			node.read();
			var i = er.index;

			if (node.getSize() < order - 1) {
				node.insertElement(i, e);
				if (r != null)
					node.insertChild(i + 1, r);
				if (e2 == null) {
					e2 = operator != null ? operator.apply(e) : null;
					if (e2 != null)
						node.replaceElement(i, e2);
					else
						e2 = e;
				}
				break;
			} else {
				var o = order / 2;

				var n1 = new Node();
				{
					n1.reset();
					n1.setReference(stack.isEmpty() ? new BlockReference(-1, -1, 0) : node.reference);
					var s = o;
					if (i < o)
						s--;
					if (s > 0)
						n1.appendElements(node, 0, s);
					if (node.buffer.getInt(Integer.BYTES) != 0)
						n1.appendChildren(node, 0, s + 1);
					if (i < o) {
						n1.insertElement(i, e);
						if (r != null)
							n1.insertChild(i + 1, r);
						if (e2 == null) {
							e2 = operator != null ? operator.apply(e) : null;
							if (e2 != null)
								n1.replaceElement(i, e2);
							else
								e2 = e;
						}
					}
					n1.write();
					n1.writeReference();
				}

				var n2 = new Node();
				{
					n2.reset();
					n2.setReference(new BlockReference(-1, -1, 0));
					var s = order - 1 - o;
					if (i > o)
						s--;
					if (s > 0)
						n2.appendElements(node, i <= o ? o : o + 1, s);
					if (node.buffer.getInt(Integer.BYTES) != 0)
						n2.appendChildren(node, i < o ? o : o + 1, order - (i < o ? o : o + 1));
					if (i > o) {
						n2.insertElement(i - o - 1, e);
						if (r != null)
							n2.insertChild(i - o, r);
						if (e2 == null) {
							e2 = operator != null ? operator.apply(e) : null;
							if (e2 != null)
								n2.replaceElement(i - o - 1, e2);
							else
								e2 = e;
						}
					} else if (i == o && r != null)
						n2.insertChild(0, r);
					n2.write();
					n2.writeReference();
				}

				if (i != o)
					e = node.getElement(i < o ? o - 1 : o);
				if (stack.isEmpty()) {
					node.reset();
					node.insertElement(0, e);
					node.insertChild(0, n1.reference);
					node.insertChild(1, n2.reference);
					if (e2 == null) {
						e2 = operator != null ? operator.apply(e) : null;
						if (e2 != null)
							node.replaceElement(0, e2);
						else
							e2 = e;
					}
					break;
				} else
					r = n2.reference;
			}
		}
		node.write();
		node.writeReference();
		if (stack.isEmpty())
			root = node.reference;
	}

	E get(UnaryOperator<E> operator, Node node, Deque<ElementReference> stack) {
		var er = stack.pop();
		node.setReference(er.node);
		node.reset();
		node.read();

		var e = node.getElement(er.index);
		var e2 = operator != null ? operator.apply(e) : null;
		if (e2 != null) {
			node.replaceElement(er.index, e2);
			node.write();
			node.writeReference();
		}
		return e;
	}

	E remove(Node node, Deque<ElementReference> stack) {
		BlockReference r;
		var er = stack.pop();
		node.setReference(er.node);
		node.reset();
		node.read();

		var e = node.deleteElement(er.index);

		r = node.getChild(er.index);
		if (r != null) {
			var n = new Node();
			stack.push(er);
			for (;;) {
				n.setReference(r);
				n.read();
				var i = n.buffer.getInt(Integer.BYTES) - 1;
				if (i < 0)
					break;
				stack.push(new ElementReference(r, i));
				r = n.getChild(i);
			}
			node.insertElement(er.index, n.deleteElement(n.getSize() - 1));
			node.write();
			node.writeReference();

			n.copyTo(node);
		}

		var o = (order - 1) / 2;
		if (node.getSize() >= o || stack.isEmpty()) {
			node.write();
			node.writeReference();
			return e;
		}

		var pn = new Node();
		var sn1 = new Node();
		var sn2 = new Node();
		for (;;) {
			er = stack.pop();

			pn.setReference(er.node);
			pn.reset();
			pn.read();

			sn1.setReference(pn.getChild(er.index + 1));
			if (sn1.reference != null) {
				sn1.reset();
				sn1.read();
				if (sn1.getSize() > o) {
					node.insertElement(o - 1, pn.deleteElement(er.index));
					pn.insertElement(er.index, sn1.deleteElement(0));
					if (sn1.buffer.getInt(Integer.BYTES) > 0)
						node.insertChild(o, sn1.deleteChild(0));
					node.write();
					node.writeReference();
					pn.write();
					pn.writeReference();
					sn1.write();
					sn1.writeReference();
					return e;
				}
			}

			sn2.setReference(pn.getChild(er.index - 1));
			if (sn2.reference != null) {
				sn2.reset();
				sn2.read();
				if (sn2.getSize() > o) {
					node.insertElement(0, pn.deleteElement(er.index - 1));
					pn.insertElement(er.index - 1, sn2.deleteElement(sn2.getSize() - 1));
					var i = sn2.buffer.getInt(Integer.BYTES) - 1;
					if (i >= 0)
						node.insertChild(0, sn2.deleteChild(i));
					node.write();
					node.writeReference();
					pn.write();
					pn.writeReference();
					sn2.write();
					sn2.writeReference();
					return e;
				}
			}

			Node n;
			if (sn1.reference != null) {
				node.insertElement(o - 1, pn.deleteElement(er.index));
				node.appendElements(sn1, 0, o);
				if (sn1.buffer.getInt(Integer.BYTES) > 0)
					node.appendChildren(sn1, 0, o + 1);
				if (node.referenceChanged)
					pn.replaceChild(er.index, node.reference);
				pn.deleteChild(er.index + 1);
				memory.free(sn1.reference);
				n = node;
			} else {
				sn2.insertElement(o, pn.deleteElement(er.index - 1));
				sn2.appendElements(node, 0, o - 1);
				if (node.buffer.getInt(Integer.BYTES) > 0)
					sn2.appendChildren(node, 0, o);
				if (sn2.referenceChanged)
					pn.replaceChild(er.index - 1, sn2.reference);
				pn.deleteChild(er.index);
				memory.free(node.reference);
				n = sn2;
			}

			var s = pn.getSize();
			if (stack.isEmpty() && s == 0) {
				n.reference = new BlockReference(root.self(), n.reference.position(), n.reference.capacity());
				memory.free(root);
				root = n.reference;
			}

			n.write();
			n.writeReference();

			if (stack.isEmpty() || s >= o) {
				pn.write();
				pn.writeReference();
				return e;
			}

			pn.copyTo(node);
		}
	}

	record ElementReference(BlockReference node, int index) {
	}

	class Node {

		ByteBuffer buffer;

		boolean changed;

		BlockReference reference;

		boolean referenceChanged;

		void setReference(BlockReference reference) {
			this.reference = reference;
			var c = reference != null ? reference.capacity() : 0;
			if (c > (buffer != null ? buffer.capacity() : 0))
				resize(c);
			referenceChanged = false;
		}

		int getSize() {
			if (buffer == null)
				return 0;
			var p = 2 * Integer.BYTES + buffer.getInt(Integer.BYTES) * BlockReference.BYTES;
			buffer.position(p);
			var s = 0;
			while (buffer.hasRemaining()) {
				var l = helper.getLength(buffer);
				p += l;
				if (l < 0 || p > buffer.limit()) {
					System.out.println("l=" + l);
					throw new RuntimeException();
				}
				buffer.position(p);
				s++;
			}
			return s;
		}

		void appendChildren(Node source, int start, int length) {
			if (length == 0)
				return;
			var l = (buffer != null ? buffer.limit() : 2 * Integer.BYTES) + length * BlockReference.BYTES;
			resize(l);
			var p = 2 * Integer.BYTES + buffer.getInt(Integer.BYTES) * BlockReference.BYTES;
			if (p < buffer.limit())
				System.arraycopy(buffer.array(), p, buffer.array(), p + length * BlockReference.BYTES,
						buffer.limit() - p);
			System.arraycopy(source.buffer.array(), 2 * Integer.BYTES + start * BlockReference.BYTES, buffer.array(), p,
					length * BlockReference.BYTES);
			buffer.limit(l);
			buffer.putInt(0, buffer.limit());
			buffer.putInt(Integer.BYTES, buffer.getInt(Integer.BYTES) + length);
			changed = true;
		}

		void appendElements(Node source, int start, int length) {
			if (length == 0)
				return;
			source.buffer.position(2 * Integer.BYTES + source.buffer.getInt(Integer.BYTES) * BlockReference.BYTES);
			var i = 0;
			var p0 = source.buffer.position();
			while (source.buffer.hasRemaining()) {
				if (i == start)
					p0 = source.buffer.position();
				else if (i == start + length)
					break;
				source.buffer.position(source.buffer.position() + helper.getLength(source.buffer));
				i++;
			}
			var p = source.buffer.position();
			var l = (buffer != null ? buffer.limit() : 2 * Integer.BYTES) + p - p0;
			resize(l);

			System.arraycopy(source.buffer.array(), p0, buffer.array(), buffer.limit(), p - p0);
			buffer.limit(l);
			buffer.putInt(0, buffer.limit());
			changed = true;
		}

		BlockReference deleteChild(int index) {
			var p = 2 * Integer.BYTES + index * BlockReference.BYTES;
			var r = new BlockReference(-1, buffer.getLong(p), buffer.getInt(p + Long.BYTES));
			var q = p + BlockReference.BYTES;
			if (q < buffer.limit())
				System.arraycopy(buffer.array(), q, buffer.array(), p, buffer.limit() - q);
			buffer.limit(buffer.limit() - BlockReference.BYTES);
			buffer.putInt(0, buffer.limit());
			buffer.putInt(Integer.BYTES, buffer.getInt(Integer.BYTES) - 1);
			changed = true;
			return r;
		}

		E deleteElement(int index) {
			buffer.position(2 * Integer.BYTES + buffer.getInt(Integer.BYTES) * BlockReference.BYTES);
			for (var i = 0; i < index; i++)
				buffer.position(buffer.position() + helper.getLength(buffer));
			var p = buffer.position();
			var e = helper.getElement(buffer);
			var q = buffer.position();
			if (q < buffer.limit())
				System.arraycopy(buffer.array(), q, buffer.array(), p, buffer.limit() - q);
			buffer.limit(buffer.limit() - (q - p));
			buffer.putInt(0, buffer.limit());
			changed = true;
			return e;
		}

		BlockReference getChild(int index) {
			if (index < 0 || index >= (buffer != null ? buffer.getInt(Integer.BYTES) : 0))
				return null;
			var p = 2 * Integer.BYTES + index * BlockReference.BYTES;
			var r = new BlockReference(reference.position() + p, buffer.getLong(p), buffer.getInt(p + Long.BYTES));
			return r;
		}

		E getElement(int index) {
			if (buffer == null)
				return null;
			buffer.position(2 * Integer.BYTES + buffer.getInt(Integer.BYTES) * BlockReference.BYTES);
			var i = 0;
			while (buffer.hasRemaining()) {
				if (i == index)
					return helper.getElement(buffer);
				buffer.position(buffer.position() + helper.getLength(buffer));
				i++;
			}
			return null;
		}

		int getIndex(E element) {
			if (buffer == null)
				return -1;
			buffer.position(2 * Integer.BYTES + buffer.getInt(Integer.BYTES) * BlockReference.BYTES);
			var i = 0;
			while (buffer.hasRemaining()) {
				var c = helper.compare(buffer, element);
				if (c == 0)
					return i;
				if (c > 0)
					break;
				buffer.position(buffer.position() + helper.getLength(buffer));
				i++;
			}
			return -i - 1;
		}

		void insertChild(int index, BlockReference child) {
			var l = buffer.limit() + BlockReference.BYTES;
			resize(l);
			var p = 2 * Integer.BYTES + index * BlockReference.BYTES;
			if (p < buffer.limit())
				System.arraycopy(buffer.array(), p, buffer.array(), p + BlockReference.BYTES,
						buffer.limit() - p);
			buffer.limit(l);
			buffer.putLong(p, child.position());
			buffer.putInt(p + Long.BYTES, child.capacity());
			buffer.putInt(0, buffer.limit());
			buffer.putInt(Integer.BYTES, buffer.getInt(Integer.BYTES) + 1);
			changed = true;
		}

		void insertElement(int index, E element) {
			int p;
			if (buffer == null && index == 0)
				p = 2 * Integer.BYTES;
			else {
				buffer.position(2 * Integer.BYTES + buffer.getInt(Integer.BYTES) * BlockReference.BYTES);
				for (var i = 0; i < index; i++)
					buffer.position(buffer.position() + helper.getLength(buffer));
				p = buffer.position();
			}

			var b = helper.getBytes(element);
			var l = (buffer != null ? buffer.limit() : 2 * Integer.BYTES) + b.length;
			resize(l);

			if (p < buffer.limit())
				System.arraycopy(buffer.array(), p, buffer.array(), p + b.length, buffer.limit() - p);
			buffer.limit(buffer.limit() + b.length);
			buffer.put(p, b);
			buffer.putInt(0, buffer.limit());
			changed = true;
		}

		void read() {
			try {
				var c = reference.capacity();
				if (c == 0)
					return;
				if (c > (buffer != null ? buffer.capacity() : 0))
					resize(c);
				channel.position(reference.position());
				buffer.position(0);
				buffer.limit(reference.capacity());
				IO.repeat(_ -> channel.read(buffer), buffer.remaining());
				buffer.limit(buffer.getInt(0));
				changed = false;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		void replaceChild(int index, BlockReference child) {
			var p = 2 * Integer.BYTES + index * BlockReference.BYTES;
			buffer.putLong(p, child.position());
			buffer.putInt(p + Long.BYTES, child.capacity());
			changed = true;
		}

		void replaceElement(int index, E element) {
			buffer.position(2 * Integer.BYTES + buffer.getInt(Integer.BYTES) * BlockReference.BYTES);
			for (var i = 0; i < index; i++)
				buffer.position(buffer.position() + helper.getLength(buffer));
			var p = buffer.position();

			var q = p + helper.getLength(buffer);
			var b = helper.getBytes(element);
			var d = p + b.length - q;
			resize(buffer.limit() + d);

			if (d != 0)
				System.arraycopy(buffer.array(), q, buffer.array(), p + b.length, buffer.limit() - q);
			buffer.limit(buffer.limit() + d);
			buffer.put(p, b);
			buffer.putInt(0, buffer.limit());
			changed = true;
		}

		void reset() {
			if (buffer == null)
				return;
			Arrays.fill(buffer.array(), (byte) 0);
			buffer.limit(2 * Integer.BYTES);
			buffer.putInt(0, buffer.limit());
			changed = false;
		}

		void resize(int length) {
			var r = reference;
			if (length > r.capacity()) {
				if (r.capacity() > 0)
					memory.free(r);
				r = memory.allocate(length);
				r = new BlockReference(reference.self(), r.position(), r.capacity());
				reference = r;
				referenceChanged = true;
			}

			var b = buffer;
			if (r.capacity() > (b != null ? b.capacity() : 0)) {
				b = ByteBuffer.allocate(r.capacity());
				if (buffer == null) {
					b.putInt(2 * Integer.BYTES);
					b.putInt(0);
					b.limit(2 * Integer.BYTES);
				} else {
					buffer.position(0);
					b.put(buffer);
					b.limit(buffer.limit());
				}
				buffer = b;
			}
		}

		void write() {
			try {
				if (!changed)
					return;
				int l;
				channel.position(reference.position());
				l = buffer.limit();
				buffer.position(0);
				buffer.limit(reference.capacity());
				IO.repeat(_ -> channel.write(buffer), buffer.remaining());
				buffer.limit(l);
				changed = false;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		void writeReference() {
			try {
				if (!referenceChanged)
					return;
				if (reference.self() >= 0)
					BlockReference.write(reference, channel);
				referenceChanged = false;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		void copyTo(Node destination) {
			destination.setReference(reference);
			System.arraycopy(buffer.array(), 0, destination.buffer.array(), 0, buffer.limit());
			destination.buffer.limit(buffer.limit());
			destination.changed = changed;
			destination.referenceChanged = referenceChanged;
		}
	}
}

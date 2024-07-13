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
		var o = 3;
		var r = ThreadLocalRandom.current();
		var a = IntStream.range(0, 10).map(x -> r.nextInt(1, 1000)).toArray();
		System.out.println(Arrays.toString(a));
		var b = IntStream.range(0, r.nextInt(1, a.length + 1)).map(x -> r.nextInt(a.length)).distinct().toArray();
		System.out.println(Arrays.toString(b));

		var f = Files.createTempFile("btree", "");
		var g = Files.createTempFile("btree", ".transaction");
		try (var c = new TransactionalByteChannel(
				FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE),
				FileChannel.open(g, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE))) {
			Supplier<BTree<Integer>> ts = () -> {
				try {
					var m = new Memory();
					var u = m.getFreeBTree();
					u.setOrder(o);
					u.setChannel(c);
					u.setRoot(Memory.BlockReference.read(c, 0));
					m.setAppendPosition(Math.max(2 * Memory.BlockReference.HELPER_LENGTH, c.size()));

					var t = new BTree<Integer>();
					t.setOrder(o);
					t.setChannel(c);
					t.setMemory(m);
					t.setHelper(ElementHelper.INTEGER);
					t.setRoot(Memory.BlockReference.read(c, Memory.BlockReference.HELPER_LENGTH));
					return t;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			};

			c.startTransaction();
			{
				var t = ts.get();
				for (var i : a)
					t.add(i);
			}
			c.commitTransaction();

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			c.startTransaction();
			int[] e;
			{
				var t = ts.get();
				for (var i : b) {
					t.remove(a[i]);
					a[i] = -1;
				}
				e = Arrays.stream(a).filter(x -> x >= 0).toArray();
				System.out.println(Arrays.toString(e));
			}
			c.commitTransaction();

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var t = ts.get();
				var s = t.stream().mapToInt(Integer::intValue).toArray();
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

	private Memory.BlockReference root;

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

	public Memory.BlockReference getRoot() {
		return root;
	}

	public void setRoot(Memory.BlockReference root) {
		this.root = root;
	}

	public void add(E element) {
		add(element, null);
	}

	public void add(E element, UnaryOperator<E> operator) {
//		System.out.println("add element=" + element);

		Memory.BlockReference r = root;
		var n = new Node();
		var l = new ArrayDeque<ElementReference>();
		for (;;) {
			n.setReference(r);
			n.read();
			var i = n.getIndex(element);
			var a = new ElementReference(r, i < 0 ? -i - 1 : i);
			l.push(a);
			r = n.getChild(a.index);
			if (r == null) {
				add(element, operator, n, l);
				return;
			}
		}
	}

	public E get(E element) {
		return get(element, null);
	}

	public E get(E element, UnaryOperator<E> operator) {
//		System.out.println("get element=" + element);

		Memory.BlockReference r = root;
		var n = new Node();
		var l = new ArrayDeque<ElementReference>();
		for (;;) {
			n.setReference(r);
			n.read();
			var i = n.getIndex(element);
			var a = new ElementReference(r, i < 0 ? -i - 1 : i);
			l.push(a);
			if (i >= 0)
				return get(operator, n, l);
			r = n.getChild(a.index);
			if (r == null)
				return null;
		}
	}

	public E getOrAdd(E element) {
		return getOrAdd(element, null);
	}

	public E getOrAdd(E element, UnaryOperator<E> operator) {
//		System.out.println("getOrAdd element=" + element);

		Memory.BlockReference r = root;
		var n = new Node();
		var l = new ArrayDeque<ElementReference>();
		for (;;) {
			n.setReference(r);
			n.read();
			var i = n.getIndex(element);
			var a = new ElementReference(r, i < 0 ? -i - 1 : i);
			l.push(a);
			if (i >= 0)
				return get(operator, n, l);
			r = n.getChild(a.index);
			if (r == null) {
				add(element, operator, n, l);
				return null;
			}
		}
	}

	public E remove(E element) {
//		System.out.println("remove element=" + element);

		Memory.BlockReference r = root;
		var n = new Node();
		var l = new ArrayDeque<ElementReference>();
		for (;;) {
			n.setReference(r);
			n.read();
			var i = n.getIndex(element);
			var a = new ElementReference(r, i < 0 ? -i - 1 : i);
			l.push(a);
			if (i >= 0)
				return remove(n, l);
			r = n.getChild(a.index);
			if (r == null)
				return null;
		}
	}

	public Stream<E> stream() {
		var r = root;
		var n = new Node();
		class A {

			Memory.BlockReference r;

			int i;
		}
		var l = new ArrayDeque<A>();
		do {
			n.setReference(r);
			n.read();
			var a = new A();
			a.r = r;
			l.push(a);
			r = n.getChild(0);
		} while (r != null);

		class B {

			boolean e;

			boolean h() {
				if (e)
					return false;
				e = l.isEmpty();
				return true;
			}

			E n() {
				var a = l.peek();
				if (a == null) {
					e = true;
					return null;
				}
				var s = n.getSize();
				if (a.i <= s) {
					if (a.i < s) {
						var k = n.getElement(a.i);
						a.i++;
						var c = n.getChild(a.i);
						if (c != null) {
							a = new A();
							a.r = c;
							l.push(a);
							n.setReference(c);
							n.read();
							for (;;) {
								c = n.getChild(0);
								if (c == null)
									break;
								a = new A();
								a.r = c;
								l.push(a);
								n.setReference(c);
								n.read();
							}
						} else
							while (a.i == s) {
								l.pop();
								a = l.peek();
								if (a == null)
									break;
								n.setReference(a.r);
								n.read();
								s = n.getSize();
							}
						return k;
					}
					a.i++;
				}
				e = true;
				return null;
			}
		}

		var b = new B();
		return Stream.iterate(b.n(), x -> b.h(), x -> b.n());
	}

	void add(E element, UnaryOperator<E> operator, Node node, Deque<ElementReference> stack) {
//		System.out.println("add element=" + element);
		Memory.BlockReference r;
		var e = element;
		r = null;
		E f = null;
		for (;;) {
			var a = stack.pop();
			node.reset();
			node.setReference(a.node);
			node.read();
			var i = a.index;

			if (node.getSize() < order - 1) {
				node.insertElement(i, e);
				if (r != null)
					node.insertChild(i + 1, r);
				if (f == null) {
					f = operator != null ? operator.apply(e) : null;
					if (f != null)
						node.replaceElement(i, f);
					else
						f = e;
				}
				break;
			} else {
				var o = order / 2;

				var u = new Node();
				{
					u.reset();
					u.setReference(stack.isEmpty() ? new Memory.BlockReference(-1, -1, 0) : node.reference);
					var s = o;
					if (i < o)
						s--;
					if (s > 0)
						u.appendElements(node, 0, s);
					if (node.buffer.getInt(4) != 0)
						u.appendChildren(node, 0, s + 1);
					if (i < o) {
						u.insertElement(i, e);
						if (r != null)
							u.insertChild(i + 1, r);
						if (f == null) {
							f = operator != null ? operator.apply(e) : null;
							if (f != null)
								u.replaceElement(i, f);
							else
								f = e;
						}
					}
					u.write();
					u.writeReference();
				}

				var v = new Node();
				{
					v.reset();
					v.setReference(new Memory.BlockReference(-1, -1, 0));
					var s = order - 1 - o;
					if (i > o)
						s--;
					if (s > 0)
						v.appendElements(node, i <= o ? o : o + 1, s);
					if (node.buffer.getInt(4) != 0)
						v.appendChildren(node, i < o ? o : o + 1, order - (i < o ? o : o + 1));
					if (i > o) {
						v.insertElement(i - o - 1, e);
						if (r != null)
							v.insertChild(i - o, r);
						if (f == null) {
							f = operator != null ? operator.apply(e) : null;
							if (f != null)
								v.replaceElement(i - o - 1, f);
							else
								f = e;
						}
					} else if (i == o && r != null)
						v.insertChild(0, r);
					v.write();
					v.writeReference();
				}

				if (i != o)
					e = node.getElement(i < o ? o - 1 : o);
				if (stack.isEmpty()) {
					node.reset();
					node.insertElement(0, e);
					node.insertChild(0, u.reference);
					node.insertChild(1, v.reference);
					if (f == null) {
						f = operator != null ? operator.apply(e) : null;
						if (f != null)
							node.replaceElement(0, f);
						else
							f = e;
					}
					break;
				} else
					r = v.reference;
			}
		}
		node.write();
		node.writeReference();
		if (stack.isEmpty())
			root = node.reference;
	}

	E get(UnaryOperator<E> operator, Node node, Deque<ElementReference> stack) {
		var a = stack.pop();
		node.setReference(a.node);
		node.reset();
		node.read();

		var e = node.getElement(a.index);
		var f = operator != null ? operator.apply(e) : null;
		if (f != null) {
			node.replaceElement(a.index, f);
			node.write();
			node.writeReference();
		}
		return e;
	}

	E remove(Node node, Deque<ElementReference> stack) {
		Memory.BlockReference r;
		var a = stack.pop();
		node.setReference(a.node);
		node.reset();
		node.read();

		var e = node.deleteElement(a.index);

		r = node.getChild(a.index);
		if (r != null) {
			var d = new Node();
			stack.push(a);
			for (;;) {
				d.setReference(r);
				d.read();
				var i = d.buffer.getInt(4) - 1;
				if (i < 0)
					break;
				stack.push(new ElementReference(r, i));
				r = d.getChild(i);
			}
			node.insertElement(a.index, d.deleteElement(d.getSize() - 1));
			node.write();
			node.writeReference();

			d.copyTo(node);
		}

		var o = (order - 1) / 2;
		if (node.getSize() >= o || stack.isEmpty()) {
			node.write();
			node.writeReference();
			return e;
		}

		var p = new Node();
		var s1 = new Node();
		var s2 = new Node();
		for (;;) {
			a = stack.pop();

			p.setReference(a.node);
			p.reset();
			p.read();

			s1.setReference(p.getChild(a.index + 1));
			if (s1.reference != null) {
				s1.reset();
				s1.read();
				if (s1.getSize() > o) {
					node.insertElement(o - 1, p.deleteElement(a.index));
					p.insertElement(a.index, s1.deleteElement(0));
					if (s1.buffer.getInt(4) > 0)
						node.insertChild(o, s1.deleteChild(0));
					node.write();
					node.writeReference();
					p.write();
					p.writeReference();
					s1.write();
					s1.writeReference();
					return e;
				}
			}

			s2.setReference(p.getChild(a.index - 1));
			if (s2.reference != null) {
				s2.reset();
				s2.read();
				if (s2.getSize() > o) {
					node.insertElement(0, p.deleteElement(a.index - 1));
					p.insertElement(a.index - 1, s2.deleteElement(s2.getSize() - 1));
					var i = s2.buffer.getInt(4) - 1;
					if (i >= 0)
						node.insertChild(0, s2.deleteChild(i));
					node.write();
					node.writeReference();
					p.write();
					p.writeReference();
					s2.write();
					s2.writeReference();
					return e;
				}
			}

			Node m;
			if (s1.reference != null) {
				node.insertElement(o - 1, p.deleteElement(a.index));
				node.appendElements(s1, 0, o);
				if (s1.buffer.getInt(4) > 0)
					node.appendChildren(s1, 0, o + 1);
				if (node.referenceChanged)
					p.replaceChild(a.index, node.reference);
				p.deleteChild(a.index + 1);
				memory.free(s1.reference);
				m = node;
			} else {
				s2.insertElement(o, p.deleteElement(a.index - 1));
				s2.appendElements(node, 0, o - 1);
				if (node.buffer.getInt(4) > 0)
					s2.appendChildren(node, 0, o);
				if (s2.referenceChanged)
					p.replaceChild(a.index - 1, s2.reference);
				p.deleteChild(a.index);
				memory.free(node.reference);
				m = s2;
			}

			var s = p.getSize();
			if (stack.isEmpty() && s == 0) {
				m.reference = new Memory.BlockReference(root.self(), m.reference.position(), m.reference.capacity());
				memory.free(root);
				root = m.reference;
			}

			m.write();
			m.writeReference();

			if (stack.isEmpty() || s >= o) {
				p.write();
				p.writeReference();
				return e;
			}

			p.copyTo(node);
		}
	}

	record ElementReference(Memory.BlockReference node, int index) {
	}

	class Node {

		ByteBuffer buffer;

		boolean changed;

		Memory.BlockReference reference;

		boolean referenceChanged;

		void setReference(Memory.BlockReference reference) {
			this.reference = reference;
			var c = reference != null ? reference.capacity() : 0;
			if (c > (buffer != null ? buffer.capacity() : 0))
				resize(c);
			referenceChanged = false;
		}

		int getSize() {
			if (buffer == null)
				return 0;
			var p = 2 * 4 + buffer.getInt(4) * Memory.BlockReference.HELPER_LENGTH;
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
			var l = (buffer != null ? buffer.limit() : 2 * 4) + length * Memory.BlockReference.HELPER_LENGTH;
			resize(l);
			var p = 2 * 4 + buffer.getInt(4) * Memory.BlockReference.HELPER_LENGTH;
			if (p < buffer.limit())
				System.arraycopy(buffer.array(), p, buffer.array(), p + length * Memory.BlockReference.HELPER_LENGTH,
						buffer.limit() - p);
			System.arraycopy(source.buffer.array(), 2 * 4 + start * Memory.BlockReference.HELPER_LENGTH, buffer.array(), p,
					length * Memory.BlockReference.HELPER_LENGTH);
			buffer.limit(l);
			buffer.putInt(0, buffer.limit());
			buffer.putInt(4, buffer.getInt(4) + length);
			changed = true;
		}

		void appendElements(Node source, int start, int length) {
			if (length == 0)
				return;
			source.buffer.position(2 * 4 + source.buffer.getInt(4) * Memory.BlockReference.HELPER_LENGTH);
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
			var l = (buffer != null ? buffer.limit() : 2 * 4) + p - p0;
			resize(l);

			System.arraycopy(source.buffer.array(), p0, buffer.array(), buffer.limit(), p - p0);
			buffer.limit(l);
			buffer.putInt(0, buffer.limit());
			changed = true;
		}

		Memory.BlockReference deleteChild(int index) {
			var p = 2 * 4 + index * Memory.BlockReference.HELPER_LENGTH;
			var r = new Memory.BlockReference(-1, buffer.getLong(p), buffer.getInt(p + 8));
			var q = p + Memory.BlockReference.HELPER_LENGTH;
			if (q < buffer.limit())
				System.arraycopy(buffer.array(), q, buffer.array(), p, buffer.limit() - q);
			buffer.limit(buffer.limit() - Memory.BlockReference.HELPER_LENGTH);
			buffer.putInt(0, buffer.limit());
			buffer.putInt(4, buffer.getInt(4) - 1);
			changed = true;
			return r;
		}

		E deleteElement(int index) {
			buffer.position(2 * 4 + buffer.getInt(4) * Memory.BlockReference.HELPER_LENGTH);
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

		Memory.BlockReference getChild(int index) {
			if (index < 0 || index >= (buffer != null ? buffer.getInt(4) : 0))
				return null;
			var p = 2 * 4 + index * Memory.BlockReference.HELPER_LENGTH;
			var r = new Memory.BlockReference(reference.position() + p, buffer.getLong(p), buffer.getInt(p + 8));
			return r;
		}

		E getElement(int index) {
			if (buffer == null)
				return null;
			buffer.position(2 * 4 + buffer.getInt(4) * Memory.BlockReference.HELPER_LENGTH);
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
			buffer.position(2 * 4 + buffer.getInt(4) * Memory.BlockReference.HELPER_LENGTH);
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

		void insertChild(int index, Memory.BlockReference child) {
			var l = buffer.limit() + Memory.BlockReference.HELPER_LENGTH;
			resize(l);
			var p = 2 * 4 + index * Memory.BlockReference.HELPER_LENGTH;
			if (p < buffer.limit())
				System.arraycopy(buffer.array(), p, buffer.array(), p + Memory.BlockReference.HELPER_LENGTH,
						buffer.limit() - p);
			buffer.limit(l);
			buffer.putLong(p, child.position());
			buffer.putInt(p + 8, child.capacity());
			buffer.putInt(0, buffer.limit());
			buffer.putInt(4, buffer.getInt(4) + 1);
			changed = true;
		}

		void insertElement(int index, E element) {
			int p;
			if (buffer == null && index == 0)
				p = 2 * 4;
			else {
				buffer.position(2 * 4 + buffer.getInt(4) * Memory.BlockReference.HELPER_LENGTH);
				for (var i = 0; i < index; i++)
					buffer.position(buffer.position() + helper.getLength(buffer));
				p = buffer.position();
			}

			var b = helper.getBytes(element);
			var l = (buffer != null ? buffer.limit() : 2 * 4) + b.length;
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
				IO.repeat(x -> channel.read(buffer), buffer.remaining());
				buffer.limit(buffer.getInt(0));
				changed = false;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		void replaceChild(int index, Memory.BlockReference child) {
			var p = 2 * 4 + index * Memory.BlockReference.HELPER_LENGTH;
			buffer.putLong(p, child.position());
			buffer.putInt(p + 8, child.capacity());
			changed = true;
		}

		void replaceElement(int index, E element) {
			buffer.position(2 * 4 + buffer.getInt(4) * Memory.BlockReference.HELPER_LENGTH);
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
			buffer.limit(2 * 4);
			buffer.putInt(0, buffer.limit());
			changed = false;
		}

		void resize(int length) {
			var r = reference;
			if (length > r.capacity()) {
				if (r.capacity() > 0)
					memory.free(r);
				r = memory.allocate(length);
				r = new Memory.BlockReference(reference.self(), r.position(), r.capacity());
				reference = r;
				referenceChanged = true;
			}

			var b = buffer;
			if (r.capacity() > (b != null ? b.capacity() : 0)) {
				b = ByteBuffer.allocate(r.capacity());
				if (buffer == null) {
					b.putInt(2 * 4);
					b.putInt(0);
					b.limit(2 * 4);
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
				IO.repeat(x -> channel.write(buffer), buffer.remaining());
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
					Memory.BlockReference.write(reference, channel);
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

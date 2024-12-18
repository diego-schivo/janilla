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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.janilla.io.ElementHelper;
import com.janilla.io.IO;
import com.janilla.json.Json;
import com.janilla.util.Lazy;

public class Store<E> {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("store", "");
		try (var c = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {

			Supplier<Store<String>> g = () -> {
				var s = new Store<String>();
				s.setInitializeBTree(t -> {
					try {
						var m = new Memory();
						var u = m.getFreeBTree();
						u.setOrder(o);
						u.setChannel(c);
						u.setRoot(Memory.BlockReference.read(c, 0));
						m.setAppendPosition(
								Math.max(2 * Memory.BlockReference.HELPER_LENGTH + IdAndSize.HELPER_LENGTH, c.size()));

						t.setOrder(o);
						t.setChannel(c);
						t.setMemory(m);
						t.setRoot(Memory.BlockReference.read(c, Memory.BlockReference.HELPER_LENGTH));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
				s.setElementHelper(ElementHelper.STRING);
				try {
					s.setIdAndSize(IdAndSize.read(c, 2 * Memory.BlockReference.HELPER_LENGTH));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				return s;
			};

			long i;
			{
				var s = g.get();
				i = s.create(j -> Json.format(Map.of("id", j, "title", "Foo")));
				s.update(i, t -> {
					@SuppressWarnings("unchecked")
					var m = (Map<String, Object>) Json.parse(t);
					m.put("title", "FooBarBazQux");
					return Json.format(m);
				});
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var s = g.get();
				var m = Json.parse(s.delete(i));
				System.out.println(m);
				var n = Map.of("id", i, "title", "FooBarBazQux");
				assert m.equals(n) : m;
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var s = g.get();
				var t = s.read(i);
				assert t == null : t;
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();
		}
	}

	private Consumer<BTree<IdAndElement>> initializeBTree;

	private ElementHelper<E> elementHelper;

	private Supplier<BTree<IdAndElement>> btree = Lazy.of(() -> {
		var t = new BTree<IdAndElement>();
		t.setHelper(IdAndElement.HELPER);
		initializeBTree.accept(t);
		return t;
	});

	private IdAndSize idAndSize;

	public void setInitializeBTree(Consumer<BTree<IdAndElement>> initializeBTree) {
		this.initializeBTree = initializeBTree;
	}

	public void setElementHelper(ElementHelper<E> elementHelper) {
		this.elementHelper = elementHelper;
	}

	public BTree<IdAndElement> getBTree() {
		return btree.get();
	}

	public IdAndSize getIdAndSize() {
		return idAndSize;
	}

	public void setIdAndSize(IdAndSize idAndSize) {
		this.idAndSize = idAndSize;
	}

	public long create(LongFunction<E> element) {
		try {
			var t = btree.get();
			var i = idAndSize.id;
			var e = element.apply(i);
			var b = elementHelper.getBytes(e);
			var a = t.getMemory().allocate(b.length);
			t.getChannel().position(a.position());
			var d = ByteBuffer.allocate(a.capacity());
			d.put(0, b);
			IO.repeat(x -> t.getChannel().write(d), d.remaining());
			var r = new Memory.BlockReference(-1, a.position(), a.capacity());
			t.add(new IdAndElement(i, r));
			idAndSize = new IdAndSize(i + 1, idAndSize.size + 1);
			return i;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public E read(long id) {
		var t = btree.get();
		var i = t.get(new IdAndElement(id, new Memory.BlockReference(-1, -1, 0)));
		if (i == null)
			return null;
		return readElement(i.element);
	}

	public E update(long id, UnaryOperator<E> operator) {
		var t = btree.get();
		class A {

			E e;
		}
		var a = new A();
		t.get(new IdAndElement(id, new Memory.BlockReference(-1, -1, 0)), j -> {
			try {
				if (j == null)
					return null;
				var e = readElement(j.element);
				a.e = operator.apply(e);
				var c = elementHelper.getBytes(a.e);
				var r = c.length > j.element.capacity() ? t.getMemory().allocate(c.length) : null;
				var p = r != null ? r.position() : j.element.position();
				t.getChannel().position(p);
				var d = ByteBuffer.allocate(r != null ? r.capacity() : j.element.capacity());
				d.put(0, c);
				IO.repeat(x -> t.getChannel().write(d), d.remaining());
				return r != null ? new IdAndElement(id, new Memory.BlockReference(-1, r.position(), r.capacity())) : null;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		return a.e;
	}

	public E delete(long id) {
		var t = btree.get();
		var i = t.remove(new IdAndElement(id, new Memory.BlockReference(-1, -1, 0)));
		if (i == null)
			return null;
		var e = readElement(i.element);
		idAndSize = new IdAndSize(idAndSize.id, idAndSize.size - 1);
		return e;
	}

	public LongStream ids() {
		return btree.get().stream().mapToLong(IdAndElement::id);
	}

	public Stream<E> elements() {
		return btree.get().stream().map(IdAndElement::element).map(this::readElement);
	}

	public long count() {
		return idAndSize.size;
	}

	protected E readElement(Memory.BlockReference e) {
		try {
			var c = btree.get().getChannel();
			c.position(e.position());
			var b = ByteBuffer.allocate(e.capacity());
			IO.repeat(x -> c.read(b), b.remaining());
			b.position(0);
			return elementHelper.getElement(b);
		} catch (IOException f) {
			throw new UncheckedIOException(f);
		}
	}

	public record IdAndElement(long id, Memory.BlockReference element) {

		static int HELPER_LENGTH = 8 + Memory.BlockReference.HELPER_LENGTH;

		static ElementHelper<IdAndElement> HELPER = new ElementHelper<>() {

			@Override
			public byte[] getBytes(IdAndElement element) {
				var b = ByteBuffer.allocate(HELPER_LENGTH);
				b.putLong(element.id());
				b.putLong(element.element.position());
				b.putInt(element.element.capacity());
				return b.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				return HELPER_LENGTH;
			}

			@Override
			public IdAndElement getElement(ByteBuffer buffer) {
				var i = buffer.getLong();
				var q = buffer.getLong();
				var c = buffer.getInt();
				return new IdAndElement(i, new Memory.BlockReference(-1, q, c));
			}

			@Override
			public int compare(ByteBuffer buffer, IdAndElement element) {
				return Long.compare(buffer.getLong(buffer.position()), element.id());
			}
		};
	}

	public record IdAndSize(long id, long size) {

		static int HELPER_LENGTH = 2 * 8;

		public static IdAndSize read(SeekableByteChannel channel, long position) throws IOException {
			channel.position(position);
			var b = ByteBuffer.allocate(HELPER_LENGTH);
			IO.repeat(x -> channel.read(b), b.remaining());
			return new IdAndSize(b.getLong(0), b.getLong(8));
		}

		public static void write(IdAndSize idAndSize, SeekableByteChannel channel, long position) throws IOException {
			channel.position(position);
			var b = ByteBuffer.allocate(HELPER_LENGTH);
			b.putLong(0, idAndSize.id());
			b.putLong(8, idAndSize.size());
			IO.repeat(x -> channel.write(b), b.remaining());
		}
	}
}

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
import java.util.Map;
import java.util.function.LongFunction;
import java.util.function.UnaryOperator;

import com.janilla.database.Memory.BlockReference;
import com.janilla.io.ElementHelper;
import com.janilla.io.IO;
import com.janilla.json.Json;

public class Store<E> {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("store", null);
		try (var c = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {

			IO.Supplier<Store<String>> g = () -> {
				var s = new Store<String>();
				s.setInitializeBTree(t -> {
					var m = new Memory();
					var u = m.getFreeBTree();
					u.setOrder(o);
					u.setChannel(c);
					u.setRoot(BTree.readReference(c, 0));
					m.setAppendPosition(Math.max(2 * (8 + 4) + 2 * 8, c.size()));

					t.setOrder(o);
					t.setChannel(c);
					t.setMemory(m);
					t.setRoot(BTree.readReference(c, 8 + 4));
				});
				s.setElementHelper(ElementHelper.STRING);
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
				var m = Json.parse(s.read(i));
				System.out.println(m);
				assert m.equals(Map.of("id", i, "title", "FooBarBazQux")) : m;
			}
		}
	}

	IO.Consumer<BTree<IdAndElement>> initializeBTree;

	ElementHelper<E> elementHelper;

	IO.Supplier<BTree<IdAndElement>> btree = IO.Lazy.of(() -> {
		var t = new BTree<IdAndElement>();
		t.helper = IdAndElement.HELPER;
		initializeBTree.accept(t);
		return t;
	});

//	IO.ConstantSupplier<IdAndSize> idAndSize = new IO.ConstantSupplier<>(() -> {
//		var t = btree.get();
//		t.channel.position(t.root.self() + (8 + 4));
//		var b = ByteBuffer.allocate(2 * 8);
//		IO.repeat(x -> t.channel.read(b), b.remaining());
//		return new IdAndSize(b.getLong(0), b.getLong(8));
//	});
	IdAndSize idAndSize;

	public void setInitializeBTree(IO.Consumer<BTree<IdAndElement>> initializeBTree) {
		this.initializeBTree = initializeBTree;
	}

	public void setElementHelper(ElementHelper<E> elementHelper) {
		this.elementHelper = elementHelper;
	}

	public E read(long id) throws IOException {
		var t = btree.get();
		var i = t.get(new IdAndElement(id, new BlockReference(-1, -1, 0)), null);
		if (i == null)
			return null;
		t.channel.position(i.element.position());
		var b = ByteBuffer.allocate(i.element.capacity());
		IO.repeat(x -> t.channel.read(b), b.remaining());
		b.position(0);
		return elementHelper.getElement(b);
	}

	public long create(LongFunction<E> element) throws IOException {
		var t = btree.get();
//		var i = idAndSize.get();
		var i = idAndSize.id;
		var e = element.apply(i);
		var b = elementHelper.getBytes(e);
		var a = t.memory.allocate(b.length);
		t.channel.position(a.position());
		var d = ByteBuffer.allocate(a.capacity());
		d.put(0, b);
		IO.repeat(x -> t.channel.write(d), d.remaining());
		t.add(new IdAndElement(i, new BlockReference(-1, a.position(), a.capacity())), null);
		idAndSize = new IdAndSize(i + 1, idAndSize.size + 1);
//		writeIdAndSize();
		return i;
	}

	public E update(long id, UnaryOperator<E> operator) throws IOException {
		var t = btree.get();
		class A {

			E e;
		}
		var a = new A();
		t.get(new IdAndElement(id, new BlockReference(-1, -1, 0)), j -> {
			if (j == null)
				return null;
			t.channel.position(j.element.position());
			var b = ByteBuffer.allocate(j.element.capacity());
			IO.repeat(x -> t.channel.read(b), b.remaining());
			b.position(0);
			var e = elementHelper.getElement(b);
			a.e = operator.apply(e);
			var c = elementHelper.getBytes(a.e);
			var r = c.length > j.element.capacity() ? t.memory.allocate(c.length) : null;
			var p = r != null ? r.position() : j.element.position();
			t.channel.position(p);
			var d = ByteBuffer.allocate(r != null ? r.capacity() : j.element.capacity());
			d.put(0, c);
			IO.repeat(x -> t.channel.write(d), d.remaining());
			return r != null ? new IdAndElement(id, new BlockReference(-1, r.position(), r.capacity())) : null;
		});
		return a.e;
	}

	public E delete(long id) throws IOException {
		var t = btree.get();
		var i = t.remove(new IdAndElement(id, new BlockReference(-1, -1, 0)));
		if (i == null)
			return null;
		t.channel.position(i.element.position());
		var b = ByteBuffer.allocate(i.element.capacity());
		IO.repeat(x -> t.channel.read(b), b.remaining());
		b.position(0);
		var e = elementHelper.getElement(b);
//		var s = idAndSize.get();
//		idAndSize.set(new IdAndSize(s.id, s.size - 1));
		idAndSize = new IdAndSize(idAndSize.id, idAndSize.size - 1);
//		writeIdAndSize();
		return e;
	}

	public long count() throws IOException {
//		var s = idAndSize.get();
		return idAndSize.size;
	}

//	void writeIdAndSize() throws IOException {
//		var t = btree.get();
//		t.channel.position(t.root.self() + 8 + 4);
//		var b = ByteBuffer.allocate(2 * 8);
//		var i = idAndSize.get();
//		b.putLong(0, i.id);
//		b.putLong(8, i.size);
//		IO.repeat(x -> t.channel.write(b), b.remaining());
//	}

	public record IdAndElement(long id, BlockReference element) {

		static ElementHelper<IdAndElement> HELPER = new ElementHelper<>() {

			@Override
			public byte[] getBytes(IdAndElement element) {
				var b = ByteBuffer.allocate(2 * 8 + 4);
				b.putLong(element.id());
				b.putLong(element.element.position());
				b.putInt(element.element.capacity());
				return b.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				return 2 * 8 + 4;
			}

			@Override
			public IdAndElement getElement(ByteBuffer buffer) {
				var p = buffer.position();
				var i = buffer.getLong(p);
				var q = buffer.getLong(p + 8);
				var c = buffer.getInt(p + 2 * 8);
				return new IdAndElement(i, new BlockReference(-1, q, c));
			}

			@Override
			public int compare(ByteBuffer buffer, IdAndElement element) {
				return Long.compare(buffer.getLong(buffer.position()), element.id());
			}
		};
	}

	public record IdAndSize(long id, long size) {
	}
}

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
		try (var ch = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {
			Supplier<Store<String>> ss = () -> {
				var s = new Store<String>();
				s.setInitializeBTree(x -> {
					try {
						var m = new Memory();
						var ft = m.getFreeBTree();
						ft.setOrder(o);
						ft.setChannel(ch);
						ft.setRoot(BlockReference.read(ch, 0));
						m.setAppendPosition(
								Math.max(2 * BlockReference.HELPER_LENGTH + IdAndSize.HELPER_LENGTH, ch.size()));

						x.setOrder(o);
						x.setChannel(ch);
						x.setMemory(m);
						x.setRoot(BlockReference.read(ch, BlockReference.HELPER_LENGTH));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
				s.setElementHelper(ElementHelper.STRING);
				try {
					s.setIdAndSize(IdAndSize.read(ch, 2 * BlockReference.HELPER_LENGTH));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				return s;
			};

			{
				var s = ss.get();
				var id = s.create(x -> Json.format(Map.of("id", x, "title", "Foo")));
				s.update(id, x -> {
					@SuppressWarnings("unchecked")
					var m = (Map<String, Object>) Json.parse(x);
					m.put("title", "FooBarBazQux");
					return Json.format(m);
				});
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var s = ss.get();
				var m = Json.parse(s.delete(1));
				System.out.println(m);
				var n = Map.of("id", 1L, "title", "FooBarBazQux");
				assert m.equals(n) : m;
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var s = ss.get();
				var x = s.read(1);
				assert x == null : x;
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
			var id = Math.max(idAndSize.id(), 1);
			var e = element.apply(id);
			var b = elementHelper.getBytes(e);
			var a = t.getMemory().allocate(b.length);
			t.getChannel().position(a.position());
			var d = ByteBuffer.allocate(a.capacity());
			d.put(0, b);
			IO.repeat(x -> t.getChannel().write(d), d.remaining());
			var r = new BlockReference(-1, a.position(), a.capacity());
			t.add(new IdAndElement(id, r));
			idAndSize = new IdAndSize(id + 1, idAndSize.size() + 1);
			return id;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public E read(long id) {
		var t = btree.get();
		var i = t.get(new IdAndElement(id, new BlockReference(-1, -1, 0)));
		if (i == null)
			return null;
		return readElement(i.element());
	}

	public E update(long id, UnaryOperator<E> operator) {
		var t = btree.get();
		class A {

			E e;
		}
		var a = new A();
		t.get(new IdAndElement(id, new BlockReference(-1, -1, 0)), j -> {
			try {
				if (j == null)
					return null;
				var e = readElement(j.element());
				a.e = operator.apply(e);
				var c = elementHelper.getBytes(a.e);
				var r = c.length > j.element().capacity() ? t.getMemory().allocate(c.length) : null;
				var p = r != null ? r.position() : j.element().position();
				t.getChannel().position(p);
				var d = ByteBuffer.allocate(r != null ? r.capacity() : j.element().capacity());
				d.put(0, c);
				IO.repeat(x -> t.getChannel().write(d), d.remaining());
				return r != null ? new IdAndElement(id, new BlockReference(-1, r.position(), r.capacity())) : null;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		return a.e;
	}

	public E delete(long id) {
		var t = btree.get();
		var i = t.remove(new IdAndElement(id, new BlockReference(-1, -1, 0)));
		if (i == null)
			return null;
		var e = readElement(i.element());
		idAndSize = new IdAndSize(idAndSize.id(), idAndSize.size() - 1);
		return e;
	}

	public LongStream ids() {
		return btree.get().stream().mapToLong(IdAndElement::id);
	}

	public Stream<E> elements() {
		return btree.get().stream().map(IdAndElement::element).map(this::readElement);
	}

	public long count() {
		return idAndSize.size();
	}

	protected E readElement(BlockReference e) {
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
}

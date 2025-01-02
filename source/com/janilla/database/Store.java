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
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
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
						m.setAppendPosition(Math.max(2 * BlockReference.BYTES, ch.size()));

						x.setOrder(o);
						x.setChannel(ch);
						x.setMemory(m);
						x.setRoot(BlockReference.read(ch, BlockReference.BYTES));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
				s.setElementHelper(ElementHelper.STRING);
				s.setIdSupplier(() -> {
					var v = s.getAttributes().get("nextId");
					var id = v != null ? (Long) v : 1L;
					s.getAttributes().put("nextId", id + 1);
					return id;
				});
				return s;
			};

			class A {

				long id;
			}
			var a = new A();
			{
				var s = ss.get();
				s.setAttributes(new LinkedHashMap<String, Object>());

				a.id = s.create(x -> Json.format(Map.of("id", x, "title", "Foo")));
				s.update(a.id, x -> {
					@SuppressWarnings("unchecked")
					var m = (Map<String, Object>) Json.parse(x);
					m.put("title", "FooBarBazQux");
					return Json.format(m);
				});
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var s = ss.get();
				s.setAttributes(new LinkedHashMap<String, Object>(Map.of("size", 1L)));

				var m = Json.parse(s.delete(a.id));
				System.out.println(m);
				var n = Map.of("id", a.id, "title", "FooBarBazQux");
				assert m.equals(n) : m;
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var s = ss.get();
				s.setAttributes(new LinkedHashMap<String, Object>());

				var x = s.read(a.id);
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

	private Map<String, Object> attributes;

	private LongSupplier idSupplier;

	public void setInitializeBTree(Consumer<BTree<IdAndElement>> initializeBTree) {
		this.initializeBTree = initializeBTree;
	}

	public void setElementHelper(ElementHelper<E> elementHelper) {
		this.elementHelper = elementHelper;
	}

	public BTree<IdAndElement> getBTree() {
		return btree.get();
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public void setIdSupplier(LongSupplier idSupplier) {
		this.idSupplier = idSupplier;
	}

	public long create(LongFunction<E> element) {
		try {
			var bt = btree.get();
			var id = idSupplier.getAsLong();
			var e = element.apply(id);
			var bb = elementHelper.getBytes(e);
			var r = bt.getMemory().allocate(bb.length);
			bt.getChannel().position(r.position());
			var b = ByteBuffer.allocate(r.capacity());
			b.put(0, bb);
			IO.repeat(_ -> bt.getChannel().write(b), b.remaining());
			r = new BlockReference(-1, r.position(), r.capacity());
			bt.add(new IdAndElement(id, r));
			attributes.compute("size", (_, v) -> v == null ? 1L : (Long) v + 1);
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
		class A {

			E e;
		}
		var t = btree.get();
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
				IO.repeat(_ -> t.getChannel().write(d), d.remaining());
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
		attributes.compute("size", (_, v) -> (Long) v - 1);
		return e;
	}

	public LongStream ids() {
		return btree.get().stream().mapToLong(IdAndElement::id);
	}

	public Stream<E> elements() {
		return btree.get().stream().map(IdAndElement::element).map(this::readElement);
	}

	public long count() {
		var x = attributes.get("size");
		return x != null ? (Long) x : 0;
	}

	protected E readElement(BlockReference reference) {
		try {
			var ch = btree.get().getChannel();
			ch.position(reference.position());
			var b = ByteBuffer.allocate(reference.capacity());
			IO.repeat(_ -> ch.read(b), b.remaining());
			b.position(0);
			return elementHelper.getElement(b);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

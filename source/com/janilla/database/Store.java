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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.janilla.io.ByteConverter;
import com.janilla.json.Json;

public class Store<ID extends Comparable<ID>, E> {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("store", "");
		try (var ch = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {
			Supplier<Store<Long, String>> ss = () -> {
				Store<Long, String> s;
				try {
					var m = new BTreeMemory(o, ch, BlockReference.read(ch, 0),
							Math.max(2 * BlockReference.BYTES, ch.size()));
					var t = new BTree<>(o, ch, m, IdAndElement.byteConverter(ByteConverter.LONG),
							BlockReference.read(ch, BlockReference.BYTES));
					s = new Store<>(t, ByteConverter.STRING, x -> {
						var v = x.get("nextId");
						var id = v != null ? (long) v : 1L;
						x.put("nextId", id + 1);
						return id;
					});
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
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

	protected final BTree<IdAndElement<ID>> bTree;

	protected final ByteConverter<E> elementConverter;

	protected final Function<Map<String, Object>, ID> newId;

	protected Map<String, Object> attributes;

	public Store(BTree<IdAndElement<ID>> bTree, ByteConverter<E> elementConverter,
			Function<Map<String, Object>, ID> newId) {
		this.bTree = bTree;
		this.elementConverter = elementConverter;
		this.newId = newId;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public ID create(Function<ID, E> element) {
		try {
			var bt = bTree;
			var id = newId.apply(attributes);
			var e = element.apply(id);
			var bb = elementConverter.serialize(e);
			var r = bt.memory().allocate(bb.length);
			bt.channel().position(r.position());
			var b = ByteBuffer.allocate(r.capacity());
			b.put(0, bb);
			bt.channel().write(b);
			if (b.remaining() != 0)
				throw new IOException();
			r = new BlockReference(-1, r.position(), r.capacity());
			bt.add(new IdAndElement<>(id, r));
			attributes.compute("size", (_, v) -> v == null ? 1L : (Long) v + 1);
			return id;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public E read(ID id) {
		var t = bTree;
		var i = t.get(new IdAndElement<>(id, new BlockReference(-1, -1, 0)));
		if (i == null)
			return null;
		return readElement(i.element());
	}

	public E update(ID id, UnaryOperator<E> operator) {
		class A {

			E e;
		}
		var t = bTree;
		var a = new A();
		t.get(new IdAndElement<>(id, new BlockReference(-1, -1, 0)), j -> {
			try {
				if (j == null)
					return null;
				var e = readElement(j.element());
				a.e = operator.apply(e);
				var c = elementConverter.serialize(a.e);
				var r = c.length > j.element().capacity() ? t.memory().allocate(c.length) : null;
				var p = r != null ? r.position() : j.element().position();
				t.channel().position(p);
				var d = ByteBuffer.allocate(r != null ? r.capacity() : j.element().capacity());
				d.put(0, c);
				t.channel().write(d);
				if (d.remaining() != 0)
					throw new IOException();
				return r != null ? new IdAndElement<>(id, new BlockReference(-1, r.position(), r.capacity())) : null;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		return a.e;
	}

	public E delete(ID id) {
		var t = bTree;
		var i = t.remove(new IdAndElement<>(id, new BlockReference(-1, -1, 0)));
		if (i == null)
			return null;
		var e = readElement(i.element());
		attributes.compute("size", (_, v) -> (Long) v - 1);
		return e;
	}

	public Stream<ID> ids() {
		return bTree.stream().map(IdAndElement::id);
	}

	public Stream<E> elements() {
		return bTree.stream().map(IdAndElement::element).map(this::readElement);
	}

	public long count() {
		var x = attributes.get("size");
		return x != null ? (Long) x : 0;
	}

	protected E readElement(BlockReference reference) {
		try {
			var ch = bTree.channel();
			ch.position(reference.position());
			var b = ByteBuffer.allocate(reference.capacity());
			ch.read(b);
			if (b.remaining() != 0)
				throw new IOException();
			b.position(0);
			return elementConverter.deserialize(b);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

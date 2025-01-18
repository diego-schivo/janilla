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

import com.janilla.io.ByteConverter;
import com.janilla.json.Json;

public class Stores {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("stores", "");
		try (var ch = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {
			Supplier<Stores> sss = () -> {
				Stores ss;
				try {
					var m = new BTreeMemory(o, ch, BlockReference.read(ch, 0),
							Math.max(2 * BlockReference.BYTES, ch.size()));
					ss = new Stores(
							new BTree<>(o, ch, m, NameAndData.BYTE_CONVERTER,
									BlockReference.read(ch, BlockReference.BYTES)),
							x -> new Store<String>(new BTree<>(o, ch, m, IdAndElement.BYTE_CONVERTER, x.bTree()),
									ByteConverter.STRING, y -> {
										var v = y.get("nextId");
										var id = v != null ? (long) v : 1L;
										y.put("nextId", id + 1);
										return id;
									}));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				return ss;
			};

			class A {

				long id;
			}
			var a = new A();
			{
				var ss = sss.get();
				ss.create("Article");
				ss.perform("Article", s -> {
					a.id = s.create(x -> Json.format(Map.of("id", x, "title", "Foo")));
					return s.update(a.id, x -> {
						@SuppressWarnings("unchecked")
						var m = (Map<String, Object>) Json.parse((String) x);
						m.put("title", "FooBarBazQux");
						return Json.format(m);
					});
				});
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var ss = sss.get();
				ss.perform("Article", s -> {
					var m = Json.parse((String) s.read(a.id));
					System.out.println(m);
					assert m.equals(Map.of("id", a.id, "title", "FooBarBazQux")) : m;
					return m;
				});
			}
		}
	}

	protected final BTree<NameAndData> bTree;

	protected final Function<NameAndData, Store<?>> newStore;

	public Stores(BTree<NameAndData> bTree, Function<NameAndData, Store<?>> newStore) {
		this.bTree = bTree;
		this.newStore = newStore;
	}

	public void create(String name) {
		bTree.getOrAdd(new NameAndData(name, new BlockReference(-1, -1, 0), new BlockReference(-1, -1, 0)));
	}

	public <E, R> R perform(String name, Function<Store<E>, R> operation) {
		class A {

			R r;
		}
		var bt = bTree;
		var a = new A();
		bt.get(new NameAndData(name, new BlockReference(-1, -1, 0), new BlockReference(-1, -1, 0)), x -> {
			@SuppressWarnings("unchecked")
			var s = (Store<E>) newStore.apply(x);
			Map<String, Object> aa;
			try {
				if (x.attributes().capacity() == 0)
					aa = new LinkedHashMap<>();
				else {
					bt.channel().position(x.attributes().position());
					var b = ByteBuffer.allocate(x.attributes().capacity());
					bt.channel().read(b);
					if (b.remaining() != 0)
						throw new IOException();
					b.position(0);
					@SuppressWarnings("unchecked")
					var m = (Map<String, Object>) Json.parse(ByteConverter.STRING.deserialize(b));
					aa = m;
				}
				s.setAttributes(new LinkedHashMap<>(aa));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			a.r = operation.apply(s);

			var aar = x.attributes();
			if (!s.getAttributes().equals(aa)) {
				var bb = ByteConverter.STRING.serialize(Json.format(s.getAttributes()));
				if (bb.length > aar.capacity()) {
					if (aar.capacity() > 0)
						bt.memory().free(aar);
					aar = bt.memory().allocate(bb.length);
				}
				var b = ByteBuffer.allocate(aar.capacity());
				b.put(0, bb);
				try {
					bt.channel().position(aar.position());
					bt.channel().write(b);
					if (b.remaining() != 0)
						throw new IOException();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			var btr = s.bTree.root();
			return aar.position() != x.attributes().position() || btr.position() != x.bTree().position()
					? new NameAndData(name, aar, btr)
					: null;
		});
		return a.r;
	}
}

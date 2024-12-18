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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.janilla.io.ElementHelper;
import com.janilla.json.Json;
import com.janilla.util.Lazy;

public class Stores {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("stores", "");
		try (var c = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {

			Supplier<Stores> g = () -> {
				var s = new Stores();
				s.setInitializeBTree(t -> {
					try {
						var m = new Memory();
						var u = m.getFreeBTree();
						u.setOrder(o);
						u.setChannel(c);
						u.setRoot(Memory.BlockReference.read(c, 0));
						m.setAppendPosition(Math.max(2 * Memory.BlockReference.HELPER_LENGTH, c.size()));

						t.setOrder(o);
						t.setChannel(c);
						t.setMemory(m);
						t.setRoot(Memory.BlockReference.read(c, Memory.BlockReference.HELPER_LENGTH));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
				s.setInitializeStore((n, t) -> {
					@SuppressWarnings("unchecked")
					var u = (Store<String>) t;
					u.setElementHelper(ElementHelper.STRING);
				});
				return s;
			};

			var i = new long[1];
			{
				var s = g.get();
				s.perform("articles", t -> {
					i[0] = t.create(j -> Json.format(Map.of("id", j, "title", "Foo")));
					return t.update(i[0], u -> {
						@SuppressWarnings("unchecked")
						var m = (Map<String, Object>) Json.parse((String) u);
						m.put("title", "FooBarBazQux");
						return Json.format(m);
					});
				});
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var s = g.get();
				s.perform("articles", t -> {
					var m = Json.parse((String) t.read(i[0]));
					System.out.println(m);
					assert m.equals(Map.of("id", (int) i[0], "title", "FooBarBazQux")) : m;
					return m;
				});
			}
		}
	}

	private Consumer<BTree<NameAndStore>> initializeBTree;

	private BiConsumer<String, Store<?>> initializeStore;

	private Supplier<BTree<NameAndStore>> btree = Lazy.of(() -> {
		var t = new BTree<NameAndStore>();
		initializeBTree.accept(t);
		t.setHelper(NameAndStore.HELPER);
		return t;
	});

	public void setInitializeBTree(Consumer<BTree<NameAndStore>> initializeBTree) {
		this.initializeBTree = initializeBTree;
	}

	public void setInitializeStore(BiConsumer<String, Store<?>> initializeStore) {
		this.initializeStore = initializeStore;
	}

	public void create(String name) {
		btree.get().getOrAdd(new NameAndStore(name, new Memory.BlockReference(-1, -1, 0), new Store.IdAndSize(0, 0)));
	}

	public <E, R> R perform(String name, Function<Store<E>, R> operation) {
		var t = btree.get();
		var o = new Object[1];
		t.get(new NameAndStore(name, new Memory.BlockReference(-1, -1, 0), new Store.IdAndSize(0, 0)), n -> {
			var s = new Store<E>();
			s.setInitializeBTree(u -> {
				u.setOrder(t.getOrder());
				u.setChannel(t.getChannel());
				u.setMemory(t.getMemory());
				u.setRoot(n.root);
			});
			s.setIdAndSize(n.idAndSize);
			initializeStore.accept(name, s);
			o[0] = operation.apply(s);
			var r = s.getBTree().getRoot();
			var i = s.getIdAndSize();
			return r.position() != n.root.position() || !i.equals(n.idAndSize) ? new NameAndStore(name, r, i) : null;
		});
		@SuppressWarnings("unchecked")
		var r = (R) o[0];
		return r;
	}

	public record NameAndStore(String name, Memory.BlockReference root, Store.IdAndSize idAndSize) {

		static ElementHelper<NameAndStore> HELPER = new ElementHelper<>() {

			@Override
			public byte[] getBytes(NameAndStore element) {
				var b = element.name().getBytes();
				var c = ByteBuffer.allocate(4 + b.length + Memory.BlockReference.HELPER_LENGTH + Store.IdAndSize.HELPER_LENGTH);
				c.putInt(b.length);
				c.put(b);
				c.putLong(element.root.position());
				c.putInt(element.root.capacity());
				c.putLong(element.idAndSize.id());
				c.putLong(element.idAndSize.size());
				return c.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				return 4 + buffer.getInt(buffer.position()) + Memory.BlockReference.HELPER_LENGTH + Store.IdAndSize.HELPER_LENGTH;
			}

			@Override
			public NameAndStore getElement(ByteBuffer buffer) {
				var b = new byte[buffer.getInt()];
				buffer.get(b);
				var p = buffer.getLong();
				var c = buffer.getInt();
				var i = Math.max(buffer.getLong(), 1);
				var s = buffer.getLong();
				return new NameAndStore(new String(b), new Memory.BlockReference(-1, p, c), new Store.IdAndSize(i, s));
			}

			@Override
			public int compare(ByteBuffer buffer, NameAndStore element) {
				var p = buffer.position();
				var b = new byte[buffer.getInt(p)];
				buffer.get(p + 4, b);
				return new String(b).compareTo(element.name());
			}
		};
	}
}

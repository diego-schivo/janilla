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
		try (var ch = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {
			Supplier<Stores> sss = () -> {
				var ss = new Stores();
				ss.setInitializeBTree(x -> {
					try {
						var m = new Memory();
						var ft = m.getFreeBTree();
						ft.setOrder(o);
						ft.setChannel(ch);
						ft.setRoot(BlockReference.read(ch, 0));
						m.setAppendPosition(Math.max(2 * BlockReference.HELPER_LENGTH, ch.size()));

						x.setOrder(o);
						x.setChannel(ch);
						x.setMemory(m);
						x.setRoot(BlockReference.read(ch, BlockReference.HELPER_LENGTH));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
				ss.setInitializeStore((n, s) -> {
					@SuppressWarnings("unchecked")
					var s2 = (Store<String>) s;
					s2.setElementHelper(ElementHelper.STRING);
				});
				return ss;
			};

			{
				var ss = sss.get();
				ss.create("Article");
				ss.perform("Article", s -> {
					var id = s.create(x -> Json.format(Map.of("id", x, "title", "Foo")));
					return s.update(id, x -> {
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
					var m = Json.parse((String) s.read(1));
					System.out.println(m);
					assert m.equals(Map.of("id", 1L, "title", "FooBarBazQux")) : m;
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
		btree.get().getOrAdd(new NameAndStore(name, new BlockReference(-1, -1, 0), new IdAndSize(0, 0)));
	}

	public <E, R> R perform(String name, Function<Store<E>, R> operation) {
		var bt = btree.get();
		class A {

			R r;
		}
		var a = new A();
		bt.get(new NameAndStore(name, new BlockReference(-1, -1, 0), new IdAndSize(0, 0)), n -> {
			var s = new Store<E>();
			s.setInitializeBTree(x -> {
				x.setOrder(bt.getOrder());
				x.setChannel(bt.getChannel());
				x.setMemory(bt.getMemory());
				x.setRoot(n.root());
			});
			s.setIdAndSize(n.idAndSize());
			initializeStore.accept(name, s);
			a.r = operation.apply(s);
			var r = s.getBTree().getRoot();
			var i = s.getIdAndSize();
			return r.position() != n.root().position() || !i.equals(n.idAndSize()) ? new NameAndStore(name, r, i)
					: null;
		});
		return a.r;
	}
}

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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;

import com.janilla.io.ElementHelper;
import com.janilla.io.IO;
import com.janilla.json.Json;

public class Database {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("database", null);
		try (var c = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {

			record IdAndDate(long id, Instant date) {

				static ElementHelper<IdAndDate> HELPER = new ElementHelper<>() {

					@Override
					public byte[] getBytes(IdAndDate element) {
						var b = ByteBuffer.allocate(2 * 8);
						b.putLong(element.id);
						b.putLong(element.date.toEpochMilli());
						return b.array();
					}

					@Override
					public int getLength(ByteBuffer buffer) {
						return 2 * 8;
					}

					@Override
					public IdAndDate getElement(ByteBuffer buffer) {
						var i = buffer.getLong();
						var d = Instant.ofEpochMilli(buffer.getLong());
						return new IdAndDate(i, d);
					}

					@Override
					public int compare(ByteBuffer buffer, IdAndDate element) {
						var m = buffer.getLong(buffer.position() + 8);
						var n = element.date.toEpochMilli();
						return Long.compare(m, n);
					}
				};
			}

			IO.Supplier<Database> g = () -> {
				var d = new Database();
				var m = new Memory();
				var u = m.getFreeBTree();
				u.setOrder(o);
				u.setChannel(c);
				u.setRoot(BTree.readReference(c, 0));
				m.setAppendPosition(Math.max(3 * (8 + 4), c.size()));

				d.setBTreeOrder(o);
				d.setChannel(c);
				d.setMemoryManager(m);
				d.setStoresRoot(8 + 4);
				d.setIndexesRoot(2 * (8 + 4));
				d.setInitializeStore((n, s) -> {
					@SuppressWarnings("unchecked")
					var t = (Store<String>) s;
					t.setElementHelper(ElementHelper.STRING);
				});
				d.setInitializeIndex((n, i) -> {
					@SuppressWarnings("unchecked")
					var k = (Index<String, IdAndDate>) i;
					k.setKeyHelper(ElementHelper.STRING);
					k.setValueHelper(IdAndDate.HELPER);
				});
				return d;
			};

			var i = new long[1];
			{
				var d = g.get();
				d.<String>storeAccept("articles", s -> {
					i[0] = s.create(j -> Json.format(Map.of("id", j, "title", "Foo")));
					s.update(i[0], t -> {
						@SuppressWarnings("unchecked")
						var m = (Map<String, Object>) Json.parse(t);
						m.put("title", "FooBarBazQux");
						return Json.format(m);
					});
				});
				d.indexAccept("Article.tagList", j -> {
					j.add("foo",
							new IdAndDate(0, LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC)));
					j.add("foo",
							new IdAndDate(1, LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC)));
				});
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var d = g.get();
				d.<String>storeAccept("articles", t -> {
					var j = Json.parse(t.read(i[0]));
					System.out.println(j);
					assert j.equals(Map.of("id", i[0], "name", "FooBarBazQux")) : j;
				});
				d.indexAccept("Article.tagList", j -> {
					var k = j.list("foo").mapToLong(x -> ((IdAndDate) x).id).toArray();
					System.out.println(Arrays.toString(k));
					assert Arrays.equals(k, new long[] { 0, 1 }) : j;
				});
			}
		}
	}

	int btreeOrder;

	SeekableByteChannel channel;

	Memory memory;

	long storesRoot;

	long indexesRoot;

	IO.BiConsumer<String, Store<?>> initializeStore;

	IO.BiConsumer<String, Index<?, ?>> initializeIndex;

	IO.Supplier<Stores> stores = IO.Lazy.of(() -> {
		var s = new Stores();
		s.initializeBTree = t -> {
			t.order = btreeOrder;
			t.channel = channel;
			t.memory = memory;
			t.root = BTree.readReference(channel, storesRoot);
		};
		s.initializeStore = initializeStore;
		return s;
	});

	IO.Supplier<Indexes> indexes = IO.Lazy.of(() -> {
		var i = new Indexes();
		i.initializeBTree = t -> {
			t.order = btreeOrder;
			t.channel = channel;
			t.memory = memory;
			t.root = BTree.readReference(channel, indexesRoot);
		};
		i.initializeIndex = initializeIndex;
		return i;
	});

	public int getBTreeOrder() {
		return btreeOrder;
	}

	public void setBTreeOrder(int btreeOrder) {
		this.btreeOrder = btreeOrder;
	}

	public SeekableByteChannel getChannel() {
		return channel;
	}

	public void setChannel(SeekableByteChannel channel) {
		this.channel = channel;
	}

	public Memory getMemoryManager() {
		return memory;
	}

	public void setMemoryManager(Memory memory) {
		this.memory = memory;
	}

	public long getStoresRoot() {
		return storesRoot;
	}

	public void setStoresRoot(long storesRoot) {
		this.storesRoot = storesRoot;
	}

	public long getIndexesRoot() {
		return indexesRoot;
	}

	public void setIndexesRoot(long indexesRoot) {
		this.indexesRoot = indexesRoot;
	}

	public void setInitializeStore(IO.BiConsumer<String, Store<?>> initializeStore) {
		this.initializeStore = initializeStore;
	}

	public void setInitializeIndex(IO.BiConsumer<String, Index<?, ?>> initializeIndex) {
		this.initializeIndex = initializeIndex;
	}

	public synchronized <E> void storeAccept(String name, IO.Consumer<Store<E>> consumer) throws IOException {
		stores.get().accept(name, consumer);
	}

	public synchronized <E, R> R storeApply(String name, IO.Function<Store<E>, R> function) throws IOException {
		return stores.get().apply(name, function);
	}

	public synchronized <K, V> void indexAccept(String name, IO.Consumer<Index<K, V>> consumer) throws IOException {
		indexes.get().accept(name, consumer);
	}

	public synchronized <K, V, R> R indexApply(String name, IO.Function<Index<K, V>, R> function) throws IOException {
		return indexes.get().apply(name, function);
	}
}

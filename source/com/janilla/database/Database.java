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
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;

import com.janilla.database.Memory.BlockReference;
import com.janilla.io.ElementHelper;
import com.janilla.io.ElementHelper.SortOrder;
import com.janilla.io.ElementHelper.TypeAndOrder;
import com.janilla.io.IO;
import com.janilla.io.TransactionalByteChannel;
import com.janilla.json.Json;

public class Database {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f1 = Files.createTempFile("database", "");
		var f2 = Files.createTempFile("database", ".transaction");
		try (var c = new TransactionalByteChannel(
				FileChannel.open(f1, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE),
				FileChannel.open(f2, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE))) {
			IO.Supplier<Database> g = () -> {
				var d = new Database();
				var m = new Memory();
				var u = m.getFreeBTree();
				u.setOrder(o);
				u.setChannel(c);
				u.setRoot(BlockReference.read(c, 0));
				m.setAppendPosition(Math.max(3 * BlockReference.HELPER_LENGTH, c.size()));

				d.setBTreeOrder(o);
				d.setChannel(c);
				d.setMemory(m);
				d.setStoresRoot(BlockReference.HELPER_LENGTH);
				d.setIndexesRoot(2 * BlockReference.HELPER_LENGTH);
				d.setInitializeStore((n, s) -> {
					@SuppressWarnings("unchecked")
					var t = (Store<String>) s;
					t.setElementHelper(ElementHelper.STRING);
				});
				d.setInitializeIndex((n, i) -> {
					@SuppressWarnings("unchecked")
					var k = (Index<String, Object[]>) i;
					k.setKeyHelper(ElementHelper.STRING);
					k.setValueHelper(ElementHelper.of(new TypeAndOrder(Instant.class, SortOrder.DESCENDING),
							new TypeAndOrder(Long.class, SortOrder.DESCENDING)));
				});
				return d;
			};

			var i = new long[1];
			{
				var d = g.get();
				d.performTransaction(() -> {
					d.performOnStore("articles", s -> {
						i[0] = s.create(j -> Json.format(Map.of("id", j, "title", "Foo")));
						s.update(i[0], t -> {
							@SuppressWarnings("unchecked")
							var m = (Map<String, Object>) Json.parse((String) t);
							m.put("title", "FooBarBazQux");
							return Json.format(m);
						});
					});
					d.performOnIndex("Article.tagList", j -> {
						j.add("foo", new Object[] {
								LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC), 0L });
						j.add("foo", new Object[] {
								LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC), 1L });
					});
				});
			}

			new ProcessBuilder("hexdump", "-C", f1.toString()).inheritIO().start().waitFor();

			{
				var d = g.get();
				d.performTransaction(() -> {
					d.performOnStore("articles", t -> {
						var j = Json.parse((String) t.read(i[0]));
						System.out.println(j);
						assert j.equals(Map.of("id", (int) i[0], "title", "FooBarBazQux")) : j;
					});
					d.performOnIndex("Article.tagList", j -> {
						var k = j.list("foo").mapToLong(x -> (Long) ((Object[]) x)[1]).toArray();
						System.out.println(Arrays.toString(k));
						assert Arrays.equals(k, new long[] { 1, 0 }) : j;
					});
				});
			}
		}
	}

	private int btreeOrder;

	private TransactionalByteChannel channel;

	private Memory memory;

	private long storesRoot;

	private long indexesRoot;

	private IO.BiConsumer<String, Store<?>> initializeStore;

	private IO.BiConsumer<String, Index<?, ?>> initializeIndex;

	private IO.Supplier<Stores> stores = IO.Lazy.of(() -> {
		var s = new Stores();
		s.setInitializeBTree(t -> {
			t.setOrder(btreeOrder);
			t.setChannel(channel);
			t.setMemory(memory);
			t.setRoot(BlockReference.read(channel, storesRoot));
		});
		s.setInitializeStore(initializeStore);
		return s;
	});

	private IO.Supplier<Indexes> indexes = IO.Lazy.of(() -> {
		var i = new Indexes();
		i.initializeBTree = t -> {
			t.setOrder(btreeOrder);
			t.setChannel(channel);
			t.setMemory(memory);
			t.setRoot(BlockReference.read(channel, indexesRoot));
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

	public TransactionalByteChannel getChannel() {
		return channel;
	}

	public void setChannel(TransactionalByteChannel channel) {
		this.channel = channel;
	}

	public Memory getMemory() {
		return memory;
	}

	public void setMemory(Memory memory) {
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

	public synchronized void createStore(String name) throws IOException {
		stores.get().create(name);
	}

	public synchronized <E> void performOnStore(String name, IO.Consumer<Store<E>> operation) throws IOException {
		stores.get().perform(name, operation);
	}

	public void createIndex(String name) throws IOException {
		indexes.get().create(name);
	}

	public synchronized <K, V> void performOnIndex(String name, IO.Consumer<Index<K, V>> operation) throws IOException {
		indexes.get().perform(name, operation);
	}

	public synchronized void performTransaction(IO.Runnable operation) throws IOException {
		channel.startTransaction();
		var c = false;
		try {
			operation.run();
			c = true;
		} finally {
			if (c)
				channel.commitTransaction();
			else
				channel.rollbackTransaction();
		}
	}
}

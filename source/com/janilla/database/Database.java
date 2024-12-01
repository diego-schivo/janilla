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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.janilla.io.ElementHelper;
import com.janilla.io.TransactionalByteChannel;
import com.janilla.json.Json;
import com.janilla.util.Lazy;

public class Database {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("database", "");
		var tf = Files.createTempFile("database", ".transaction");
		try (var ch = new TransactionalByteChannel(
				FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE),
				FileChannel.open(tf, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE))) {
			Supplier<Database> ds = () -> {
				try {
					var m = new Memory();
					var ft = m.getFreeBTree();
					ft.setChannel(ch);
					ft.setOrder(o);
					ft.setRoot(BlockReference.read(ch, 0));
					m.setAppendPosition(Math.max(3 * BlockReference.HELPER_LENGTH, ch.size()));

					var d = new Database();
					d.setBTreeOrder(o);
					d.setChannel(ch);
					d.setMemory(m);
					d.setStoresRoot(BlockReference.HELPER_LENGTH);
					d.setIndexesRoot(2 * BlockReference.HELPER_LENGTH);
					d.setInitializeStore((n, s) -> {
						@SuppressWarnings("unchecked")
						var s2 = (Store<String>) s;
						s2.setElementHelper(ElementHelper.STRING);
					});
					d.setInitializeIndex((n, i) -> {
						@SuppressWarnings("unchecked")
						var i2 = (Index<String, Object[]>) i;
						i2.setKeyHelper(ElementHelper.STRING);
						i2.setValueHelper(ElementHelper.of(
								new ElementHelper.TypeAndOrder(Instant.class, ElementHelper.SortOrder.DESCENDING),
								new ElementHelper.TypeAndOrder(Long.class, ElementHelper.SortOrder.DESCENDING)));
					});
					return d;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			};

			{
				var d = ds.get();
				d.perform((ss, ii) -> {
					ss.create("Article");
					ss.perform("Article", s -> {
						var id = s.create(x -> Json.format(Map.of("id", x, "title", "Foo")));
						s.update(id, x -> {
							@SuppressWarnings("unchecked")
							var m = (Map<String, Object>) Json.parse((String) x);
							m.put("title", "FooBarBazQux");
							return Json.format(m);
						});
						return null;
					});

					ii.create("Article.tagList");
					ii.perform("Article.tagList", i -> {
						i.add("foo",
								new Object[] { LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC),
										1L },
								new Object[] { LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC),
										2L });
						return null;
					});
					return null;
				}, true);
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var d = ds.get();
				d.perform((ss, ii) -> {
					ss.perform("Article", s -> {
						var m = Json.parse((String) s.read(1L));
						System.out.println(m);
						assert m.equals(Map.of("id", 1L, "title", "FooBarBazQux")) : m;
						return null;
					});

					ii.perform("Article.tagList", i -> {
						var ll = i.list("foo").mapToLong(x -> (Long) ((Object[]) x)[1]).toArray();
						System.out.println(Arrays.toString(ll));
						assert Arrays.equals(ll, new long[] { 2, 1 }) : ll;
						return null;
					});
					return null;
				}, false);
			}
		}
	}

	private int btreeOrder;

	private TransactionalByteChannel channel;

	private Memory memory;

	private long storesRoot;

	private long indexesRoot;

	private BiConsumer<String, Store<?>> initializeStore;

	private BiConsumer<String, Index<?, ?>> initializeIndex;

	private Supplier<Stores> stores = Lazy.of(() -> {
		var s = new Stores();
		s.setInitializeBTree(x -> {
			try {
				x.setOrder(btreeOrder);
				x.setChannel(channel);
				x.setMemory(memory);
				x.setRoot(BlockReference.read(channel, storesRoot));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		s.setInitializeStore(initializeStore);
		return s;
	});

	private Supplier<Indexes> indexes = Lazy.of(() -> {
		var i = new Indexes();
		i.initializeBTree = x -> {
			try {
				x.setOrder(btreeOrder);
				x.setChannel(channel);
				x.setMemory(memory);
				x.setRoot(BlockReference.read(channel, indexesRoot));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
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

	public void setInitializeStore(BiConsumer<String, Store<?>> initializeStore) {
		this.initializeStore = initializeStore;
	}

	public void setInitializeIndex(BiConsumer<String, Index<?, ?>> initializeIndex) {
		this.initializeIndex = initializeIndex;
	}

	boolean performing;

	public synchronized <T> T perform(BiFunction<Stores, Indexes, T> operation, boolean write) {
		try {
			var p = performing;
			if (!p)
				performing = true;
			var l = !p ? ((FileChannel) channel.getChannel()).lock() : null;
			try {
				var s = stores.get();
				var i = indexes.get();
				T t = null;
				var c = false;
				if (!p && write)
					channel.startTransaction();
				try {
					t = operation.apply(s, i);
					c = true;
				} finally {
					if (!p && write) {
						if (c)
							channel.commitTransaction();
						else
							channel.rollbackTransaction();
					}
				}
				return t;
			} finally {
				if (!p) {
					l.release();
					performing = false;
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

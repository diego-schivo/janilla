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
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.janilla.io.ByteConverter;
import com.janilla.io.TransactionalByteChannel;
import com.janilla.json.Json;

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
					var m = new BTreeMemory(o, ch, BlockReference.read(ch, 0),
							Math.max(3 * BlockReference.BYTES, ch.size()));
					return new Database(o, ch, m, BlockReference.BYTES, 2 * BlockReference.BYTES,
							x -> new Store<>(
									new BTree<>(o, ch, m, IdAndElement.byteConverter(ByteConverter.LONG), x.bTree()),
									ByteConverter.STRING, y -> {
										var v = y.get("nextId");
										var id = v != null ? (long) v : 1L;
										y.put("nextId", id + 1);
										return id;
									}),
							x -> new Index<String, Object[]>(
									new BTree<>(o, ch, m, KeyAndData.getByteConverter(ByteConverter.STRING), x.bTree()),
									ByteConverter.of(
											new ByteConverter.TypeAndOrder(Instant.class,
													ByteConverter.SortOrder.DESCENDING),
											new ByteConverter.TypeAndOrder(Long.class,
													ByteConverter.SortOrder.DESCENDING))));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			};

			{
				var d = ds.get();
				d.perform((ss, ii) -> {
					ss.create("Article");
					ss.perform("Article", s0 -> {
						@SuppressWarnings("unchecked")
						var s = (Store<Long, String>) s0;
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
						i.add("foo", new Object[][] {
								new Object[] { LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC),
										1L },
								new Object[] { LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC),
										2L } });
						return null;
					});
					return null;
				}, true);
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var d = ds.get();
				d.perform((ss, ii) -> {
					ss.perform("Article", s0 -> {
						@SuppressWarnings("unchecked")
						var s = (Store<Long, String>) s0;
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

	protected final int bTreeOrder;

	protected final TransactionalByteChannel channel;

	protected final Memory memory;

	protected final long storesRoot;

	protected final long indexesRoot;

	protected final Function<NameAndData, Store<?, ?>> newStore;

	protected final Function<NameAndData, Index<?, ?>> newIndex;

	protected final Stores stores;

	protected final Indexes indexes;

	protected final Lock performLock = new ReentrantLock();

	protected final AtomicBoolean performing = new AtomicBoolean();

	public Database(int bTreeOrder, TransactionalByteChannel channel, BTreeMemory memory, long storesRoot,
			long indexesRoot, Function<NameAndData, Store<?, ?>> newStore,
			Function<NameAndData, Index<?, ?>> newIndex) {
		this.bTreeOrder = bTreeOrder;
		this.channel = channel;
		this.memory = memory;
		this.storesRoot = storesRoot;
		this.indexesRoot = indexesRoot;
		this.newStore = newStore;
		this.newIndex = newIndex;

		try {
			stores = new Stores(new BTree<>(bTreeOrder, channel, memory, NameAndData.BYTE_CONVERTER,
					BlockReference.read(channel, storesRoot)), newStore);
			indexes = new Indexes(new BTree<>(bTreeOrder, channel, memory, NameAndData.BYTE_CONVERTER,
					BlockReference.read(channel, indexesRoot)), newIndex);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public int bTreeOrder() {
		return bTreeOrder;
	}

	public TransactionalByteChannel channel() {
		return channel;
	}

	public Memory memory() {
		return memory;
	}

	public long storesRoot() {
		return storesRoot;
	}

	public long indexesRoot() {
		return indexesRoot;
	}

	public <T> T perform(BiFunction<Stores, Indexes, T> operation, boolean write) {
		performLock.lock();
		try {
//			if (write)
//				System.out.println("1 " + LocalDateTime.now());
			var p = performing.getAndSet(true);
			var fl = !p ? ((FileChannel) channel.channel()).lock() : null;
			try {
				T t = null;
				var c = false;
				if (!p && write)
					channel.startTransaction();
				try {
					t = operation.apply(stores, indexes);
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
					fl.release();
					performing.set(false);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
//			if (write)
//				System.out.println("2 " + LocalDateTime.now());
			performLock.unlock();
		}
	}
}

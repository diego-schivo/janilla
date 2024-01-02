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
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import com.janilla.database.Memory.BlockReference;
import com.janilla.io.ElementHelper;
import com.janilla.io.IO;

public class Indexes {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("indexes", null);
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

			IO.Supplier<Indexes> g = () -> {
				var i = new Indexes();
				i.setInitializeBTree(t -> {
					var m = new Memory();
					var u = m.getFreeBTree();
					u.setOrder(o);
					u.setChannel(c);
					u.setRoot(BTree.readReference(c, 0));
					m.setAppendPosition(Math.max(2 * (8 + 4), c.size()));

					t.setOrder(o);
					t.setChannel(c);
					t.setMemory(m);
					t.setRoot(BTree.readReference(c, 8 + 4));
				});
				i.setInitializeIndex((n, j) -> {
					@SuppressWarnings("unchecked")
					var k = (Index<String, IdAndDate>) j;
					k.setKeyHelper(ElementHelper.STRING);
					k.setValueHelper(IdAndDate.HELPER);
				});
				return i;
			};

			{
				var i = g.get();
				i.accept("Article.tagList", j -> {
					j.add("foo",
							new IdAndDate(0, LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC)));
					j.add("foo",
							new IdAndDate(1, LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC)));
				});
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var i = g.get();
				i.accept("Article.tagList", j -> {
					var k = j.list("foo").mapToLong(x -> ((IdAndDate) x).id).toArray();
					System.out.println(Arrays.toString(k));
					assert Arrays.equals(k, new long[] { 0, 1 }) : j;
				});
			}
		}
	}

	IO.Consumer<BTree<NameAndIndex>> initializeBTree;

	IO.BiConsumer<String, Index<?, ?>> initializeIndex;

	IO.Supplier<BTree<NameAndIndex>> btree = IO.Lazy.of(() -> {
		var t = new BTree<NameAndIndex>();
		initializeBTree.accept(t);
		t.helper = NameAndIndex.HELPER;
		return t;
	});

	public void setInitializeBTree(IO.Consumer<BTree<NameAndIndex>> initializeBTree) {
		this.initializeBTree = initializeBTree;
	}

	public void setInitializeIndex(IO.BiConsumer<String, Index<?, ?>> initializeIndex) {
		this.initializeIndex = initializeIndex;
	}

	public <K, V> void accept(String name, IO.Consumer<Index<K, V>> consumer) throws IOException {
		var t = btree.get();
		t.getOrAdd(new NameAndIndex(name, new BlockReference(-1, -1, 0)), n -> {
			var i = new Index<K, V>();
			i.initializeBTree = u -> {
				u.order = t.order;
				u.channel = t.channel;
				u.memory = t.memory;
				u.root = n.root;
			};
			initializeIndex.accept(name, i);
			consumer.accept(i);
			var r = i.btree.get().root;
			return r.position() != n.root.position() ? new NameAndIndex(name, r) : null;
		});
	}

	public <K, V, R> R apply(String name, IO.Function<Index<K, V>, R> function) throws IOException {
		var t = btree.get();
		var o = new Object[1];
		t.getOrAdd(new NameAndIndex(name, new BlockReference(-1, -1, 0)), n -> {
			var i = new Index<K, V>();
			i.initializeBTree = u -> {
				u.order = t.order;
				u.channel = t.channel;
				u.memory = t.memory;
				u.root = n.root;
			};
			initializeIndex.accept(name, i);
			o[0] = function.apply(i);
			var r = i.btree.get().root;
			return r.position() != n.root.position() ? new NameAndIndex(name, r) : null;
		});
		@SuppressWarnings("unchecked")
		var r = (R) o[0];
		return r;
	}

	public record NameAndIndex(String name, BlockReference root) {

		static ElementHelper<NameAndIndex> HELPER = new ElementHelper<>() {

			@Override
			public byte[] getBytes(NameAndIndex element) {
				var b = element.name().getBytes();
				var c = ByteBuffer.allocate(4 + b.length + 8 + 4);
				c.putInt(b.length);
				c.put(b);
				c.putLong(element.root.position());
				c.putInt(element.root.capacity());
				return c.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				return 4 + buffer.getInt(buffer.position()) + 8 + 4;
			}

			@Override
			public NameAndIndex getElement(ByteBuffer buffer) {
				var b = new byte[buffer.getInt()];
				buffer.get(b);
				var p = buffer.getLong();
				var c = buffer.getInt();
				return new NameAndIndex(new String(b), new BlockReference(-1, p, c));
			}

			@Override
			public int compare(ByteBuffer buffer, NameAndIndex element) {
				var p = buffer.position();
				var b = new byte[buffer.getInt(p)];
				buffer.get(p + 4, b);
				return new String(b).compareTo(element.name());
			}
		};
	}
}

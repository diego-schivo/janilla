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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.janilla.database.Memory.BlockReference;
import com.janilla.io.ElementHelper;
import com.janilla.io.ElementHelper.SortOrder;
import com.janilla.io.ElementHelper.TypeAndOrder;
import com.janilla.util.Lazy;

public class Indexes {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("indexes", "");
		try (var c = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {
			Supplier<Indexes> g = () -> {
				var i = new Indexes();
				i.setInitializeBTree(t -> {
					try {
						var m = new Memory();
						var u = m.getFreeBTree();
						u.setOrder(o);
						u.setChannel(c);
						u.setRoot(BlockReference.read(c, 0));
						m.setAppendPosition(Math.max(2 * BlockReference.HELPER_LENGTH, c.size()));

						t.setOrder(o);
						t.setChannel(c);
						t.setMemory(m);
						t.setRoot(BlockReference.read(c, BlockReference.HELPER_LENGTH));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
				i.setInitializeIndex((n, j) -> {
					@SuppressWarnings("unchecked")
					var k = (Index<String, Object[]>) j;
					k.setKeyHelper(ElementHelper.STRING);
					k.setValueHelper(ElementHelper.of(new TypeAndOrder(Instant.class, SortOrder.DESCENDING),
							new TypeAndOrder(Long.class, SortOrder.DESCENDING)));
				});
				return i;
			};

			{
				var i = g.get();
				i.perform("Article.tagList", j -> {
					j.add("foo",
							new Object[] { LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC), 0L });
					j.add("foo",
							new Object[] { LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC), 1L });
					return null;
				});
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var i = g.get();
				i.perform("Article.tagList", j -> {
					var k = j.list("foo").mapToLong(x -> (Long) ((Object[]) x)[1]).toArray();
					System.out.println(Arrays.toString(k));
					assert Arrays.equals(k, new long[] { 1, 0 }) : j;
					return k;
				});
			}
		}
	}

	Consumer<BTree<NameAndIndex>> initializeBTree;

	BiConsumer<String, Index<?, ?>> initializeIndex;

	Supplier<BTree<NameAndIndex>> btree = Lazy.of(() -> {
		var t = new BTree<NameAndIndex>();
		initializeBTree.accept(t);
		t.setHelper(NameAndIndex.HELPER);
		return t;
	});

	public void setInitializeBTree(Consumer<BTree<NameAndIndex>> initializeBTree) {
		this.initializeBTree = initializeBTree;
	}

	public void setInitializeIndex(BiConsumer<String, Index<?, ?>> initializeIndex) {
		this.initializeIndex = initializeIndex;
	}

	public void create(String name) {
		btree.get().getOrAdd(new NameAndIndex(name, new BlockReference(-1, -1, 0)));
	}

	public <K, V, R> R perform(String name, Function<Index<K, V>, R> operation) {
		var t = btree.get();
		var o = new Object[1];
		t.get(new NameAndIndex(name, new BlockReference(-1, -1, 0)), n -> {
			var i = new Index<K, V>();
			i.setInitializeBTree(u -> {
				u.setOrder(t.getOrder());
				u.setChannel(t.getChannel());
				u.setMemory(t.getMemory());
				u.setRoot(n.root);
			});
			initializeIndex.accept(name, i);
			o[0] = operation.apply(i);
			var r = i.getBTree().getRoot();
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
				var c = ByteBuffer.allocate(4 + b.length + BlockReference.HELPER_LENGTH);
				c.putInt(b.length);
				c.put(b);
				c.putLong(element.root.position());
				c.putInt(element.root.capacity());
				return c.array();
			}

			@Override
			public int getLength(ByteBuffer buffer) {
				return 4 + buffer.getInt(buffer.position()) + BlockReference.HELPER_LENGTH;
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

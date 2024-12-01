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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.janilla.io.ElementHelper;
import com.janilla.io.IO;
import com.janilla.json.Json;
import com.janilla.util.Lazy;

public class Indexes {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("indexes", "");
		try (var ch = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {
			Supplier<Indexes> iis = () -> {
				var ii = new Indexes();
				ii.setInitializeBTree(x -> {
					try {
						var m = new Memory();
						var ft = m.getFreeBTree();
						ft.setOrder(o);
						ft.setChannel(ch);
						ft.setRoot(BlockReference.read(ch, 0));
						m.setAppendPosition(Math.max(2 * BlockReference.BYTES, ch.size()));

						x.setOrder(o);
						x.setChannel(ch);
						x.setMemory(m);
						x.setRoot(BlockReference.read(ch, BlockReference.BYTES));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
				ii.setInitializeIndex((n, x) -> {
					@SuppressWarnings("unchecked")
					var i = (Index<String, Object[]>) x;
					i.setKeyHelper(ElementHelper.STRING);
					i.setValueHelper(ElementHelper.of(
							new ElementHelper.TypeAndOrder(Instant.class, ElementHelper.SortOrder.DESCENDING),
							new ElementHelper.TypeAndOrder(Long.class, ElementHelper.SortOrder.DESCENDING)));
				});
				return ii;
			};

			{
				var ii = iis.get();
				ii.create("Article.tagList");
				ii.perform("Article.tagList", i -> {
					i.add("foo",
							new Object[] { LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC), 1L },
							new Object[] { LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC), 2L });
					return null;
				});
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var ii = iis.get();
				ii.perform("Article.tagList", i -> {
					var ll = i.list("foo").mapToLong(x -> (Long) ((Object[]) x)[1]).toArray();
					System.out.println(Arrays.toString(ll));
					assert Arrays.equals(ll, new long[] { 2, 1 }) : ll;
					return ll;
				});
			}
		}
	}

	Consumer<BTree<NameAndData>> initializeBTree;

	BiConsumer<String, Index<?, ?>> initializeIndex;

	Supplier<BTree<NameAndData>> btree = Lazy.of(() -> {
		var bt = new BTree<NameAndData>();
		initializeBTree.accept(bt);
		bt.setHelper(NameAndData.HELPER);
		return bt;
	});

	public void setInitializeBTree(Consumer<BTree<NameAndData>> initializeBTree) {
		this.initializeBTree = initializeBTree;
	}

	public void setInitializeIndex(BiConsumer<String, Index<?, ?>> initializeIndex) {
		this.initializeIndex = initializeIndex;
	}

	public void create(String name) {
		btree.get().getOrAdd(new NameAndData(name, new BlockReference(-1, -1, 0), new BlockReference(-1, -1, 0)));
	}

	public <K, V, R> R perform(String name, Function<Index<K, V>, R> operation) {
		class A {

			R r;
		}
		var bt = btree.get();
		var a = new A();
		bt.get(new NameAndData(name, new BlockReference(-1, -1, 0), new BlockReference(-1, -1, 0)), x -> {
			var i = new Index<K, V>();
			i.setInitializeBTree(y -> {
				y.setOrder(bt.getOrder());
				y.setChannel(bt.getChannel());
				y.setMemory(bt.getMemory());
				y.setRoot(x.btree());
			});
			Map<String, Object> aa;
			try {
				if (x.attributes().capacity() == 0)
					aa = new LinkedHashMap<>();
				else {
					bt.getChannel().position(x.attributes().position());
					var b = ByteBuffer.allocate(x.attributes().capacity());
					IO.repeat(y -> bt.getChannel().read(b), b.remaining());
					b.position(0);
					@SuppressWarnings("unchecked")
					var m = (Map<String, Object>) Json.parse(ElementHelper.STRING.getElement(b));
					aa = m;
				}
				i.setAttributes(new LinkedHashMap<>(aa));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			initializeIndex.accept(name, i);

			a.r = operation.apply(i);

			var aar = x.attributes();
			if (!i.getAttributes().equals(aa)) {
				var bb = ElementHelper.STRING.getBytes(Json.format(i.getAttributes()));
				if (bb.length > aar.capacity()) {
					if (aar.capacity() > 0)
						bt.getMemory().free(aar);
					aar = bt.getMemory().allocate(bb.length);
				}
				var b = ByteBuffer.allocate(aar.capacity());
				b.put(0, bb);
				try {
					bt.getChannel().position(aar.position());
					IO.repeat(y -> bt.getChannel().write(b), b.remaining());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			var btr = i.getBTree().getRoot();
			return aar.position() != x.attributes().position() || btr.position() != x.btree().position()
					? new NameAndData(name, aar, btr)
					: null;
		});
		return a.r;
	}
}

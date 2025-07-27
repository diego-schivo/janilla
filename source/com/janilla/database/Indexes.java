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

import java.io.IO;
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
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.janilla.io.ByteConverter;
import com.janilla.json.Json;

public class Indexes {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("indexes", "");
		try (var ch = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {
			Supplier<Indexes> iis = () -> {
				Indexes ii;
				try {
					var m = new BTreeMemory(o, ch, BlockReference.read(ch),
							Math.max(2 * BlockReference.BYTES, ch.size()));
					var t = new BTree<>(o, ch, m, KeyAndData.getByteConverter(ByteConverter.STRING),
							BlockReference.read(ch));
					ii = new Indexes(t, x -> new Index<String, Object[]>(
							new BTree<>(o, ch, m, KeyAndData.getByteConverter(ByteConverter.STRING), x.bTree()),
							ByteConverter.of(
									new ByteConverter.TypeAndOrder(Instant.class, ByteConverter.SortOrder.DESCENDING),
									new ByteConverter.TypeAndOrder(Long.class, ByteConverter.SortOrder.DESCENDING))));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				return ii;
			};

			{
				var ii = iis.get();
				ii.create("Article.tagList");
				ii.perform("Article.tagList", i -> {
					i.add("foo", new Object[][] {
							new Object[] { LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC), 1L },
							new Object[] { LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC),
									2L } });
					return null;
				});
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var ii = iis.get();
				ii.perform("Article.tagList", i -> {
					var ll = i.list("foo").mapToLong(x -> (Long) ((Object[]) x)[1]).toArray();
					IO.println(Arrays.toString(ll));
					assert Arrays.equals(ll, new long[] { 2, 1 }) : ll;
					return ll;
				});
			}
		}
	}

	protected final BTree<KeyAndData<String>> bTree;

	protected final Function<KeyAndData<String>, Index<?, ?>> newIndex;

	public Indexes(BTree<KeyAndData<String>> bTree, Function<KeyAndData<String>, Index<?, ?>> newIndex) {
		this.bTree = bTree;
		this.newIndex = newIndex;
	}

	public void create(String name) {
//		IO.println("Indexes.create, name=" + name);
		bTree.getOrAdd(new KeyAndData<>(name, new BlockReference(-1, -1, 0), new BlockReference(-1, -1, 0)));
	}

	public <K, V, R> R perform(String name, Function<Index<K, V>, R> operation) {
		class A {

			R r;
		}
		var bt = bTree;
		var a = new A();
		bt.get(new KeyAndData<>(name, new BlockReference(-1, -1, 0), new BlockReference(-1, -1, 0)), x -> {
			@SuppressWarnings("unchecked")
			var i = (Index<K, V>) newIndex.apply(x);
			String aa1;
			try {
				if (x.attributes().capacity() == 0)
					aa1 = "{}";
				else {
					bt.channel().position(x.attributes().position());
					var b = ByteBuffer.allocate(x.attributes().capacity());
					bt.channel().read(b);
					if (b.remaining() != 0)
						throw new IOException();
					b.position(0);
					aa1 = ByteConverter.STRING.deserialize(b);
				}
				@SuppressWarnings("unchecked")
				var m = (Map<String, Object>) Json.parse(aa1);
				i.setAttributes(m);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
//			IO.println("Indexes.perform, name=" + name + ", i=" + i);

			a.r = operation.apply(i);

			var aa2 = Json.format(i.getAttributes());
			var aar = x.attributes();
			if (!aa2.equals(aa1)) {
				var bb = ByteConverter.STRING.serialize(aa2);
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
			var btr = i.bTree.root();
			return aar.position() != x.attributes().position() || btr.position() != x.bTree().position()
					? new KeyAndData<>(name, aar, btr)
					: null;
		});
		return a.r;
	}
}

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
import java.util.stream.Stream;

import com.janilla.database.Memory.BlockReference;
import com.janilla.io.ElementHelper;
import com.janilla.io.IO;
import com.janilla.io.IO.UnaryOperator;

public class Index<K, V> {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("index", null);
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

			IO.Supplier<Index<String, IdAndDate>> g = () -> {
				var i = new Index<String, IdAndDate>();
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
				i.setKeyHelper(ElementHelper.STRING);
				i.setValueHelper(IdAndDate.HELPER);
				return i;
			};

			{
				var i = g.get();
				i.add("foo", new IdAndDate(0, LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC)));
				i.add("foo", new IdAndDate(1, LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC)));
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var i = g.get();
				var j = i.list("foo").mapToLong(x -> ((IdAndDate) x).id).toArray();
				System.out.println(Arrays.toString(j));
				assert Arrays.equals(j, new long[] { 0, 1 }) : j;
			}
		}
	}

	IO.Consumer<BTree<KeyAndValues<K>>> initializeBTree;

	ElementHelper<K> keyHelper;

	ElementHelper<V> valueHelper;

	IO.Supplier<BTree<KeyAndValues<K>>> btree = IO.Lazy.of(() -> {
		var t = new BTree<KeyAndValues<K>>();
		initializeBTree.accept(t);
		t.helper = KeyAndValues.getHelper(keyHelper);
		return t;
	});

	public ElementHelper<?> getKeyHelper() {
		return keyHelper;
	}

	public void setKeyHelper(ElementHelper<K> keyHelper) {
		this.keyHelper = keyHelper;
	}

	public ElementHelper<?> getValueHelper() {
		return valueHelper;
	}

	public void setValueHelper(ElementHelper<V> valueHelper) {
		this.valueHelper = valueHelper;
	}

	public void setInitializeBTree(IO.Consumer<BTree<KeyAndValues<K>>> initializeBTree) {
		this.initializeBTree = initializeBTree;
	}

	public boolean add(K key, V value) throws IOException {
		return apply(key, x -> {
			var v = x.btree.getOrAdd(value, null);
			return v == null ? new ResultAndSize<>(true, x.size + 1) : null;
		}, true) != null;
	}

	public boolean remove(K key, V value) throws IOException {
		return apply(key, x -> {
			var v = x.btree.remove(value);
			return v != null ? new ResultAndSize<>(true, x.size - 1) : null;
		}, false) != null;
	}

	public Stream<V> list(K key) throws IOException {
		var s = apply(key, x -> new ResultAndSize<>(x.btree.stream(), x.size), false);
		return s != null ? s : Stream.empty();
	}

	public long count(K key) throws IOException {
		var c = apply(key, x -> new ResultAndSize<>(Long.valueOf(x.size), x.size), false);
		return c != null ? c : 0;
	}

	public Stream<K> keys() throws IOException {
		return btree.get().stream().map(KeyAndValues::key);
	}

	public Stream<V> values() throws IOException {
		return btree.get().stream().flatMap(x -> {
			try {
				return apply(x.key, y -> new ResultAndSize<>(y.btree.stream(), y.size), false);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	<R> R apply(K key, IO.Function<BTreeAndSize<V>, ResultAndSize<R>> function, boolean add) throws IOException {
		class A {

			ResultAndSize<R> r;
		}
		var t = btree.get();
		var k = new KeyAndValues<K>(key, new BlockReference(-1, -1, 0), 0);
		var a = new A();
		UnaryOperator<KeyAndValues<K>> o = i -> {
			var u = new BTree<V>();
			u.order = t.order;
			u.channel = t.channel;
			u.memory = t.memory;
			u.helper = valueHelper;
			u.root = i.root;

			a.r = function.apply(new BTreeAndSize<>(u, i.size));
			return u.root.position() != i.root.position() || (a.r != null && a.r.size != i.size)
					? new KeyAndValues<K>(key, u.root, a.r.size)
					: null;
		};
		if (add)
			t.getOrAdd(k, o);
		else
			t.get(k, o);
		if (a.r != null && a.r.size == 0)
			t.remove(k);
		return a.r != null ? a.r.result : null;
	}

	record BTreeAndSize<V>(BTree<V> btree, long size) {
	}

	record ResultAndSize<R>(R result, long size) {
	}

	public record KeyAndValues<K>(K key, BlockReference root, long size) {

		static <K> ElementHelper<KeyAndValues<K>> getHelper(ElementHelper<K> keyHelper) {
			return new ElementHelper<>() {

				@Override
				public byte[] getBytes(KeyAndValues<K> element) {
					var b = keyHelper.getBytes(element.key());
					var c = ByteBuffer.allocate(b.length + 8 + 4 + 8);
					c.put(b);
					var r = element.root();
					c.putLong(r.position());
					c.putInt(r.capacity());
					c.putLong(element.size());
					return c.array();
				}

				@Override
				public int getLength(ByteBuffer buffer) {
					return keyHelper.getLength(buffer) + 8 + 4 + 8;
				}

				@Override
				public KeyAndValues<K> getElement(ByteBuffer buffer) {
					var k = keyHelper.getElement(buffer);
					var p = buffer.getLong();
					var c = buffer.getInt();
					var s = buffer.getLong();
					return new KeyAndValues<>(k, new BlockReference(-1, p, c), s);
				}

				@Override
				public int compare(ByteBuffer buffer, KeyAndValues<K> element) {
					return keyHelper.compare(buffer, element.key());
				}
			};
		}
	}
}

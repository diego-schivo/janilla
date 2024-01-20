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
import com.janilla.io.ElementHelper.SortOrder;
import com.janilla.io.ElementHelper.TypeAndOrder;
import com.janilla.io.IO;

public class Index<K, V> {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("index", "");
		try (var c = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {
			IO.Supplier<Index<String, Object[]>> g = () -> {
				var i = new Index<String, Object[]>();
				i.setInitializeBTree(t -> {
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
				});
				i.setKeyHelper(ElementHelper.STRING);
				i.setValueHelper(ElementHelper.of(new TypeAndOrder(Instant.class, SortOrder.DESCENDING),
						new TypeAndOrder(Long.class, SortOrder.DESCENDING)));
				return i;
			};

			{
				var i = g.get();
				i.add("foo", new Object[] { LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC), 0L });
				i.add("foo", new Object[] { LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC), 1L });
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var i = g.get();
				var j = i.list("foo").mapToLong(x -> (Long) ((Object[]) x)[1]).toArray();
				System.out.println(Arrays.toString(j));
				assert Arrays.equals(j, new long[] { 1, 0 }) : j;
			}
		}
	}

	private IO.Consumer<BTree<KeyAndValues<K>>> initializeBTree;

	private ElementHelper<K> keyHelper;

	private ElementHelper<V> valueHelper;

	private IO.Supplier<BTree<KeyAndValues<K>>> btree = IO.Lazy.of(() -> {
		var t = new BTree<KeyAndValues<K>>();
		initializeBTree.accept(t);
		t.setHelper(KeyAndValues.getHelper(keyHelper));
		return t;
	});

	public void setInitializeBTree(IO.Consumer<BTree<KeyAndValues<K>>> initializeBTree) {
		this.initializeBTree = initializeBTree;
	}

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

	public BTree<KeyAndValues<K>> getBTree() {
		try {
			return btree.get();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public boolean add(K key, V value) throws IOException {
		return apply(key, x -> {
			var v = x.btree.getOrAdd(value);
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

	protected <R> R apply(K key, IO.Function<BTreeAndSize<V>, ResultAndSize<R>> function, boolean add) throws IOException {
		class A {

			ResultAndSize<R> r;
		}
		var t = btree.get();
		var k = new KeyAndValues<K>(key, new BlockReference(-1, -1, 0), 0);
		var a = new A();
		IO.UnaryOperator<KeyAndValues<K>> o = i -> {
			var u = new BTree<V>();
			u.setOrder(t.getOrder());
			u.setChannel(t.getChannel());
			u.setMemory(t.getMemory());
			u.setHelper(valueHelper);
			u.setRoot(i.root);

			a.r = function.apply(new BTreeAndSize<>(u, i.size));
			return u.getRoot().position() != i.root.position() || (a.r != null && a.r.size != i.size)
					? new KeyAndValues<K>(key, u.getRoot(), a.r.size)
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

	public record KeyAndValues<K>(K key, BlockReference root, long size) {

		static <K> ElementHelper<KeyAndValues<K>> getHelper(ElementHelper<K> keyHelper) {
			return new ElementHelper<>() {

				@Override
				public byte[] getBytes(KeyAndValues<K> element) {
					var b = keyHelper.getBytes(element.key());
					var c = ByteBuffer.allocate(b.length + BlockReference.HELPER_LENGTH + 8);
					c.put(b);
					var r = element.root();
					c.putLong(r.position());
					c.putInt(r.capacity());
					c.putLong(element.size());
					return c.array();
				}

				@Override
				public int getLength(ByteBuffer buffer) {
					return keyHelper.getLength(buffer) + BlockReference.HELPER_LENGTH + 8;
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

	protected record BTreeAndSize<V>(BTree<V> btree, long size) {
	}

	protected record ResultAndSize<R>(R result, long size) {
	}
}

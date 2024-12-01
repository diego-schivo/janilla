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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.janilla.io.ElementHelper;
import com.janilla.util.Lazy;

public class Index<K, V> {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("index", "");
		try (var ch = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {
			Supplier<Index<String, Object[]>> is = () -> {
				var i = new Index<String, Object[]>();
				i.setInitializeBTree(x -> {
					try {
						var m = new Memory();
						var ft = m.getFreeBTree();
						ft.setOrder(o);
						ft.setChannel(ch);
						ft.setRoot(BlockReference.read(ch, 0));
						m.setAppendPosition(Math.max(2 * BlockReference.HELPER_LENGTH, ch.size()));

						x.setOrder(o);
						x.setChannel(ch);
						x.setMemory(m);
						x.setRoot(BlockReference.read(ch, BlockReference.HELPER_LENGTH));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
				i.setKeyHelper(ElementHelper.STRING);
				i.setValueHelper(ElementHelper.of(
						new ElementHelper.TypeAndOrder(Instant.class, ElementHelper.SortOrder.DESCENDING),
						new ElementHelper.TypeAndOrder(Long.class, ElementHelper.SortOrder.DESCENDING)));
				return i;
			};

			{
				var i = is.get();
				i.add("foo", new Object[] { LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC), 1L },
						new Object[] { LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC), 2L });
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var i = is.get();
				var ll = i.list("foo").mapToLong(x -> (Long) ((Object[]) x)[1]).toArray();
				System.out.println(Arrays.toString(ll));
				assert Arrays.equals(ll, new long[] { 2, 1 }) : ll;
			}
		}
	}

	private Consumer<BTree<KeyAndValues<K>>> initializeBTree;

	private ElementHelper<K> keyHelper;

	private ElementHelper<V> valueHelper;

	private Supplier<BTree<KeyAndValues<K>>> btree = Lazy.of(() -> {
		var bt = new BTree<KeyAndValues<K>>();
		initializeBTree.accept(bt);
		bt.setHelper(KeyAndValues.getHelper(keyHelper));
		return bt;
	});

	public void setInitializeBTree(Consumer<BTree<KeyAndValues<K>>> initializeBTree) {
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
		return btree.get();
	}

	public boolean add(K key, @SuppressWarnings("unchecked") V... value) {
		return apply(key, x -> {
			var s = x.size;
			for (var v : value) {
				var w = x.btree.getOrAdd(v);
				if (w == null)
					s++;
			}
			return s != x.size ? new ResultAndSize<>(true, s) : null;
		}, true) != null;
	}

	public boolean remove(K key, V value) {
		return apply(key, x -> {
			var v = x.btree.remove(value);
			return v != null ? new ResultAndSize<>(true, x.size - 1) : null;
		}, false) != null;
	}

	public Stream<V> list(K key) {
		var s = apply(key, x -> new ResultAndSize<>(x.btree.stream(), x.size), false);
		return s != null ? s : Stream.empty();
	}

	public long count(K key) {
		var c = apply(key, x -> new ResultAndSize<>(Long.valueOf(x.size), x.size), false);
		return c != null ? c : 0;
	}

	public long countIf(Predicate<K> operation) {
		return btree.get().stream().filter(x -> operation.test(x.key())).mapToLong(x -> x.size()).sum();
	}

	public Stream<K> keys() {
		return btree.get().stream().map(KeyAndValues::key);
	}

	public Stream<V> values() {
		return valuesIf(k -> true);
	}

	public Stream<V> valuesIf(Predicate<K> operation) {
		return btree.get().stream().filter(x -> operation.test(x.key()))
				.flatMap(x -> apply(x.key(), y -> new ResultAndSize<>(y.btree.stream(), y.size), false));
	}

	public long count() {
		return btree.get().stream().mapToLong(KeyAndValues::size).sum();
	}

	protected <R> R apply(K key, Function<BTreeAndSize<V>, ResultAndSize<R>> function, boolean add) {
		class A {

			ResultAndSize<R> r;
		}
		var bt = btree.get();
		var k = new KeyAndValues<K>(key, new BlockReference(-1, -1, 0), 0);
		var a = new A();
		UnaryOperator<KeyAndValues<K>> o = i -> {
			var u = new BTree<V>();
			u.setOrder(bt.getOrder());
			u.setChannel(bt.getChannel());
			u.setMemory(bt.getMemory());
			u.setHelper(valueHelper);
			u.setRoot(i.root());

			a.r = function.apply(new BTreeAndSize<>(u, i.size()));
			return u.getRoot().position() != i.root().position() || (a.r != null && a.r.size != i.size())
					? new KeyAndValues<K>(key, u.getRoot(), a.r.size)
					: null;
		};
		if (add)
			bt.getOrAdd(k, o);
		else
			bt.get(k, o);
		if (a.r != null && a.r.size == 0)
			bt.remove(k);
		return a.r != null ? a.r.result : null;
	}

	protected record BTreeAndSize<V>(BTree<V> btree, long size) {
	}

	protected record ResultAndSize<R>(R result, long size) {
	}
}

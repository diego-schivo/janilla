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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.janilla.io.ElementHelper;
import com.janilla.io.IO;
import com.janilla.json.Json;
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
						m.setAppendPosition(Math.max(2 * BlockReference.BYTES, ch.size()));

						x.setOrder(o);
						x.setChannel(ch);
						x.setMemory(m);
						x.setRoot(BlockReference.read(ch, BlockReference.BYTES));
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
				i.setAttributes(new LinkedHashMap<String, Object>());

				i.add("foo", new Object[][] {
						new Object[] { LocalDateTime.parse("2023-12-03T09:00:00").toInstant(ZoneOffset.UTC), 1L },
						new Object[] { LocalDateTime.parse("2023-12-03T10:00:00").toInstant(ZoneOffset.UTC), 2L } });
			}

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();

			{
				var i = is.get();
				i.setAttributes(new LinkedHashMap<String, Object>(Map.of("size", 2L)));

				var ll = i.list("foo").mapToLong(x -> (long) ((Object[]) x)[1]).toArray();
				System.out.println(Arrays.toString(ll));
				assert Arrays.equals(ll, new long[] { 2, 1 }) : ll;
			}
		}
	}

	private Consumer<BTree<KeyAndData<K>>> initializeBTree;

	private ElementHelper<K> keyHelper;

	private ElementHelper<V> valueHelper;

	private Supplier<BTree<KeyAndData<K>>> btree = Lazy.of(() -> {
		var bt = new BTree<KeyAndData<K>>();
		initializeBTree.accept(bt);
		bt.setHelper(KeyAndData.getHelper(keyHelper));
		return bt;
	});

	private Map<String, Object> attributes;

	public void setInitializeBTree(Consumer<BTree<KeyAndData<K>>> initializeBTree) {
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

	public BTree<KeyAndData<K>> getBTree() {
		return btree.get();
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public boolean add(K key, V[] values) {
//		System.out.println("Index.add key=" + key + ", values="
//				+ Arrays.stream(values).map(x -> x instanceof Object[] oo ? List.of(oo) : x).toList());

		return apply(key, (aa, bt) -> {
			Boolean r = null;
			for (var v : values)
				if (bt.getOrAdd(v) == null) {
					r = Boolean.TRUE;
					aa.compute("size", (k, y) -> y == null ? 1L : (long) y + 1);
					attributes.compute("size", (k, y) -> y == null ? 1L : (long) y + 1);
				}
			return r;
		}, true) != null;
	}

	public boolean remove(K key, V[] values) {
		return apply(key, (aa, bt) -> {
			Boolean r = null;
			for (var v : values)
				if (bt.remove(v) != null) {
					r = Boolean.TRUE;
					aa.compute("size", (k, y) -> y == null ? 1L : (long) y - 1);
					attributes.compute("size", (k, y) -> y == null ? 1L : (long) y - 1);
				}
			return r;
		}, false) != null;
	}

	public Stream<V> list(K key) {
		var s = apply(key, (aa, bt) -> bt.stream(), false);
		return s != null ? s : Stream.empty();
	}

	public long count(K key) {
		var c = apply(key, (aa, bt) -> (long) aa.getOrDefault("size", 0L), false);
		return c != null ? c : 0;
	}

	public long countIf(Predicate<K> operation) {
		return btree.get().stream().filter(x -> operation.test(x.key())).mapToLong(x -> count(x.key())).sum();
	}

	public Stream<K> keys() {
		return btree.get().stream().map(KeyAndData::key);
	}

	public Stream<V> values() {
		return valuesIf(k -> true);
	}

	public Stream<V> valuesIf(Predicate<K> predicate) {
		return btree.get().stream().filter(x -> predicate.test(x.key()))
				.flatMap(x -> apply(x.key(), (aa, vt) -> vt.stream(), false));
	}

	public long count() {
		return (long) attributes.getOrDefault("size", 0L);
	}

	public <R> R apply(K key, BiFunction<Map<String, Object>, BTree<V>, R> function, boolean add) {
		class A {

			R r;

			boolean x;
		}
		var bt = btree.get();
		var kd = new KeyAndData<K>(key, new BlockReference(-1, -1, 0), new BlockReference(-1, -1, 0));
		var a = new A();
		UnaryOperator<KeyAndData<K>> op = x -> {
			String aa1;
			try {
				if (x.attributes().capacity() == 0)
					aa1 = "{}";
				else {
					bt.getChannel().position(x.attributes().position());
					var b = ByteBuffer.allocate(x.attributes().capacity());
					IO.repeat(y -> bt.getChannel().read(b), b.remaining());
					b.position(0);
					aa1 = ElementHelper.STRING.getElement(b);
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			@SuppressWarnings("unchecked")
			var aa = (Map<String, Object>) Json.parse(aa1);

			var vt = new BTree<V>();
			vt.setOrder(bt.getOrder());
			vt.setChannel(bt.getChannel());
			vt.setMemory(bt.getMemory());
			vt.setHelper(valueHelper);
			vt.setRoot(x.btree());

			var s1 = (long) aa.getOrDefault("size", 0L);
			a.r = function.apply(aa, vt);
			var s2 = (long) aa.getOrDefault("size", 0L);
			a.x = s1 != 0 && s2 == 0;

			var aa2 = Json.format(aa);
			var aar = x.attributes();
			if (!aa2.equals(aa1)) {
				var bb = ElementHelper.STRING.getBytes(aa2);
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
			var vtr = vt.getRoot();
			return aar.position() != x.attributes().position() || vtr.position() != x.btree().position()
					? new KeyAndData<>(key, aar, vtr)
					: null;
		};
		kd = add ? bt.getOrAdd(kd, op) : bt.get(kd, op);
		if (a.x)
			bt.remove(kd);
		return a.r;
	}
}

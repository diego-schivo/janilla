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
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.janilla.io.ByteConverter;
import com.janilla.json.Json;

public class Index<K, V> {

	public static void main(String[] args) throws Exception {
		var o = 3;
		var f = Files.createTempFile("index", "");
		try (var ch = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
				StandardOpenOption.WRITE)) {
			Supplier<Index<String, Object[]>> is = () -> {
				try {
					var m = new BTreeMemory(o, ch, BlockReference.read(ch, 0),
							Math.max(2 * BlockReference.BYTES, ch.size()));
					return new Index<>(
							new BTree<>(o, ch, m, KeyAndData.getByteConverter(ByteConverter.STRING),
									BlockReference.read(ch, BlockReference.BYTES)),
							ByteConverter.of(
									new ByteConverter.TypeAndOrder(Instant.class, ByteConverter.SortOrder.DESCENDING),
									new ByteConverter.TypeAndOrder(Long.class, ByteConverter.SortOrder.DESCENDING)));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
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

	protected final BTree<KeyAndData<K>> bTree;

	protected final ByteConverter<V> valueConverter;

	protected Map<String, Object> attributes;

	public Index(BTree<KeyAndData<K>> bTree, ByteConverter<V> valueConverter) {
		this.bTree = bTree;
		this.valueConverter = valueConverter;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public boolean add(K key, V[] values) {
//		System.out.println("Index.add, key=" + key + ", values="
//				+ Arrays.stream(values).map(x -> x instanceof Object[] oo ? List.of(oo) : x).toList());

		return apply(key, (aa, bt) -> {
			Boolean r = null;
			for (var v : values)
				if (bt.getOrAdd(v) == null) {
					r = Boolean.TRUE;
					aa.compute("size", (_, y) -> y == null ? 1L : (long) y + 1);
					attributes.compute("size", (_, y) -> y == null ? 1L : (long) y + 1);
				}
			return r;
		}, true) != null;
	}

	public boolean remove(K key, V[] values) {
//		System.out.println("Index.remove, key=" + key + ", values="
//				+ Arrays.stream(values).map(x -> x instanceof Object[] oo ? List.of(oo) : x).toList());

		return apply(key, (aa, bt) -> {
			Boolean r = null;
			for (var v : values)
				if (bt.remove(v) != null) {
					r = Boolean.TRUE;
					aa.compute("size", (_, y) -> y == null ? 1L : (long) y - 1);
					attributes.compute("size", (_, y) -> y == null ? 1L : (long) y - 1);
				}
			return r;
		}, false) != null;
	}

	public Stream<V> list(K key) {
		var s = apply(key, (_, bt) -> bt.stream(), false);
		return s != null ? s : Stream.empty();
	}

	public long count(K key) {
		var c = apply(key, (aa, _) -> (long) aa.getOrDefault("size", 0L), false);
		return c != null ? c : 0;
	}

	public long countIf(Predicate<K> operation) {
		return bTree.stream().filter(x -> operation.test(x.key())).mapToLong(x -> count(x.key())).sum();
	}

	public Stream<K> keys() {
		return bTree.stream().map(KeyAndData::key);
	}

	public Stream<V> values() {
		return valuesIf(_ -> true);
	}

	public Stream<V> valuesIf(Predicate<K> predicate) {
		return bTree.stream().filter(x -> predicate.test(x.key()))
				.flatMap(x -> apply(x.key(), (_, vt) -> vt.stream(), false));
	}

	public long count() {
		return (long) attributes.getOrDefault("size", 0L);
	}

	public <R> R apply(K key, BiFunction<Map<String, Object>, BTree<V>, R> function, boolean add) {
//		System.out.println(
//				"Index.apply, key=" + (key instanceof Object[] oo ? Arrays.toString(oo) : key) + ", add=" + add);
		class A {

			R r;

			boolean removeKey;
		}
		var bt = bTree;
		var kd = new KeyAndData<K>(key, new BlockReference(-1, -1, 0), new BlockReference(-1, -1, 0));
		var a = new A();
		UnaryOperator<KeyAndData<K>> op = x -> {
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
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			@SuppressWarnings("unchecked")
			var aa = (Map<String, Object>) Json.parse(aa1);
			var vt = new BTree<V>(bt.order(), bt.channel(), bt.memory(), valueConverter, x.bTree());
			var s1 = (long) aa.getOrDefault("size", 0L);
			a.r = function.apply(aa, vt);
			var s2 = (long) aa.getOrDefault("size", 0L);
			a.removeKey = s1 != 0 && s2 == 0;

			var aa2 = Json.format(aa);
//			System.out.println("Index.apply, aa1=" + aa1 + ", aa2=" + aa2);
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
			var vtr = vt.root();
			return aar.position() != x.attributes().position() || vtr.position() != x.bTree().position()
					? new KeyAndData<>(key, aar, vtr)
					: null;
		};
		kd = add ? bt.getOrAdd(kd, op) : bt.get(kd, op);
		if (a.removeKey)
			bt.remove(kd);
		return a.r;
	}

	@Override
	public String toString() {
		return super.toString() + "[" + Objects.toString(attributes) + "]";
	}
}

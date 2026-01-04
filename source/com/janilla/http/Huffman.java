/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
package com.janilla.http;

import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public final class Huffman {

	protected static final HuffmanTable TABLE = new HuffmanTable();

	public static void main(String[] args) {
		var s1 = "www.example.com";
		var b = IntStream.builder();
		encode(s1, b);
		var h = Hpack.toHexString(b.build());
		IO.println("h=" + h);
		assert h.equals("f1e3 c2e5 f23a 6ba0 ab90 f4ff") : h;
		var s2 = decode(Hpack.parseHex(h).iterator(), 12);
		IO.println("s2=" + s2);
		assert s2.equals(s1) : s2;
	}

	public static int encode(String string, IntConsumer bytes) {
		class BitsConsumer implements IntConsumer {

			private final IntConsumer bytes;

			private long current;

			private int currentLength;

			private int length;

			public BitsConsumer(IntConsumer bytes) {
				this.bytes = bytes;
			}

			public int length() {
				return length;
			}

			@Override
			public void accept(int value) {
				accept(value, 8);
			}

			public void accept(int value, int bitsLength) {
				current = (current << bitsLength) | (value & ((1L << bitsLength) - 1));
				var l = currentLength + bitsLength;
				for (; l >= 8; l -= 8) {
					bytes.accept((int) (current >>> (l - 8)));
					length++;
					current &= (1 << (l - 8)) - 1;
				}
				currentLength = l;
			}
		}
		var c = new BitsConsumer(bytes);
		IntStream.concat(string.chars(), IntStream.of(256)).forEach(new IntConsumer() {

			private int totalBitLength;

			@Override
			public void accept(int value) {
				var r = TABLE.get(value);
				var l = r.bitLength();
				if (value == 256) {
					l = 8 - (totalBitLength & 0x07);
					if (l == 8)
						return;
				}
				c.accept(r.code(), l);
				totalBitLength += l;
			}
		});
		return c.length();
	}

	public static String decode(PrimitiveIterator.OfInt bytes, int length) {
		class BitsIterator implements PrimitiveIterator.OfInt {

			private final PrimitiveIterator.OfInt bytes;

			private long current;

			private int currentLength;

			public BitsIterator(PrimitiveIterator.OfInt bytes) {
				this.bytes = bytes;
			}

			@Override
			public boolean hasNext() {
				return hasNext(8);
			}

			@Override
			public int nextInt() {
				return nextInt(8);
			}

			public boolean hasNext(int bitLength) {
				return currentLength >= bitLength || bytes.hasNext();
			}

			public int nextInt(int bitLength) {
				while (currentLength < bitLength) {
					var i = bytes.nextInt();
					current = (current << 8) | i;
					currentLength += 8;
				}
				var l = currentLength - bitLength;
				var i = (int) (current >>> l);
				current &= (1 << l) - 1;
				currentLength = l;
				return i;
			}
		}
		var k = 0;
		var kl = 0;
		var t = length << 3;
		var bi = new BitsIterator(bytes);
		var b = new StringBuilder();
		while (t > 0) {
			var d = TABLE.maxBitLength() - kl;
			var l = Math.min(d, t - kl);
			var i = bi.nextInt(l);
			k = (k << l) | i;
			kl += l;
			var r = TABLE.row(k << (d - l));
			if (r.bitLength() > t)
				break;
			b.append((char) r.symbol());
			kl -= r.bitLength();
			k &= (1 << kl) - 1;
			t -= r.bitLength();
		}
		return b.toString();
	}
}

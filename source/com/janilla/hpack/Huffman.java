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
package com.janilla.hpack;

import java.util.Arrays;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.IntStream.IntMapMultiConsumer;

public class Huffman {

	static HuffmanTable table = new HuffmanTable();

	public static void main(String[] args) {
		var s1 = "www.example.com";
		var ii = encode(s1).toArray();
		var h = Hpack.toHexString(Arrays.stream(ii));
		System.out.println("h=" + h);
		assert h.equals("f1e3 c2e5 f23a 6ba0 ab90 f4ff") : h;
		var s2 = decode(Hpack.parseHex(h).iterator(), ii.length);
		System.out.println("s2=" + s2);
		assert s2.equals(s1) : s2;
	}

	static IntStream encode(String string) {
		return IntStream.concat(string.chars(), IntStream.of(256)).mapMulti(new IntMapMultiConsumer() {

			long bits;

			int length;

			@Override
			public void accept(int value, IntConsumer ic) {
				if (value == 256 && length == 0)
					return;
				var c = table.get(value);
				length += c.bitLength();
				bits |= ((long) c.code()) << (64 - length);
				var n = value == 256 ? 1 : length >> 3;
				for (var i = 1; i <= n; i++) {
					ic.accept((int) ((bits >>> (64 - 8)) & 0xff));
					bits <<= 8;
					length -= 8;
				}
			}
		});
	}

	static String decode(PrimitiveIterator.OfInt bytes, int length) {
		var bb = 0L;
		var l = 0;
		var b = new StringBuilder();
		for (var i = 0; i < length || l > 0;) {
			for (; l < table.maxBitLength() && i < length; l += 8) {
				bb = (bb << 8) | (bytes.hasNext() ? bytes.nextInt() : 0xff);
				i++;
			}
			var d = l - table.maxBitLength();
			var r = table.row((int) (d >= 0 ? bb >>> d : bb << -d));
			if (l < r.bitLength())
				break;
			b.append((char) r.symbol());
			l -= r.bitLength();
			bb &= (1L << l) - 1;
		}
		return b.toString();
	}
}

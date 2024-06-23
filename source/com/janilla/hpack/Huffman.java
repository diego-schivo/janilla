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

import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.IntStream.IntMapMultiConsumer;

public class Huffman {

	static HuffmanTable table = new HuffmanTable();

	public static void main(String[] args) {
		var s = "www.example.com";
		var h = Hpack.toHexString(encode(s));
		System.out.println("h=" + h);
		s = decode(Hpack.parseHex(h).iterator(), s.length());
		System.out.println("s=" + s);
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
		var i = 0L;
		var l = 0;
		var cc = new StringBuilder();
		for (var z = 0; z < length;) {
			for (; l < table.maxBitLength() && z < length; l += 8) {
				int y;
				if (bytes.hasNext()) {
					y = bytes.nextInt();
					z++;
				} else
					y = 0xff;
				i = (i << 8) | y;
			}
			var r = table.row((int) (i >>> (l - table.maxBitLength())));
			cc.append((char) r.symbol());
			l -= r.bitLength();
			i &= (1L << l) - 1;
		}
		return cc.toString();
	}
}

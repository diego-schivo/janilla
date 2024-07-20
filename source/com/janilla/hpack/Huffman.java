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

import com.janilla.util.BitsConsumer;
import com.janilla.util.BitsIterator;
import com.janilla.util.Util;

public class Huffman {

	static HuffmanTable table = new HuffmanTable();

	public static void main(String[] args) {
		var s1 = "www.example.com";
		var bb = IntStream.builder();
		encode(s1, bb);
		var h = Util.toHexString(bb.build());
		System.out.println("h=" + h);
		assert h.equals("f1e3 c2e5 f23a 6ba0 ab90 f4ff") : h;
		var s2 = decode(Util.parseHex(h).iterator(), 12);
		System.out.println("s2=" + s2);
		assert s2.equals(s1) : s2;
	}

	static int encode(String string, IntConsumer bytes) {
		var bc = new BitsConsumer(bytes);
		IntStream.concat(string.chars(), IntStream.of(256)).forEach(new IntConsumer() {

			int totalBitLength;

			@Override
			public void accept(int value) {
				var r = table.get(value);
				var bl = r.bitLength();
				if (value == 256) {
					bl = 8 - (totalBitLength & 0x07);
					if (bl == 8)
						return;
				}
				bc.accept(r.code(), bl);
				totalBitLength += bl;
			}
		});
		return bc.length();
	}

	static String decode(PrimitiveIterator.OfInt bytes, int length) {
		var k = 0;
		var kl = 0;
		var t = length << 3;
		var bi = new BitsIterator(bytes);
		var b = new StringBuilder();
		while (t > 0) {
			var d = table.maxBitLength() - kl;
			var l = Math.min(d, t - kl);
			var i = bi.nextInt(l);
			k = (k << l) | i;
			kl += l;
			var r = table.row(k << (d - l));
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

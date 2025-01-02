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
package com.janilla.util;

public interface Base32 {

	static String encode(byte[] bytes) {
		var c = new char[Math.ceilDiv(bytes.length << 3, 5)];
		for (var i = 0; i < c.length; i++) {
			var j = i * 5;
			var d = j >>> 3;
			var m = j & 7;
			var h = 8 - m;
			int k;
			if (h >= 5)
				k = (Byte.toUnsignedInt(bytes[d]) >>> (h - 5)) & 0x1f;
			else {
				var l = 5 - h;
				var x = (Byte.toUnsignedInt(bytes[d]) << l) & 0x1f;
				var y = Byte.toUnsignedInt(bytes[d + 1]) >>> (8 - l);
				k = x | y;
			}
			c[i] = (char) (k < 26 ? 'A' + k : '2' + k - 26);
		}
		return new String(c);
	}

	static byte[] decode(String string) {
		var c = string.toCharArray();
		var b = new byte[(c.length * 5) >>> 3];
		for (var i = 0; i < c.length; i++) {
			var x = c[i] >= 'A' ? c[i] - 'A' : 26 + c[i] - '2';
			var j = i * 5;
			var d = j >>> 3;
			var m = j & 7;
			var h = 8 - m;
			if (h >= 5)
				b[d] |= x << (h - 5);
			else {
				var l = 5 - h;
				b[d] |= x >>> l;
				b[d + 1] = (byte) (x << (8 - l));
			}
		}
		return b;
	}
}

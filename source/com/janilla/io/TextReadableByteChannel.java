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
package com.janilla.io;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class TextReadableByteChannel extends BufferedReadableByteChannel {

	public TextReadableByteChannel(ReadableByteChannel channel, ByteBuffer buffer) {
		super(channel, buffer);
	}

	public String readLine() {
//		System.out.println(Thread.currentThread().getName() + " TextReadableByteChannel.readLine");

		// var t = System.currentTimeMillis();

		var i1 = 0;
		var a = new byte[2000];
		var i = 0;
		String l;
		for (;;) {
			var i2 = readByte();

//			System.out.println("i2=" + i2);

			if (i2 < 0) {
				l = i > 0 ? new String(a, 0, i) : null;
				break;
			}
			a[i++] = (byte) i2;
			if (i1 == '\r' && i2 == '\n') {
				l = new String(a, 0, i - 2);
				break;
			}
			i1 = i2;
		}

//		System.out.println(Thread.currentThread().getName() + " TextReadableByteChannel.readLine l " + l + " "
//				+ (System.currentTimeMillis() - t));

		return l;
	}
}

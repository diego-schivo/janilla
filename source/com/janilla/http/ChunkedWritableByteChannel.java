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
package com.janilla.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

public class ChunkedWritableByteChannel extends HttpBufferedWritableByteChannel {

	public static void main(String[] args) throws IOException {
		var i = new ByteArrayInputStream("foobarbazqux".getBytes());
		var o = new ByteArrayOutputStream();
		try (var c = new ChunkedWritableByteChannel(Channels.newChannel(o), ByteBuffer.allocate(10))) {
			i.transferTo(Channels.newOutputStream(c));
		}

		var t = o.toString();
		System.out.println(t);
		assert Objects.equals(t, """
				A\r
				foobarbazq\r
				2\r
				ux\r
				0\r
				\r
				""") : t;
	}

	protected int chunk;

	public ChunkedWritableByteChannel(WritableByteChannel channel, ByteBuffer buffer) {
		super(channel, buffer);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		var n = super.write(src);

//		if (n > 0)
//			System.out.println(
//					">>> " + new String(src.array(), src.position() - n, src.position()).replace("\n", "\n>>> "));

		return n;
	}

	@Override
	public void flush() throws IOException {
		super.flush();
		writeLineToChannel("0");
		writeLineToChannel("");
	}

	@Override
	protected int writeBuffer(boolean all, int max) throws IOException {
		if (max == 0)
			return 0;

		var p = buffer.position();
		if (p == 0)
			return 0;

		var n = 0;
		do {
			if (chunk == 0) {
				var s = Integer.toString(p, 16).toUpperCase();
				writeLineToChannel(s);
				chunk = p;
			}
			var o = super.writeBuffer(all, max < 0 ? chunk : Math.min(chunk, max - n));
			p -= o;
			n += o;
			chunk -= o;
			if (chunk == 0)
				writeLineToChannel("");
		} while (all && (max < 0 || n < max) && p > 0);
		return n;
	}
}

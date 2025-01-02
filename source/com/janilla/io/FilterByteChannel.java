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
package com.janilla.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

public class FilterByteChannel extends FilterChannel<ByteChannel> implements ByteChannel {

	public static void main(String[] args) throws Exception {
		var s = new ByteBufferHolder();
		s.setBuffer(ByteBuffer.wrap("foo".getBytes()).position(3));
		var d = new ByteBufferHolder();
		d.setBuffer(ByteBuffer.allocate(10));

		try (var c = new FilterByteChannel(new ByteBufferByteChannel(s, d))) {
			var a = new byte[3];
			IO.read(c, a);
			var t = new String(a);
			System.out.println(t);
			assert t.equals("foo") : t;

			IO.write("bar".getBytes(), c);
			var b = d.getBuffer();
			t = new String(b.array(), 0, b.position());
			System.out.println(t);
			assert t.equals("bar") : t;
		}
	}

	public FilterByteChannel(ByteChannel channel) {
		super(channel);
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return channel.read(dst);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
//		System.out.println("src=" + src);
		return channel.write(src);
	}
}

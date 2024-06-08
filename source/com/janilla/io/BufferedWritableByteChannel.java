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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;

public class BufferedWritableByteChannel extends FilterWritableByteChannel {

	public static void main(String[] args) throws Exception {
		var o = new ByteArrayOutputStream();
		try (var c = new BufferedWritableByteChannel(Channels.newChannel(o), ByteBuffer.allocate(10))) {
			for (var s : List.of("foo", "bar", "baz")) {
				IO.write(s.getBytes(), c);
				var t = o.toString();
				System.out.println(t);
				assert t.equals("") : t;
			}

			IO.write("qux".getBytes(), c);
			var t = o.toString();
			System.out.println(t);
			assert t.equals("foobarbazq") : t;
		}

		var t = o.toString();
		System.out.println(t);
		assert t.equals("foobarbazqux") : t;
	}

	protected ByteBuffer buffer;

	public BufferedWritableByteChannel(WritableByteChannel channel, ByteBuffer buffer) {
		super(channel);
		this.buffer = buffer;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		var n1 = src.remaining();
		if (n1 == 0)
			return 0;
		if (!buffer.hasRemaining() && writeBuffer(false, -1) == 0)
			return 0;
		var n2 = buffer.remaining();
		int n, l;
		if (n1 > n2) {
			n = n2;
			l = src.limit();
			src.limit(src.position() + n);
		} else {
			n = n1;
			l = -1;
		}
		buffer.put(src);
		if (l >= 0)
			src.limit(l);
		return n;
	}

	@Override
	public void close() throws IOException {
		try {
			flush();
		} catch (Exception e) {
		}
		super.close();
	}

	public void flush() throws IOException {
		writeBuffer(true, -1);
	}

	protected int writeBuffer(boolean all, int max) throws IOException {
		var p = buffer.position();
		if (p == 0)
			return 0;
		buffer.flip();
		if (max >= 0 && max < p)
			buffer.limit(max);
		var n = 0;
		try {
			do
				n += super.write(buffer);
			while (all && buffer.hasRemaining());
		} finally {
			if (max >= 0 && max < p)
				buffer.limit(p);
			buffer.compact();
		}

//		System.out.println(Thread.currentThread().getName() + " BufferedWritableByteChannel.write n " + n);

		return n;
	}

	protected void writeByte(byte b) throws IOException {
		while (!buffer.hasRemaining() && writeBuffer(false, -1) == 0)
			;
		buffer.put(b);
	}
}

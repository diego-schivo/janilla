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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class BufferedReadableByteChannel extends FilterReadableByteChannel {

	public static void main(String[] args) throws Exception {
		var i = new ByteArrayInputStream("foobarbazqux".getBytes());
		try (var c = new BufferedReadableByteChannel(Channels.newChannel(i), ByteBuffer.allocate(10))) {
			var b = ByteBuffer.allocate(3);
			c.read(b);

			var s = new String(b.array(), 0, b.position());
			System.out.println(s);
			assert s.equals("foo") : s;

			for (var j = 0; j < 3; j++) {
				b.clear();
				c.read(b);
			}

			s = new String(b.array(), 0, b.position());
			System.out.println(s);
			assert s.equals("q") : s;

			b.clear();
			c.read(b);

			s = new String(b.array(), 0, b.position());
			System.out.println(s);
			assert s.equals("ux") : s;
		}
	}

	protected ByteBuffer buffer;

	protected int index;

	protected boolean ended;

	public BufferedReadableByteChannel(ReadableByteChannel channel) {
		this(channel, ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY));
	}

	public BufferedReadableByteChannel(ReadableByteChannel channel, ByteBuffer buffer) {
		super(channel);
		this.buffer = buffer;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (index == buffer.position() && readBuffer() == 0)
			return ended ? -1 : 0;
		var p = buffer.position();
		var l = p - index;
		var n = Math.min(dst.remaining(), l);
		buffer.position(index);
		buffer.limit(index + n);
		dst.put(buffer);
		buffer.limit(buffer.capacity());
		buffer.position(p);
		index += n;
		return n;
	}

	protected int readBuffer() {
		if (ended)
			return 0;
		if (!buffer.hasRemaining()) {
			buffer.clear();
			index = 0;
		}
		int n;
		try {
			n = super.read(buffer);
//			if (n != 0) System.out.println("n=" + n);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		if (n < 0) {
			ended = true;
			return 0;
		}
		return n;
	}

	protected int readByte() {
		if (index == buffer.position())
			while (readBuffer() == 0)
				if (ended)
					return -1;
		return buffer.array()[index++];
	}
}

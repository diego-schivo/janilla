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
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

import com.janilla.io.IO;

public class ChunkedReadableByteChannel extends HttpBufferedReadableByteChannel {

	public static void main(String[] args) throws Exception {
		var i = new ByteArrayInputStream("""
				A\r
				foobarbazq\r
				2\r
				ux\r
				0\r
				\r
				""".getBytes());
		var o = new ByteArrayOutputStream();
		try (var c = new ChunkedReadableByteChannel(Channels.newChannel(i))) {
			Channels.newInputStream(c).transferTo(o);
		}

		var t = o.toString();
		System.out.println(t);
		assert Objects.equals(t, "foobarbazqux") : t;
	}

	protected long length;

	protected long count;

	protected int chunk;

	protected boolean ended;

	public ChunkedReadableByteChannel(ReadableByteChannel channel) {
		this(channel, ByteBuffer.allocate(IO.DEFAULT_BUFFER_CAPACITY));
	}

	public ChunkedReadableByteChannel(ReadableByteChannel channel, ByteBuffer buffer) {
		super(channel, buffer);
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (ended)
			return -1;

		if (count >= length) {
			var s = readLine();
			chunk = s != null ? Integer.parseInt(s, 16) : 0;
			if (chunk < 0)
				throw new IOException();

			if (chunk > 0)
				length += chunk;
			else {
				ended = true;
				s = readLine();
				if (!s.isEmpty())
					throw new IOException();
				return -1;
			}
		}

		var l = dst.limit();
		if (count + dst.remaining() > length)
			dst.limit(dst.position() + (int) (length - count));

		int n;
		try {
			n = super.read(dst);
		} finally {
			dst.limit(l);
		}

		if (n > 0)
			count += n;

		if (count >= length) {
			var s = readLine();
			if (!s.isEmpty())
				throw new IOException();
		}

		return n;
	}
}

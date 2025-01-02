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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

public class LengthReadableByteChannel extends FilterReadableByteChannel {

	public static void main(String[] args) throws Exception {
		var i = new ByteArrayInputStream("foobarbazqux".getBytes());
		var o = new ByteArrayOutputStream();
		try (var sc = new LengthReadableByteChannel(Channels.newChannel(i), 10); var tc = Channels.newChannel(o)) {
			IO.transfer(sc, tc);
		}

		var t = o.toString();
		System.out.println(t);
		assert Objects.equals(t, "foobarbazq") : t;
	}

	protected long length;

	protected long count;

	protected boolean ended;

	public LengthReadableByteChannel(ReadableByteChannel channel, long length) {
		super(channel);
		if (length < 0)
			throw new IllegalArgumentException();
		if (length > 0)
			this.length = length;
		else
			ended = true;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (ended)
			return -1;

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

		if (count >= length)
			ended = true;

		return n;
	}
}

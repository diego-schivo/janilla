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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;

public class ByteBufferByteChannel implements ByteChannel {

	public static void main(String[] args) throws Exception {
		var s = new ByteBufferHolder();
		s.setBuffer(ByteBuffer.allocate(10));
		var d = new ByteBufferHolder();
		d.setBuffer(ByteBuffer.allocate(10));
		var c1 = new ByteBufferByteChannel(s, d);
		var c2 = new ByteBufferByteChannel(d, s);

		new ByteArrayInputStream("foobar".getBytes()).transferTo(Channels.newOutputStream(c1));
		var os = new ByteArrayOutputStream();
		var ba = new byte[6];
		IO.read(c2, ba);
		var t = new String(ba);
		System.out.println(t);
		assert t.equals("foobar") : t;

		new ByteArrayInputStream("bazqux".getBytes()).transferTo(Channels.newOutputStream(c2));
		os.reset();
		IO.read(c1, ba);
		t = new String(ba);
		System.out.println(t);
		assert t.equals("bazqux") : t;
	}

	protected ByteBufferHolder source;

	protected ByteBufferHolder destination;

	public ByteBufferByteChannel(ByteBufferHolder source, ByteBufferHolder destination) {
		this.source = source;
		this.destination = destination;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		var s = source.getBuffer();
		s.flip();
		try {
			return IO.put(s, dst);
		} finally {
			s.compact();
		}
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		var d = destination.getBuffer();
		while (src.remaining() > d.remaining())
			d = destination.grow();
		return IO.put(src, d);
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public void close() throws IOException {
	}
}

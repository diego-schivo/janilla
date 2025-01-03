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
import java.nio.channels.WritableByteChannel;

public class TextWritableByteChannel extends BufferedWritableByteChannel {

	public TextWritableByteChannel(WritableByteChannel channel, ByteBuffer buffer) {
		super(channel, buffer);
	}

	public void writeLine(String line) throws IOException {

//		System.out.println(
//				Thread.currentThread().getName() + " TextWritableByteChannel.writeLine line=" + line);

		var a = line.getBytes();
		var b = "\r\n".getBytes();
		var l = a.length + b.length;
		if (l <= buffer.capacity()) {
			while (buffer.remaining() < l)
				writeBuffer(false, -1);
			buffer.put(a);
			buffer.put(b);
		} else {
			writeBuffer(true, -1);
			IO.write(a, channel);
			IO.write(b, channel);
		}
	}

	protected void writeLineToChannel(String line) throws IOException {

//		System.out.println(
//				Thread.currentThread().getName() + " TextWritableByteChannel.writeLineToChannel line=" + line);

		var a = line.getBytes();
		var b = "\r\n".getBytes();
		IO.write(a, channel);
		IO.write(b, channel);
	}
}

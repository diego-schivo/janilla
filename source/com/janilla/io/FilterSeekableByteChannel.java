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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class FilterSeekableByteChannel extends FilterChannel<SeekableByteChannel> implements SeekableByteChannel {

	public FilterSeekableByteChannel(SeekableByteChannel channel) {
		super(channel);
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return channel.read(dst);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return channel.write(src);
	}

	@Override
	public long position() throws IOException {
		return channel.position();
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		channel.position(newPosition);
		return this;
	}

	@Override
	public long size() throws IOException {
		return channel.size();
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		channel.truncate(size);
		return this;
	}
}

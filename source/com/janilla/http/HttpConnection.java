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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;

import com.janilla.io.FilterByteChannel;
import com.janilla.io.IO;

public class HttpConnection implements Closeable {

	protected SocketChannel channel;

	protected HttpMessageReadableByteChannel input;

	protected HttpMessageWritableByteChannel output;

	public HttpConnection(SocketChannel channel, SSLEngine sslEngine) {
		this(channel, sslEngine, IO.DEFAULT_BUFFER_CAPACITY, -1);
	}

	public HttpConnection(SocketChannel channel, SSLEngine sslEngine, int bufferCapacity, long maxMessageLength) {
		this.channel = channel;
		var c = sslEngine != null ? new SSLByteChannel(channel, sslEngine) : channel;

		c = new FilterByteChannel(c) {

			@Override
			public int read(ByteBuffer dst) throws IOException {
				var n = super.read(dst);

//				if (n > 0)
//					System.out.println("<<< " + new String(dst.array(), dst.position() - n, n).replace("\n", "\n<<< "));
//					try {
//						Thread.sleep(1);
//					} catch (InterruptedException e) {
//					}

				return n;
			}

			@Override
			public int write(ByteBuffer src) throws IOException {
				var n = super.write(src);

//				if (n > 0)
//					System.out.println(">>> " + new String(src.array(), src.position() - n, n).replace("\n", "\n>>> "));
//					try {
//						Thread.sleep(1);
//					} catch (InterruptedException e) {
//					}

				return n;
			}

			@Override
			public void close() throws IOException {
				super.close();
			}
		};

		input = new HttpMessageReadableByteChannel(c, ByteBuffer.allocate(bufferCapacity), maxMessageLength);
		output = new HttpMessageWritableByteChannel(c, ByteBuffer.allocate(bufferCapacity));
	}

	public SocketChannel getChannel() {
		return channel;
	}

	public HttpMessageReadableByteChannel getInput() {
		return input;
	}

	public HttpMessageWritableByteChannel getOutput() {
		return output;
	}

	@Override
	public void close() throws IOException {
		try {
			input.close();
		} catch (IOException e) {
		}
		try {
			output.close();
		} catch (IOException e) {
		}
		while (channel.isOpen())
			channel.close();
	}
}

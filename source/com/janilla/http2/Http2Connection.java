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
package com.janilla.http2;

import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashSet;

import com.janilla.hpack.HeaderDecoder;
import com.janilla.net.Connection;
import com.janilla.net.SSLByteChannel;

public class Http2Connection extends Connection {

	HeaderDecoder headerDecoder = new HeaderDecoder();

	Collection<Stream> streams = new HashSet<>();

	boolean p1;

	boolean p2;

	public Http2Connection(int number, SocketChannel channel, SSLByteChannel sslChannel) {
		super(number, channel, sslChannel);
	}

	HeaderDecoder headerDecoder() {
		return headerDecoder;
	}

	Collection<Stream> streams() {
		return streams;
	}
	
	long start = System.currentTimeMillis();
	
	long millis() {
		return System.currentTimeMillis() - start;
	}
}

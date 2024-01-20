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
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import com.janilla.io.Transaction.Range;

public class TransactionalByteChannel extends FilterSeekableByteChannel {

	public static void main(String[] args) throws Exception {
		var f = Files.createTempFile("foo", "");
		var g = Files.createTempFile("foo", ".transaction");
		try (var c = new TransactionalByteChannel(
				FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE),
				FileChannel.open(g, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE))) {
			c.startTransaction();
			{
				var b = ByteBuffer.wrap("foobar".getBytes());
				IO.repeat(x -> c.write(b), b.remaining());
			}
			c.commitTransaction();

			c.startTransaction();
			{
				c.position(3);
				var b = ByteBuffer.wrap("bazqux".getBytes());
				IO.repeat(x -> c.write(b), b.remaining());

				try {
					new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();
					new ProcessBuilder("hexdump", "-C", g.toString()).inheritIO().start().waitFor();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			c.rollbackTransaction();

			new ProcessBuilder("hexdump", "-C", f.toString()).inheritIO().start().waitFor();
		}
	}

	SeekableByteChannel transactionChannel;

	Transaction transaction;

	public TransactionalByteChannel(SeekableByteChannel channel, SeekableByteChannel transactionChannel)
			throws IOException {
		super(channel);
		this.transactionChannel = transactionChannel;
		rollbackTransaction();
	}

	public void startTransaction() throws IOException {
		transaction = new Transaction(channel.size());
	}

	public void commitTransaction() throws IOException {
		transaction = null;
		transactionChannel.truncate(0);
	}

	public void rollbackTransaction() throws IOException {
		transaction = null;
		var s = transactionChannel.size();
		if (s == 0)
			return;
		var b = ByteBuffer.allocate((int) Math.min(s, IO.DEFAULT_BUFFER_CAPACITY));
		transactionChannel.position(0);
		for (var p = 0L; p < s;) {
			if (b.position() < 8 + 4) {
				b.limit(b.capacity());
				var x = b;
				IO.repeat(y -> transactionChannel.read(x), b.remaining());
			}

			var q = b.getLong(0);
			var l = b.getInt(8);
			if (l < 0) {
				channel.truncate(q);
				q += l;
				l = -l;
			}

			b.limit(b.position());
			b.position(8 + 4);
			channel.position(q);
			for (var n = 0; n < l;) {
				if (!b.hasRemaining()) {
					b.clear();
					var x = b;
					IO.repeat(y -> transactionChannel.read(x), b.remaining());
					b.flip();
				}
				var x = b;
				n += IO.repeat(y -> channel.write(x), Math.min(l - n, b.remaining()));
			}

			b.compact();
			p += 8 + 4 + l;
		}
		transactionChannel.truncate(0);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		if (src.remaining() > 0) {
			var p = channel.position();
			var i = transaction.include(new Range(p, p + src.remaining()));
			if (i != null) {
				var l = (int) (i.end() - i.start());
				var b = ByteBuffer.allocate(8 + 4 + Math.abs(l));
				b.putLong(i.start());
				b.putInt(l);
				channel.position(l >= 0 ? i.start() : i.end());
				IO.repeat(x -> channel.read(b), b.remaining());
				b.flip();
				IO.repeat(x -> transactionChannel.write(b), b.remaining());
				channel.position(p);
			}
		}
		return super.write(src);
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() throws IOException {
		super.close();
		transactionChannel.close();
	}
}

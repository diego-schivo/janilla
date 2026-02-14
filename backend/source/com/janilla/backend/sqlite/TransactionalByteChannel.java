/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
package com.janilla.backend.sqlite;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;

import com.janilla.java.FilterSeekableByteChannel;

public class TransactionalByteChannel extends FilterSeekableByteChannel<SeekableByteChannel> {

	protected final SeekableByteChannel transactionChannel;

	protected Transaction transaction;

	public TransactionalByteChannel(SeekableByteChannel channel, SeekableByteChannel transactionChannel)
			throws IOException {
		super(channel);
		this.transactionChannel = transactionChannel;
		rollbackTransaction();
	}

	public SeekableByteChannel transactionChannel() {
		return transactionChannel;
	}

	public void startTransaction() throws IOException {
		transaction = new Transaction(new ArrayList<>(), channel.size());
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
		var b = ByteBuffer.allocate((int) Math.min(s, 16384));
		transactionChannel.position(0);
		for (var p1 = 0L; p1 < s;) {
			if (b.position() < Long.BYTES + Integer.BYTES) {
				b.limit((int) Math.min(s - (p1 + b.position()), b.capacity()));
				transactionChannel.read(b);
				if (b.hasRemaining())
					throw new IOException();
			}

			var p2 = b.getLong(0);
			var l = b.getInt(Long.BYTES);
			if (l < 0) {
				l = -l;
				channel.truncate(p2);
				p2 -= l;
			}

			var p0 = b.position();
			b.limit(Math.min(Long.BYTES + Integer.BYTES + l, p0));
			b.position(Long.BYTES + Integer.BYTES);
			channel.position(p2);
			for (var n = 0; n < l;) {
				if (!b.hasRemaining()) {
					b.limit((int) Math.min(s - (p1 + Long.BYTES + Integer.BYTES + n), b.capacity()));
					b.position(0);
					transactionChannel.read(b);
					if (b.hasRemaining())
						throw new IOException();
					p0 = b.position();
					b.limit(Math.min(l - n, p0));
					b.position(0);
				}
				if (l - n < b.remaining())
					b.limit(b.position() + l - n);
				n += channel.write(b);
				if (b.hasRemaining())
					throw new IOException();
			}
			b.limit(p0);
			b.compact();
			p1 += Long.BYTES + Integer.BYTES + l;
		}
		transactionChannel.truncate(0);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		extracted(src);
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

	protected void extracted(ByteBuffer src) throws IOException {
		if (!src.hasRemaining())
			return;

		var p = channel.position();
		if (p >= transaction.startSize())
			return;

		Iterator<Range> rr;
		{
			var r = new Range(p, Math.min(p + src.remaining(), transaction.startSize()));
			rr = Range.union(transaction.writeRanges(), r).iterator();
		}
		if (!rr.hasNext())
			return;

		do {
			var r = rr.next();
			var l = (int) (r.to() - r.from());
			var b = ByteBuffer.allocate(Long.BYTES + Integer.BYTES + l);

			if (r.to() == transaction.startSize()) {
				b.putLong(r.to());
				b.putInt(-l);
			} else {
				b.putLong(r.from());
				b.putInt(l);
			}

			channel.position(r.from());
			channel.read(b);
			if (b.hasRemaining())
				throw new IOException();

			b.flip();
			transactionChannel.write(b);
			if (b.hasRemaining())
				throw new IOException();
		} while (rr.hasNext());

		channel.position(p);
	}

	public static void main(String[] args) throws Exception {
		var f1 = Files.createTempFile("foo", "");
		var f2 = Files.createTempFile("foo", ".transaction");
		try (var ch = new TransactionalByteChannel(
				FileChannel.open(f1, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE),
				FileChannel.open(f2, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE))) {
			ch.startTransaction();
			{
				var b = ByteBuffer.wrap("foobar".getBytes());
				ch.write(b);
				if (b.hasRemaining())
					throw new IOException();
			}
			ch.commitTransaction();

			new ProcessBuilder("hexdump", "-C", f1.toString()).inheritIO().start().waitFor();

			ch.startTransaction();
			{
				ch.position(3);
				var b = ByteBuffer.wrap("bazqux".getBytes());
				ch.write(b);
				if (b.hasRemaining())
					throw new IOException();

				new ProcessBuilder("hexdump", "-C", f1.toString()).inheritIO().start().waitFor();
				new ProcessBuilder("hexdump", "-C", f2.toString()).inheritIO().start().waitFor();
			}
			ch.rollbackTransaction();

			new ProcessBuilder("hexdump", "-C", f1.toString()).inheritIO().start().waitFor();
		}
	}
}

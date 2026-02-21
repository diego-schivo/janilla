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
package com.janilla.http;

import java.io.IOException;

import com.janilla.net.SecureTransfer;

public class FrameTransfer {

	protected final SecureTransfer transfer;

	protected final HttpEncoder encoder = new HttpEncoder();

	protected final HttpDecoder decoder = new HttpDecoder();

	public FrameTransfer(SecureTransfer transfer) {
		this.transfer = transfer;
	}

	public Frame readFrame() throws IOException {
		var bb = readBytes();
		var f = bb != null ? decoder.decodeFrame(bb) : null;
//		IO.println("FrameTransfer.readFrame, f=" + f);
		return f;
	}

	public void writeFrame(Frame frame) throws IOException {
//		IO.println("FrameTransfer.writeFrame, frame=" + frame);
		writeBytes(encoder.encodeFrame(frame));
	}

	protected byte[] readBytes() throws IOException {
		transfer.inLock().lock();
		try {
			byte[] bb;
			{
				if (transfer.in().remaining() < 3) {
					transfer.in().compact();
					do
						if (transfer.read() == -1)
							return null;
					while (transfer.in().position() < 3);
					transfer.in().flip();
				}
				var l = (Short.toUnsignedInt(transfer.in().getShort(transfer.in().position())) << 8)
						| Byte.toUnsignedInt(transfer.in().get(transfer.in().position() + Short.BYTES));
//				IO.println("FrameTransfer.readBytes, l=" + l);
				if (l > 16384)
					throw new IOException("l=" + l);
				bb = new byte[9 + l];
			}

			for (var o = 0;;) {
				var l = Math.min(transfer.in().remaining(), bb.length - o);
				transfer.in().get(bb, o, l);
				o += l;
				if (o == bb.length)
					break;

				transfer.in().clear();
				if (transfer.read() == -1)
					return null;
				transfer.in().flip();
			}

			return bb;
		} finally {
			transfer.inLock().unlock();
		}
	}

	protected void writeBytes(byte[] bytes) throws IOException {
//		IO.println("FrameTransfer.writeBytes, bytes=" + bytes.length);
		transfer.outLock().lock();
//		IO.println("FrameTransfer.writeBytes, lock");
		try {
			transfer.out().clear();
			for (var o = 0; o < bytes.length;) {
				var l = Math.min(bytes.length - o, transfer.out().remaining());
//				IO.println("FrameTransfer.writeBytes, l=" + l);
				transfer.out().put(bytes, o, l);
				if (!transfer.out().hasRemaining()) {
					transfer.out().flip();
					do
						transfer.write();
					while (transfer.out().hasRemaining());
					transfer.out().clear();
				}
				o += l;
//				IO.println("FrameTransfer.writeBytes, o=" + o);
			}
			for (transfer.out().flip(); transfer.out().hasRemaining();)
				transfer.write();
		} finally {
			transfer.outLock().unlock();
//			IO.println("FrameTransfer.writeBytes, unlock");
		}
	}
}

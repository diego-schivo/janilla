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
package com.janilla.http;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

public class HttpEncoder {

	public byte[] encodeFrame(Frame frame) {
//		System.out.println("HttpEncoder.encodeFrame, frame=" + frame);
		int pl;
		var bb1 = ByteBuffer.allocate(9);
		ByteBuffer bb2;
		switch (frame) {
		case Frame.Data x:
			pl = x.data().length;
			bb1.putShort((short) ((pl >>> 8) & 0xffff));
			bb1.put((byte) pl);
			bb1.put((byte) Frame.Name.DATA.type());
			bb1.put((byte) ((x.padded() ? 0x08 : 0x00) | (x.endStream() ? 0x01 : 0x00)));
			bb1.putInt(x.streamIdentifier());
			bb1.flip();
			bb2 = ByteBuffer.allocate(bb1.remaining() + pl);
			bb2.put(bb1);
			bb2.put(x.data());
			break;
		case Frame.Goaway x:
			pl = 2 * Integer.BYTES + x.additionalDebugData().length;
			bb1.putShort((short) ((pl >>> 8) & 0xffff));
			bb1.put((byte) pl);
			bb1.put((byte) Frame.Name.GOAWAY.type());
			bb1.putInt(x.streamIdentifier());
			bb1.flip();
			bb2 = ByteBuffer.allocate(bb1.remaining() + pl);
			bb2.put(bb1);
			bb2.putInt(x.lastStreamId());
			bb2.putInt(x.errorCode());
			bb2.put(x.additionalDebugData());
			break;
		case Frame.Headers x:
			var he = new HeaderEncoder();
			var ii = IntStream.builder();
			pl = 0;
			for (var f : x.fields()) {
				switch (f.name()) {
				case ":method", ":scheme", ":status", "content-type":
					pl += he.encode(f, ii);
					break;
				case ":path", "content-length":
//					pl += he.encode(f, ii, false, HeaderField.Representation.NEVER_INDEXED);
					pl += he.encode(f, ii, true, HeaderField.Representation.WITHOUT_INDEXING);
					break;
				case "date": // , "x-api-key":
					pl += he.encode(f, ii, true, HeaderField.Representation.NEVER_INDEXED);
					break;
				default:
//					pl += he.encode(f, ii, true, HeaderField.Representation.WITHOUT_INDEXING);
					pl += he.encode(f, ii, true, HeaderField.Representation.WITH_INDEXING);
					break;
				}
			}
			bb1.putShort((short) ((pl >>> 8) & 0xffff));
			bb1.put((byte) pl);
			bb1.put((byte) Frame.Name.HEADERS.type());
			bb1.put((byte) ((x.endHeaders() ? 0x04 : 0x00) | (x.endStream() ? 0x01 : 0x00)));
			bb1.putInt(x.streamIdentifier());
			bb1.flip();
			bb2 = ByteBuffer.allocate(bb1.remaining() + pl);
			bb2.put(bb1);
			var i = ii.build().iterator();
			for (var j = 0; j < pl; j++)
				bb2.put((byte) i.nextInt());
			break;
//		case Frame.Ping _:
//			throw new RuntimeException();
//		case Frame.Priority _:
//			throw new RuntimeException();
//		case Frame.RstStream _:
//			throw new RuntimeException();
		case Frame.Settings x:
			pl = x.parameters().size() * (Short.BYTES + Integer.BYTES);
			bb1.putShort((short) ((pl >>> 8) & 0xffff));
			bb1.put((byte) pl);
			bb1.put((byte) Frame.Name.SETTINGS.type());
			bb1.put((byte) (x.ack() ? 0x01 : 0x00));
			bb1.putInt(0);
			bb1.flip();
			bb2 = ByteBuffer.allocate(bb1.remaining() + pl);
			bb2.put(bb1);
			for (var y : x.parameters()) {
				bb2.putShort((short) y.name().identifier());
				bb2.putInt(y.value());
			}
			break;
//		case Frame.WindowUpdate _:
//			throw new RuntimeException();
		default:
			throw new RuntimeException();
		}
		return bb2.array();
	}
}

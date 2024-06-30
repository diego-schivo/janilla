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

import java.util.Map;

public record HeadersFrame(boolean endHeaders, boolean endStream, int streamIdentifier, Map<String, String> fields)
		implements Frame {
}
//public class HeadersFrame {
//
//	public static void encode(HeadersFrame frame, WritableByteChannel channel) {
//		var baos = new ByteArrayOutputStream();
//		var bw = new BitsWriter(new ByteWriter(baos));
//		var fe = new FrameEncoder(FrameName.HEADERS, bw);
//		var ff = frame.getFields();
//		if (ff != null && !ff.isEmpty())
//			for (var e : ff.entrySet()) {
//				var hf = new HeaderField(e.getKey(), e.getValue());
//				switch (hf.name()) {
//				case ":method", ":scheme":
//					fe.headerEncoder().encode(hf);
//					break;
//				default:
//					fe.headerEncoder().encode(hf, true, HeaderField.Representation.WITHOUT_INDEXING);
//					break;
//				}
//			}
//		var p = baos.toByteArray();
//
//		baos.reset();
//		fe.encodeLength(p.length);
//		fe.encodeType();
//		fe.encodeFlags(
//				Stream.of(frame.isEndHeaders() ? Flag.END_HEADERS : null, frame.isEndStream() ? Flag.END_STREAM : null)
//						.filter(x -> x != null).collect(Collectors.toSet()));
//		fe.encodeReserved();
//		fe.encodeStreamIdentifier(frame.getStreamIdentifier());
//		try {
//			baos.write(p);
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//
//		try {
//			IO.write(baos.toByteArray(), channel);
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
//	}
//
//	private boolean endHeaders;
//
//	private boolean endStream;
//
//	private int streamIdentifier;
//
//	private Map<String, String> fields;
//
//	public boolean isEndHeaders() {
//		return endHeaders;
//	}
//
//	public void setEndHeaders(boolean endHeaders) {
//		this.endHeaders = endHeaders;
//	}
//
//	public boolean isEndStream() {
//		return endStream;
//	}
//
//	public void setEndStream(boolean endStream) {
//		this.endStream = endStream;
//	}
//
//	public int getStreamIdentifier() {
//		return streamIdentifier;
//	}
//
//	public void setStreamIdentifier(int streamIdentifier) {
//		this.streamIdentifier = streamIdentifier;
//	}
//
//	public Map<String, String> getFields() {
//		return fields;
//	}
//
//	public void setFields(Map<String, String> fields) {
//		this.fields = fields;
//	}
//}

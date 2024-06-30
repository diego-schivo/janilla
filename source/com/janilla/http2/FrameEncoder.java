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

import java.util.Collection;

import com.janilla.hpack.HeaderEncoder;

class FrameEncoder {

	FrameName frame;

	BitsWriter bits;

	HeaderEncoder headerEncoder;

	FrameEncoder(FrameName frame, BitsWriter bits) {
		this.frame = frame;
		this.bits = bits;
		headerEncoder = new HeaderEncoder(bits);
	}

	public HeaderEncoder headerEncoder() {
		return headerEncoder;
	}

	void encodeLength(int length) {
		bits.accept(length, 24);
	}

	void encodeType() {
		bits.accept(frame.type());
	}

	void encodeFlags(Collection<Flag> flags) {
		if (flags == null || flags.isEmpty()) {
			bits.accept(0x00);
			return;
		}
		switch (frame) {
		case DATA:
			bits.accept(0x00, 4);
			bits.accept(flags.contains(Flag.PADDED) ? 0x01 : 0x00, 1);
			bits.accept(0x00, 2);
			bits.accept(flags.contains(Flag.END_STREAM) ? 0x01 : 0x00, 1);
			break;
		case HEADERS:
			bits.accept(0x00, 2);
			bits.accept(flags.contains(Flag.PRIORITY) ? 0x01 : 0x00, 1);
			bits.accept(0x00, 1);
			bits.accept(flags.contains(Flag.PADDED) ? 0x01 : 0x00, 1);
			bits.accept(flags.contains(Flag.END_HEADERS) ? 0x01 : 0x00, 1);
			bits.accept(0x00, 1);
			bits.accept(flags.contains(Flag.END_STREAM) ? 0x01 : 0x00, 1);
			break;
		case SETTINGS:
			bits.accept(0x00, 7);
			bits.accept(flags.contains(Flag.ACK) ? 0x01 : 0x00, 1);
			break;
		case WINDOW_UPDATE:
			bits.accept(0x00);
			break;
		default:
			throw new RuntimeException();
		}
	}

	void encodeReserved() {
		bits.accept(0x00, 1);
	}

	void encodeStreamIdentifier(int identifier) {
		bits.accept(identifier, 31);
	}

	void encodeSetting(Setting setting) {
		bits.accept(setting.name().identifier(), 16);
		bits.accept(setting.value(), 32);
	}

	void encodeWindowSizeIncrement(int increment) {
		bits.accept(increment, 31);
	}
}

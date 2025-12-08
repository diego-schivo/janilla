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

import java.util.Arrays;

public enum FrameName {

	DATA(0x00), HEADERS(0x01), PRIORITY(0x02), RST_STREAM(0x03), SETTINGS(0x04), PUSH_PROMISE(0x05), PING(0x06),
	GOAWAY(0x07), WINDOW_UPDATE(0x08), CONTINUATION(0x09);

	private static final FrameName[] ALL;

	static {
		var t = Arrays.stream(values()).mapToInt(FrameName::type).max().getAsInt();
		ALL = new FrameName[t + 1];
		for (var f : values())
			ALL[f.type()] = f;
	}

	public static FrameName of(int type) {
		return 0 <= type && type < ALL.length ? ALL[type] : null;
	}

	private final int type;

	private FrameName(int type) {
		this.type = type;
	}

	public int type() {
		return type;
	}
}

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

public record Setting(Name name, int value) {

	public enum Name {

		HEADER_TABLE_SIZE(0x01), ENABLE_PUSH(0x02), MAX_CONCURRENT_STREAMS(0x03), INITIAL_WINDOW_SIZE(0x04),
		MAX_FRAME_SIZE(0x05), MAX_HEADER_LIST_SIZE(0x06);

		static Name[] array;

		static {
			var i = Arrays.stream(values()).mapToInt(Name::identifier).max().getAsInt();
			array = new Name[i + 1];
			for (var s : values())
				array[s.identifier()] = s;
		}

		static Name of(int identifier) {
			return 0 <= identifier && identifier < array.length ? array[identifier] : null;
		}

		int identifier;

		Name(int identifier) {
			this.identifier = identifier;
		}

		int identifier() {
			return identifier;
		}
	}

	public record Parameter(Name name, int value) {
	}
}

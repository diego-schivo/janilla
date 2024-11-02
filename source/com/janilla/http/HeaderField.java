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

import java.util.Arrays;
import java.util.Properties;

public record HeaderField(String name, String value) {

	public static HeaderField fromLine(String line) {
		var i = line.indexOf(':');
		return new HeaderField(line.substring(0, i).trim(), line.substring(i + 1).trim());
	}

	public HeaderField withValue(String value) {
		return new HeaderField(name, value);
	}

	public String toLine() {
		return name + ": " + value;
	}

	public ComplexValue toComplexValue() {
		var i = value.indexOf(';');
		var s1 = value.substring(0, i).trim();
		var s2 = value.substring(i + 1).trim();
		var p = Arrays.stream(s2.split(";")).reduce(new Properties(), (x, y) -> {
			var j = y.indexOf('=');
			var k = y.substring(0, j).trim();
			var v = y.substring(j + 1).trim();
			if (v.startsWith("\"") && v.endsWith("\""))
				v = v.substring(1, v.length() - 1);
			x.setProperty(k, v);
			return x;
		}, (x1, x2) -> x1);
		return new ComplexValue(s1, p);
	}

	public record ComplexValue(String value, Properties properties) {
	}

	enum Representation {

		INDEXED(7, (byte) 0x80), WITH_INDEXING(6, (byte) 0x40), WITHOUT_INDEXING(4, (byte) 0x00),
		NEVER_INDEXED(4, (byte) 0x10);

		int prefix;

		byte first;

		Representation(int prefix, byte first) {
			this.prefix = prefix;
			this.first = first;
		}

		public int prefix() {
			return prefix;
		}

		public byte first() {
			return first;
		}
	}
}

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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public record RecordColumn(int type, byte[] content) implements Comparable<RecordColumn> {

	public static RecordColumn of(Object object) {
		if (object == null)
			return new RecordColumn(0, new byte[0]);
		else if (object instanceof Long x) {
			var l = x.longValue();
			if (l == 0)
				return new RecordColumn(8, new byte[0]);
			if (l == 1)
				return new RecordColumn(9, new byte[0]);
			var ni = 0;
			var nn = new int[] { 8, 16, 24, 32, 48, 64 };
			for (; ni < nn.length; ni++) {
				var m = (1l << (nn[ni] - 1)) - 1;
				if ((l & m) == l)
					break;
			}
			var b = ByteBuffer.allocate(Long.BYTES);
			b.putLong(l);
			return new RecordColumn(1 + ni, Arrays.copyOfRange(b.array(), Long.BYTES - nn[ni] / 8, Long.BYTES));
		} else if (object instanceof Double x) {
			var b = ByteBuffer.allocate(Double.BYTES);
			b.putDouble(x);
			return new RecordColumn(7, b.array());
		} else if (object instanceof String s) {
			var bb = s.getBytes();
			return new RecordColumn(bb.length * 2 + 13, bb);
		} else
			throw new RuntimeException(Objects.toString(object) + " (" + object.getClass() + ")");
	}

	public Object toObject() {
		switch (type) {
		case 0:
			return null;
		case 1, 2, 3, 4, 5:
			var b = ByteBuffer.allocate(Long.BYTES);
			b.put(Long.BYTES - content.length, content);
			return b.getLong();
		case 6:
			return ByteBuffer.wrap(content).getLong();
		case 7:
			return ByteBuffer.wrap(content).getDouble();
		case 8:
			return 0l;
		case 9:
			return 1l;
		default:
			if (type >= 12 && type % 2 == 0)
				return content;
			else if (type >= 13 && type % 2 == 1)
				return content.length != 0 ? new String(content) : "";
			else
				throw new RuntimeException();
		}
	}

	@Override
	public int compareTo(RecordColumn column) {
		switch (type) {
		case 0:
			return column.type == 0 ? 0 : -1;

		case 1, 2, 3, 4, 5, 6, 7, 8, 9:
			switch (column.type) {
			case 0:
				return 1;
			case 1, 2, 3, 4, 5, 6, 7, 8, 9:
				var n1 = (Number) toObject();
				var n2 = (Number) column.toObject();
				return (n1 instanceof Double || n2 instanceof Double)
						? Double.compare(n1.doubleValue(), n2.doubleValue())
						: Long.compare(n1.longValue(), n2.longValue());
			default:
				return -1;
			}

		default:
			return Arrays.compare(content, column.content);
		}
	}
}

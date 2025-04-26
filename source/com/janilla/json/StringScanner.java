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
package com.janilla.json;

import java.util.List;

public class StringScanner implements Scanner {

	public static void main(String[] args) {
		var s = """
				{"returnUrl":"https:\\/\\/your-company.com\\/checkout?shopperOrder=12xy.."}""";
		var o = Json.parse(s);
		System.out.println(o);
		var t = Json.format(o);
		System.out.println(t);
		var p = Json.parse(t);
		System.out.println(p);
		assert p.equals(o) : p;
	}

	int state;

	StringBuilder buffer;

	@Override
	public boolean accept(int value, List<JsonToken<?>> tokens) {
		Boolean a = null;
		do {
			var s = state;
			state = switch (s) {

			case 0 -> {
				if (value == '"') {
					buffer = new StringBuilder();
					a = true;
					yield 1;
				}
				a = false;
				yield -1;
			}

			case 1 -> {
				if (value == '\\') {
					a = true;
					yield 2;
				}
				if (value != '"') {
					buffer.append((char) value);
					a = true;
					yield 1;
				}
				tokens.add(new JsonToken<>(JsonToken.Type.STRING, buffer.toString()));
				a = true;
				yield 3;
			}

			case 2 -> {
				switch (value) {
				case '"':
					buffer.append('"');
					a = true;
					yield 1;
				case '\\':
					buffer.append('\\');
					a = true;
					yield 1;
				case 'n':
					buffer.append('\n');
					a = true;
					yield 1;
				case 'r':
					buffer.append('\r');
					a = true;
					yield 1;
				case 't':
					buffer.append('\t');
					a = true;
					yield 1;
				case '/':
					buffer.append('/');
					a = true;
					yield 1;
				default:
					a = false;
					yield -1;
				}
			}

			default -> {
				a = false;
				yield s;
			}
			};

//			System.out.println("StringParser.accept " + value + " " + a + " " + s + " -> " + state);

		} while (a == null);
		return a;
	}

	@Override
	public boolean done() {
		return state == 3;
	}
}

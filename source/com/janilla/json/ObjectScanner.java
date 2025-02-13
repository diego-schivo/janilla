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

public class ObjectScanner implements Scanner {

	int state;

	Scanner scanner;

	@Override
	public boolean accept(int value, List<JsonToken<?>> tokens) {
		Boolean a = null;
		do {
			var s = state;
			state = switch (s) {
			case 0 -> {
				if (value == '{') {
					tokens.add(JsonToken.OBJECT_START);
					a = true;
					yield 1;
				}
				a = false;
				yield -1;
			}

			case 1 -> {
				scanner = new WhitespaceScanner();
				if (scanner.accept(value, tokens)) {
					a = true;
					yield 2;
				}
				yield 3;
			}

			case 2 -> {
				if (scanner.accept(value, tokens))
					a = true;
				if (scanner.done())
					yield 3;
				if (a != null)
					yield 2;
				a = false;
				yield -1;
			}

			case 3 -> {
				if (value == '}') {
					tokens.add(JsonToken.OBJECT_END);
					a = true;
					yield 6;
				}
				tokens.add(JsonToken.MEMBER_START);
				scanner = new StringScanner();
				yield 4;
			}

			case 4 -> {
				if (scanner.accept(value, tokens))
					a = true;
				if (scanner.done()) {
					yield 5;
				}
				if (a == null) {
					a = false;
					yield -1;
				}
				yield 4;
			}

			case 5 -> {
				scanner = new WhitespaceScanner();
				if (scanner.accept(value, tokens)) {
					a = true;
					yield 6;
				}
				yield 7;
			}

			case 6 -> {
				if (scanner.accept(value, tokens))
					a = true;
				if (scanner.done())
					yield 7;
				if (a != null)
					yield 6;
				a = false;
				yield -1;
			}

			case 7 -> {
				if (value == ':') {
					a = true;
					scanner = new ValueScanner();
					yield 8;
				}
				a = false;
				yield -1;
			}

			case 8 -> {
				if (scanner.accept(value, tokens))
					a = true;
				if (scanner.done()) {
					tokens.add(JsonToken.MEMBER_END);
					yield 9;
				}
				if (a == null) {
					a = false;
					yield -1;
				}
				yield 8;
			}

			case 9 -> switch (value) {
			case ',' -> {
				a = true;
				yield 1;
			}
			case '}' -> {
				tokens.add(JsonToken.OBJECT_END);
				a = true;
				yield 10;
			}
			default -> {
				a = false;
				yield -1;
			}
			};

			default -> {
				a = false;
				yield s;
			}
			};

//			System.out.println("ObjectScanner.accept " + value + " " + a + " " + s + " -> " + state);

		} while (a == null);
		return a;
	}

	@Override
	public boolean done() {
		return state == 6 || state == 10;
	}
}

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
import java.util.stream.Stream;

public class ValueScanner implements Scanner {

	private int state;

	private Scanner scanner;

	@Override
	public boolean accept(int value, List<JsonToken<?>> tokens) {
		Boolean a = null;
		do {
			var s = state;
			state = switch (s) {

			case 0 -> {
				tokens.add(JsonToken.VALUE_START);
				scanner = new WhitespaceScanner();
				if (scanner.accept(value, tokens)) {
					a = true;
					yield 1;
				}
				yield 2;
			}

			case 1 -> {
				if (scanner.accept(value, tokens))
					a = true;
				if (scanner.done())
					yield 2;
				if (a != null)
					yield 1;
				a = false;
				yield -1;
			}

			case 2 -> {
				var x = Stream
						.of(new ArrayScanner(), new BooleanScanner(), new NullScanner(), new NumberScanner(),
								new ObjectScanner(), new StringScanner())
						.filter(y -> y.accept(value, tokens)).findFirst();
				if (x.isPresent()) {
					scanner = x.get();
					a = true;
					yield 3;
				}
				a = false;
				yield -1;
			}

			case 3 -> {
				if (scanner.accept(value, tokens))
					a = true;
				if (scanner.done())
					yield 4;
				if (a != null)
					yield 3;
				a = false;
				yield -1;
			}

			case 4 -> {
				scanner = new WhitespaceScanner();
				if (scanner.accept(value, tokens)) {
					a = true;
					yield 5;
				}
				tokens.add(JsonToken.VALUE_END);
				yield 6;
			}

			case 5 -> {
				if (scanner.accept(value, tokens))
					a = true;
				if (scanner.done()) {
					tokens.add(JsonToken.VALUE_END);
					yield 6;
				}
				if (a != null)
					yield 5;
				a = false;
				yield -1;
			}

			default -> {
				a = false;
				yield s;
			}
			};

//			System.out.println("ValueScanner.accept " + value + " " + a + " " + s + " -> " + state);

		} while (a == null);
		return a;
	}

	@Override
	public boolean done() {
		return state == 6;
	}
}

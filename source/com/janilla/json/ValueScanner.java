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
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ValueScanner implements Scanner {

	private int state;

	private Scanner scanner;

	@Override
	public boolean accept(int value, List<JsonToken<?>> tokens) {
		Boolean a = null;
		do {
			var s = state;
			switch (s) {

			case 0:
				tokens.add(JsonToken.VALUE_START);
				scanner = new WhitespaceScanner();
				if (scanner.accept(value, tokens)) {
					a = true;
					state = 1;
				} else
					state = 2;
				break;

			case 1:
				if (scanner.accept(value, tokens))
					a = true;
				if (scanner.done())
					state = 2;
				else if (a != null)
					state = 1;
				else {
					a = false;
					state = -1;
				}
				break;

			case 2: {
				var x = Stream
						.<Supplier<Scanner>>of(ArrayScanner::new, BooleanScanner::new, NullScanner::new,
								NumberScanner::new, ObjectScanner::new, StringScanner::new)
						.map(Supplier::get).filter(y -> {
//							IO.println("value=" + value + ", tokens=" + tokens);
							return y.accept(value, tokens);
						}).findFirst();
				if (x.isPresent()) {
					scanner = x.get();
					a = true;
					state = 3;
				} else {
					a = false;
					state = -1;
				}
			}
				break;

			case 3:
				if (scanner.accept(value, tokens))
					a = true;
				if (scanner.done())
					state = 4;
				else if (a != null)
					state = 3;
				else {
					a = false;
					state = -1;
				}
				break;

			case 4:
				scanner = new WhitespaceScanner();
				if (scanner.accept(value, tokens)) {
					a = true;
					state = 5;
				} else {
					tokens.add(JsonToken.VALUE_END);
					state = 6;
				}
				break;

			case 5:
				if (scanner.accept(value, tokens))
					a = true;
				if (scanner.done()) {
					tokens.add(JsonToken.VALUE_END);
					state = 6;
				} else if (a != null)
					state = 5;
				else {
					a = false;
					state = -1;
				}
				break;

			default:
				a = false;
				break;
			}

//			IO.println("ValueScanner.accept " + value + " " + a + " " + s + " -> " + state);

		} while (a == null);
		return a;
	}

	@Override
	public boolean done() {
		return state == 6;
	}
}

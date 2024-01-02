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
package com.janilla.json;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.janilla.reflect.Parameter;

public class ReflectionJsonIterator extends JsonIterator {

	public static void main(String[] args) {
		var c = new C();
		c.s = "foo";
		var d1 = new D(123);
		c.d = d1;
		var d2 = new D(234);
		var d3 = new D(345);
		c.l = List.of(d2, d3);
		var s = Json.format(new ReflectionJsonIterator(c));
		System.out.println(s);
		var o = Json.parse(s, Json.parseCollector(C.class));
		System.out.println(o);
		assert o.equals(c) : o;
	}

	public static class C {

		private String s;

		private D d;

		private List<D> l;

		public String s() {
			return s;
		}

		public void s(String value) {
			s = value;
		}

		public D d() {
			return d;
		}

		public void d(D value) {
			d = value;
		}

		public List<D> l() {
			return l;
		}

		public void l(List<D> value) {
			l = value;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != C.class)
				return false;
			var c = (C) obj;
			return Objects.equals(s, c.s) && Objects.equals(d, c.d) && Objects.equals(l, c.l);
		}

		@Override
		public String toString() {
			return "C[s=" + s + ", d=" + d + ", l=" + l + "]";
		}
	}

	public record D(@Parameter("i") int i) {
	}

	public ReflectionJsonIterator(Object object) {
		super(object);
	}

	@Override
	public Iterator<JsonToken<?>> newValueIterator(Object object) {
		return new ReflectionValueIterator(object, this);
	}
}

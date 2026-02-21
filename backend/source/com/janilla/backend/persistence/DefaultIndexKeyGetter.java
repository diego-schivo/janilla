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
package com.janilla.backend.persistence;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.janilla.java.Property;

public class DefaultIndexKeyGetter implements IndexKeyGetter {

	protected final Property[] properties;

	public DefaultIndexKeyGetter(Property... properties) {
		this.properties = properties;
	}

	@Override
	public Set<List<Object>> keys(Object object) {
//		IO.println("DefaultIndexKeyGetter.keys, object=" + object);
		var l = Arrays.stream(properties).map(p -> {
//			IO.println("DefaultIndexKeyGetter.keys, p=" + p);
			var o = p.get(object);
//			IO.println("DefaultIndexKeyGetter.keys, o=" + o);
			return o instanceof Collection c ? c.toArray() : new Object[] { o };
		}).toList();
		var ss = l.stream().mapToInt(x -> x.length).toArray();
		var s = Arrays.stream(ss).reduce((x, y) -> x * y).getAsInt();
		var ii = new int[ss.length];
		return IntStream.range(0, s).mapToObj(_ -> {
			var k = IntStream.range(0, ii.length).mapToObj(i -> l.get(i)[ii[i]]).toList();
			for (var i = ii.length - 1; i >= 0; i--) {
				if (++ii[i] != ss[i])
					break;
				ii[i] = 0;
			}
			return k;
		}).collect(Collectors.toCollection(LinkedHashSet::new));
	}
}

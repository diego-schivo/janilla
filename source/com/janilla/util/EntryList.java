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
package com.janilla.util;

import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class EntryList<K, V> extends LinkedList<Entry<K, V>> {

	public static void main(String[] args) {
		var l = new EntryList<String, String>();
		l.add("foo", "bar");
		l.set("foo", "baz");
		System.out.println(l);
		assert l.equals(List.of(Map.entry("foo", "baz"))) : l;
	}

	private static final long serialVersionUID = 2930611377458857452L;

	public void add(K key, V value) {
		add(new SimpleEntry<>(key, value));
	}

	public V get(K key) {
		for (var e : this)
			if (Objects.equals(e.getKey(), key))
				return e.getValue();
		return null;
	}

	public void set(K key, V value) {
		for (var e : this)
			if (Objects.equals(e.getKey(), key)) {
				e.setValue(value);
				return;
			}
		add(key, value);
	}
}

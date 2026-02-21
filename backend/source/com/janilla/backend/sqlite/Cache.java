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

import java.util.LinkedHashMap;

public class Cache<K, V> extends LinkedHashMap<K, V> {

	private static final long serialVersionUID = 2913088298650670193L;

	protected final int capacity;

	public Cache(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public V get(Object key) {
//		IO.println("PageCache.get, key=" + key);

		@SuppressWarnings("unchecked")
		var k = (K) key;
		var v = super.get(k);

		if (v != null) {
			remove(k);
			put(k, v);
//			IO.println("PageCache.get, keys=" + keySet());
		}

		return v;
	}

	@Override
	public V put(K key, V value) {
//		IO.println("PageCache.put, key=" + key);

		if (remove(key) == null && size() == capacity) {
			var kk = keySet().iterator();
			var k = kk.next();
//			IO.println("PageCache.put, k=" + k);
			kk.remove();
			remove(k);
		}

		var v = super.put(key, value);
//		IO.println("PageCache.put, keys=" + keySet());
		return v;
	}
}

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
package com.janilla.net;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RateLimiter<K> {

	private static final Logger LOGGER = System.getLogger(RateLimiter.class.getName());

	protected final int limit;

	protected final int period;

	protected final Map<K, List<Instant>> instants = new HashMap<>();

	protected volatile Instant timeout;

	public RateLimiter(int limit, int period) {
		if (limit <= 0)
			throw new IllegalArgumentException("limit=" + limit);
		if (period <= 0)
			throw new IllegalArgumentException("period=" + period);

		this.limit = limit;
		this.period = period;
	}

	public synchronized boolean test(K key) {
		LOGGER.log(Level.DEBUG, "key={0}", key);

		var n = Instant.now();
		var n0 = n.minusSeconds(period);
		var n1 = n.plusSeconds(period);

		if (timeout == null)
			timeout = n1;
		else if (n.compareTo(timeout) >= 0) {
			removeKeys(n0);
			timeout = n1;
		}

		var l = instants.computeIfAbsent(key, _ -> new LinkedList<>());
		removeValues(l, n0);
		l.add(n);

		var s = l.size();
		LOGGER.log(Level.DEBUG, "size={0} ({1})", s, key);

		var g = s > limit;
		if (g)
			l.removeFirst();

		return !g;
	}

	protected void removeKeys(Instant instant) {
		for (var ee = instants.entrySet().iterator(); ee.hasNext();) {
			var e = ee.next();
			var k = e.getKey();
			var l = e.getValue();

			if (instant.compareTo(l.getLast()) >= 0) {
				LOGGER.log(Level.DEBUG, "key={0}", k);
				ee.remove();
			}
		}
	}

	protected void removeValues(List<Instant> list, Instant instant) {
		for (var ii = list.iterator(); ii.hasNext();) {
			var i = ii.next();
			if (instant.compareTo(i) >= 0)
				ii.remove();
			else
				break;
		}
	}
}

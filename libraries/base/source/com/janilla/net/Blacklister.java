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
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class Blacklister {

	private static final Logger LOGGER = System.getLogger(Blacklister.class.getName());

	protected final Pattern pattern;

	protected final Set<InetAddress> addresses = new HashSet<>();

	public Blacklister(Pattern pattern) {
		Objects.requireNonNull(pattern, "pattern");

		this.pattern = pattern;
	}

	public synchronized boolean test(InetAddress address) {
		return test(address, null);
	}

	public synchronized boolean test(InetAddress address, String line) {
		LOGGER.log(Level.DEBUG, "address={0}, line={1}", address, line);

		Objects.requireNonNull(address, "address");

		var c = addresses.contains(address);

		if (!c && line != null && pattern.matcher(line).find())
			c = addresses.add(address);

		return c;
	}
}

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
package com.janilla.java;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractCopier implements Copier {

	private static final Logger LOGGER = System.getLogger(AbstractCopier.class.getName());

	@Override
	public <T> T copy(Object source, T destination, Predicate<String> filter) {
		LOGGER.log(Level.DEBUG, "source={0}, destination={1}", source, destination);

		if (source instanceof Map<?, ?> m)
			return !m.isEmpty()
					? copy(x -> m.containsKey(x) ? Optional.ofNullable(m.get(x)) : null, destination, filter)
					: destination;

		var c = source.getClass();
		return copy(x -> {
			var p = JavaReflect.property(c, x);
			return p != null ? Optional.ofNullable(p.get(source)) : null;
		}, destination, filter);
	}

	protected abstract <T> T copy(Function<String, Optional<Object>> source, T destination, Predicate<String> filter);
}

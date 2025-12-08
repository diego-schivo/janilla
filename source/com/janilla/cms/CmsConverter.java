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
package com.janilla.cms;

import java.util.Map;

import com.janilla.java.Converter;
import com.janilla.java.TypeResolver;

public class CmsConverter extends Converter {

	public CmsConverter(TypeResolver typeResolver) {
		super(typeResolver);
	}

	@Override
	protected Object convertMap(Map<?, ?> map, Class<?> target, TypeResolver typeResolver) {
		if (target == DocumentReference.class) {
			var d = (Document<?>) super.convertMap(map, null, typeResolver);
			return new DocumentReference<>(d.getClass(), d.id());
		}
		return super.convertMap(map, target, typeResolver);
	}
}

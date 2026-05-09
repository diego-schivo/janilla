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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.janilla.ioc.DiFactory;

public class DefaultCopier extends AbstractCopier {

	private static final Logger LOGGER = System.getLogger(DefaultCopier.class.getName());

	protected final DiFactory diFactory;

	public DefaultCopier() {
		this(null);
	}

	public DefaultCopier(DiFactory diFactory) {
		this.diFactory = diFactory;
	}

	@Override
	public <T> T copy(Function<String, Optional<Object>> source, T destination, Predicate<String> filter) {
		var c = destination.getClass();
		var s = JavaReflect.propertyNames(c);
		if (filter != null)
			s = s.filter(filter);
		var kk = s.toList();
		LOGGER.log(Level.DEBUG, "kk={0}", kk);

		if (kk.isEmpty())
			return destination;

		var vv = kk.stream().map(k -> {
			var v = source.apply(k);
			return v != null ? Java.mapEntry(k, v.orElse(null)) : null;
		}).filter(Objects::nonNull).collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
		LOGGER.log(Level.DEBUG, "vv={0}", vv);

		if (vv.isEmpty())
			return destination;

		T t;
		if (c.isRecord()) {
			var aa = Arrays.stream(c.getRecordComponents()).map(rc -> {
				try {
					var n = rc.getName();
					if (vv.containsKey(n))
						return vv.get(n);

					{
						var p = JavaReflect.property(c, n);
						if (p != null)
							return p.get(destination);
					}

					var m = rc.getAccessor();
					Object o;
					if (JavaReflect.inheritedAnnotation(m, Flat.class) != null
							|| c.getDeclaredField(n).isAnnotationPresent(Flat.class)) {
						Class<?> c2;
						{
							var c2a = m.getReturnType();
							var c2b = diFactory != null ? diFactory.classFor(c2a) : null;
							c2 = c2b != null ? c2b : c2a;
						}
						var aa2 = JavaReflect.properties(c2).filter(x -> !x.derived()).map(x -> {
							var n2 = x.name();
							if (vv.containsKey(n2))
								return vv.get(n2);

							var p2 = JavaReflect.property(c, n2);
							if (p2 != null)
								return p2.get(destination);

							return null;
						}).toArray();

						try {
							o = JavaInvoke.methodHandle(JavaReflect.constructor(c2)).invokeWithArguments(aa2);
						} catch (Throwable e) {
							switch (e) {
							case RuntimeException x:
								throw x;
							default:
								throw new RuntimeException(e);
							}
						}
					} else
						o = m.invoke(destination);
					return o;
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			}).toArray();

			try {
				@SuppressWarnings("unchecked")
				var x = (T) JavaInvoke.methodHandle(JavaReflect.constructor(c)).invokeWithArguments(aa);
				t = x;
			} catch (Throwable e) {
				switch (e) {
				case RuntimeException x:
					throw x;
				default:
					throw new RuntimeException(e);
				}
			}
		} else {
			t = destination;
			JavaReflect.properties(c).filter(x -> vv.containsKey(x.name()) && x.canSet()).forEach(x -> {
//			IO.println("JavaReflect.copy, x=" + x);
				x.set(t, vv.get(x.name()));
			});
		}

		LOGGER.log(Level.DEBUG, "t={0}", t);
		return t;
	}
}

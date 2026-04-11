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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JavaInvoke {

	public static MethodHandle methodHandle(Method method) {
//		IO.println("Reflection.methodHandle, method=" + method);
		class A {
			private static final Map<Method, Object> RESULTS = new ConcurrentHashMap<>();
		}
		var o = A.RESULTS.computeIfAbsent(method, _ -> {
			var l = lookup(method.getDeclaringClass());
			try {
				return l.unreflect(method);
			} catch (IllegalAccessException e) {
				return new RuntimeException(e);
			}
		});
		if (o instanceof RuntimeException e)
			throw e;
		return (MethodHandle) o;
	}

	public static MethodHandle methodHandle(Constructor<?> constructor) {
//		IO.println("Reflection.methodHandle, constructor=" + constructor);
		class A {
			private static final Map<Constructor<?>, Object> RESULTS = new ConcurrentHashMap<>();
		}
		var o = A.RESULTS.computeIfAbsent(constructor, _ -> {
			var l = lookup(constructor.getDeclaringClass());
			try {
				return l.unreflectConstructor(constructor);
			} catch (IllegalAccessException e) {
				return new RuntimeException(e);
			}
		});
		if (o instanceof RuntimeException e)
			throw e;
		return (MethodHandle) o;
	}

	protected static Lookup lookup(Class<?> c) {
		Lookup l;
		if (Modifier.isPublic(c.getModifiers()))
			l = MethodHandles.publicLookup();
		else {
			var m1 = JavaReflect.class.getModule();
			var m2 = c.getModule();
			if (!m1.canRead(m2))
				m1.addReads(m2);
			try {
				l = MethodHandles.privateLookupIn(c, MethodHandles.lookup());
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		return l;
	}
}

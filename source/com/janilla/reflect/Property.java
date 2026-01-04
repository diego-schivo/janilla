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
package com.janilla.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.janilla.java.Java;

public interface Property {

	Member member();

	Class<?> type();

	Type genericType();

	AnnotatedType annotatedType();

	String name();

	Object get(Object object);

	void set(Object object, Object value);

	boolean canGet();

	boolean canSet();

	static Property of(Field field) {
		var n = name(field);
		VarHandle h;
		try {
			h = MethodHandles.publicLookup().unreflectVarHandle(field);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return new Property() {

			@Override
			public Member member() {
				return field;
			}

			@Override
			public Class<?> type() {
				return field.getType();
			}

			@Override
			public Type genericType() {
				return field.getGenericType();
			}

			@Override
			public AnnotatedType annotatedType() {
				return field.getAnnotatedType();
			}

			@Override
			public String name() {
				return n;
			}

			@Override
			public Object get(Object object) {
				if (object == null)
					return null;
				return h.get(object);
			}

			@Override
			public void set(Object object, Object value) {
				h.set(object, value);
			}

			@Override
			public boolean canGet() {
				return true;
			}

			@Override
			public boolean canSet() {
				return !Modifier.isFinal(field.getModifiers());
			}

			@Override
			public String toString() {
				return n;
			}
		};
	}

	static Property of(Class<?> class1, Method getter, Method setter) {
//		IO.println("Property.of, class1=" + class1 + ", getter=" + getter + ", setter=" + setter);
		var n = getter != null ? name(getter) : name(setter);
//		IO.println("Property.of, n=" + n);
		MethodHandle g, s;
		try {
			var l = MethodHandles.publicLookup();
			g = getter != null ? l.unreflect(getter) : null;
			s = setter != null ? l.unreflect(setter) : null;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		var gt0 = getter != null ? getter.getGenericReturnType() : setter.getGenericParameterTypes()[0];
		var dc = getter != null ? getter.getDeclaringClass() : setter.getDeclaringClass();
		var gt = gt0 instanceof TypeVariable v && dc != class1 ? Reflection.resolveTypeVariable(v, class1, dc) : gt0;
		var t = gt != gt0 ? Java.toClass(gt) : getter != null ? getter.getReturnType() : setter.getParameterTypes()[0];
		return new Property() {

			@Override
			public Member member() {
				return getter != null ? getter : setter;
			}

			@Override
			public Class<?> type() {
				return t;
			}

			@Override
			public Type genericType() {
				return gt;
			}

			@Override
			public AnnotatedType annotatedType() {
				return getter != null ? getter.getAnnotatedReturnType() : setter.getAnnotatedParameterTypes()[0];
			}

			@Override
			public String name() {
				return n;
			}

			@Override
			public Object get(Object object) {
				if (object == null)
					return null;
				try {
					return g.invoke(object);
				} catch (Throwable e) {
					throw e instanceof RuntimeException re ? re : new RuntimeException(e);
				}
			}

			@Override
			public void set(Object object, Object value) {
				try {
					s.invoke(object, value);
				} catch (Throwable e) {
					throw e instanceof RuntimeException re ? re : new RuntimeException(e);
				}
			}

			@Override
			public boolean canGet() {
				return g != null;
			}

			@Override
			public boolean canSet() {
				return s != null;
			}

			@Override
			public String toString() {
				return n;
			}
		};
	}

	static Property of(Property property1, Property property2) {
		return new Property() {

			@Override
			public Member member() {
				return property2.member();
			}

			@Override
			public Class<?> type() {
				return property2.type();
			}

			@Override
			public Type genericType() {
				return property2.genericType();
			}

			@Override
			public AnnotatedType annotatedType() {
				return property2.annotatedType();
			}

			@Override
			public String name() {
				return property2.name();
			}

			@Override
			public Object get(Object object) {
				return property2.get(property1.get(object));
			}

			@Override
			public void set(Object object, Object value) {
				property2.set(property1.get(object), value);
			}

			@Override
			public boolean canGet() {
				return property2.canGet();
			}

			@Override
			public boolean canSet() {
				return property2.canSet();
			}
		};
	}

	static String name(Member member) {
		return switch (member) {
		case Field _ -> member.getName();
		case Method n -> {
			var g = n.getReturnType() != Void.TYPE
					&& n.getParameterCount() == (Modifier.isStatic(n.getModifiers()) ? 1 : 0) ? n : null;
			var s = n.getReturnType() == Void.TYPE && n.getParameterCount() == 1 ? n : null;
			if (g == null && s == null)
				yield null;
			var k = n.getName();
			var p = g != null ? (n.getReturnType() == Boolean.TYPE ? "is" : "get") : "set";
			if (k.length() > p.length() && k.startsWith(p) && Character.isUpperCase(k.charAt(p.length()))) {
				var i = p.length();
				do {
					i++;
				} while (i < k.length() && Character.isUpperCase(k.charAt(i)));
				k = k.substring(p.length(), i).toLowerCase() + k.substring(i);
			}
			yield k;
		}
		default -> throw new IllegalArgumentException();
		};
	}
}

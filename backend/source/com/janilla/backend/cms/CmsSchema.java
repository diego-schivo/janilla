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
 * Note that authoring this file involved dealing in other programs that are
 * provided under the following license:
 *
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.backend.cms;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.janilla.cms.Document;
import com.janilla.cms.Types;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Java;
import com.janilla.java.JavaReflect;
import com.janilla.java.TypeResolver;

public class CmsSchema extends LinkedHashMap<String, Object> {

	private static final long serialVersionUID = -8003987634573939042L;

	public CmsSchema(Class<?> dataType, TypeResolver typeResolver, DiFactory diFactory) {
		var q = new ArrayDeque<Type>(List.of(dataType));

		var m1 = new HashMap<String, Object>();

		class A {
			private static final Set<String> skip = Set.of("id", "createdAt", "updatedAt", "documentStatus");
		}

		Function<Type, String> n = x -> {
			if (x instanceof TypeVariable v)
				return v.getName();
			var t = Java.toClass(x);
			if (!Modifier.isPublic(t.getModifiers()))
				t = t.getInterfaces()[0];
			return typeResolver.format(t);
		};

		do {
			var t1 = q.remove();
			var t2 = Java.toClass(t1);

			if (m1.containsKey(n.apply(t2)))
				continue;

			var t3 = diFactory.classFor(t2);
//			IO.println("CmsSchema, t1=" + t1 + ", t2=" + t2 + ", t3=" + t3);

			var i = new int[] { 0 };
			var ii = JavaReflect.properties(t3).collect(Collectors.toMap(x -> x.name(), _ -> i[0]++));

			Object o2;
			if (t3 != null && t3.isEnum())
				o2 = Arrays.stream(t3.getEnumConstants()).map(x -> ((Enum<?>) x).name()).toList();
			else {
				var m2 = new LinkedHashMap<String, Map<String, Object>>();
				JavaReflect.properties(t1).sorted(Comparator.comparingInt(x -> ii.get(x.name())))
						.filter(x -> !A.skip.contains(x.name())).forEach(p -> {
//							var pt1 = p.genericType();
							var pt2 = p.type();
							var pt3 = !pt2.getPackageName().startsWith("java.") ? diFactory.classFor(pt2) : null;
//							IO.println("CmsSchema, pt1=" + pt1 + ", pt2=" + pt2 + ", pt3=" + pt3);

							var m3 = new LinkedHashMap<String, Object>();
							m3.put("type", n.apply(pt2 == Class.class // || (pt3 != null && pt3.isEnum())
									? String.class
									: pt2));

							List<Type> tt;
							if (pt2 == List.class) {
								var et1 = ((ParameterizedType) p.genericType()).getActualTypeArguments()[0];
								var et2 = Java.toClass(et1);
//								IO.println("et1=" + et1 + ", et2=" + et2);

//								var ta = JavaReflect.inheritedMethods((Method) p.member())
//										.map(x -> x.getAnnotatedReturnType() instanceof AnnotatedParameterizedType apt
//												? apt.getAnnotatedActualTypeArguments()[0].getAnnotation(Types.class)
//												: null)
//										.filter(x -> x != null).findFirst().orElse(null);
								var ta = p.annotatedType() instanceof AnnotatedParameterizedType apt
										? apt.getAnnotatedActualTypeArguments()[0].getAnnotation(Types.class)
										: null;

								tt = ta != null ? Arrays.asList(ta.value()) : List.of(et1);
								m3.put("elementTypes", tt.stream().map(n).toList());
								if (Document.class.isAssignableFrom(et2))
									m3.put("referenceType", n.apply(ta != null ? ta.value()[0] : et2));
							} else if (pt2 == Set.class) {
								var c2 = (Class<?>) ((ParameterizedType) p.genericType()).getActualTypeArguments()[0];
								m3.put("elementTypes", List.of(n.apply(c2.isEnum() ? String.class : c2)));
								if (c2.isEnum())
									m3.put("options", Arrays.stream(c2.getEnumConstants())
											.map(x -> ((Enum<?>) x).name()).toList());
								tt = List.of();
							} else if (pt2.getPackageName().startsWith("java."))
								tt = List.of();
							else if (pt2 == Document.class) {
								var ta = JavaReflect.inheritedAnnotation((Method) p.member(), Types.class);
								if (ta != null)
									m3.put("referenceTypes",
											Arrays.stream(ta.value()).map(diFactory::classFor).map(n).toList());
								tt = List.of();
							} else {
								if (Document.class.isAssignableFrom(pt2))
									m3.put("referenceTypes", List.of(n.apply(pt3)));
								tt = !m1.containsKey(n.apply(pt2)) ? List.of(p.genericType()) : List.of();
							}
//							IO.println("CmsSchema, tt=" + tt);

							m2.put(p.name(), m3);
							q.addAll(tt);
						});
				o2 = m2;
			}
			m1.put(n.apply(t2), o2);
		} while (!q.isEmpty());

		m1.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(x -> put(x.getKey(), x.getValue()));
	}
}

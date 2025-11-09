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

import java.lang.reflect.AnnotatedParameterizedType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.janilla.json.JsonToken;
import com.janilla.json.ReflectionJsonIterator;
import com.janilla.json.ReflectionValueIterator;
import com.janilla.json.TokenIterationContext;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;

public class CmsReflectionJsonIterator extends ReflectionJsonIterator {

	protected final Persistence persistence;

	public CmsReflectionJsonIterator(Object object, boolean includeType, Persistence persistence) {
		super(object, includeType);
		this.persistence = persistence;
	}

	@Override
	public Iterator<JsonToken<?>> newValueIterator(Object object) {
		var o = stack().peek();
		if (o instanceof Map.Entry<?, ?> kv) {
			var n = (String) kv.getKey();
			if (object instanceof Long l) {
				o = stack().pop();
				var p = Reflection.property(stack().peek().getClass(), n);
				var ta = p != null ? p.annotatedType().getAnnotation(Types.class) : null;
				var t = ta != null ? ta.value()[0] : null;
				if (t != null) {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					var x = persistence.crud((Class) t).read(l);
					object = o = x;
				}
				stack().push(o);
			} else if (object instanceof DocumentReference<?, ?> r) {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				var x = persistence.crud((Class) r.type()).read(r.id());
				object = x;
			} else if (object instanceof List<?> oo && !oo.isEmpty() && oo.getFirst() instanceof Long) {
				o = stack().pop();
				var p = Reflection.property(stack().peek().getClass(), n);
				var apt = p != null && p.annotatedType() instanceof AnnotatedParameterizedType x ? x : null;
				var ta = apt != null ? apt.getAnnotatedActualTypeArguments()[0].getAnnotation(Types.class) : null;
				var t = ta != null ? ta.value()[0] : null;
				if (t != null) {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					var x = persistence.crud((Class) t).read(oo.stream().toList());
					object = o = list(x);
				}
				stack().push(o);
			}
		}
		return new CustomReflectionValueIterator(this, object);
	}

	protected List<?> list(List<?> list) {
		return list;
	}

	protected class CustomReflectionValueIterator extends ReflectionValueIterator {

		public CustomReflectionValueIterator(TokenIterationContext context, Object object) {
			super(context, object);
		}

		@Override
		protected Iterator<JsonToken<?>> newIterator() {
			return value instanceof Class<?> c
					? context.newStringIterator(c.getName().substring(c.getPackageName().length() + 1))
					: super.newIterator();
		}

		@Override
		protected Stream<Entry<String, Object>> entries(Class<?> class0) {
			var kkvv = super.entries(class0);
			var v = class0.getAnnotation(Versions.class);
			if (v == null || !v.drafts())
				kkvv = kkvv.filter(kv -> kv.getKey() != "documentStatus");
			if (v != null) {
				var n = Version.class.getSimpleName() + "<" + class0.getSimpleName() + ">.documentId";
				var vc = persistence.database()
						.perform(() -> persistence.database().index(n).count(((Document<?>) value).id()), false);
				var kv = Map.entry("versionCount", (Object) vc);
				kkvv = Stream.concat(kkvv, Stream.of(kv));
			}
			return kkvv;
		}
	}
}

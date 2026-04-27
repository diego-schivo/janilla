/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Diego Schivo
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
 */
package com.janilla.conduit.backend;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.janilla.backend.persistence.Persistence;
import com.janilla.http.HttpServer;
import com.janilla.java.JavaReflect;
import com.janilla.json.JsonToken;
import com.janilla.json.ReflectionJsonIterator;

class CustomJsonIterator extends ReflectionJsonIterator {

	protected final Persistence persistence;

	public CustomJsonIterator(Object object, Persistence persistence) {
		super(object);
		this.persistence = persistence;
	}

	@Override
	public Iterator<JsonToken<?>> newValueIterator(Object object) {
		var o = stack().peek();
		if (o instanceof Map.Entry x) {
			var n = (String) x.getKey();
			switch (n) {
			case "article":
				var a = (Article) object;
				if (a.slug() == null)
					object = persistence.crud(Article.class).read(a.id());
				break;
			case "author", "profile":
				var u = (User) object;
				if (u.email() == null)
					object = persistence.crud(User.class).read(u.id());
				break;
			}
		}
		if (object != null)
			switch (object) {
			case Article a: {
				var m = JavaReflect.properties(Article.class).filter(x -> !x.name().equals("id")).map(x -> {
//						IO.println("k=" + k);
					var v = x.get(a);
					return new AbstractMap.SimpleImmutableEntry<>(x.name(), v);
				}).collect(LinkedHashMap::new, (x, y) -> x.put(y.getKey(), y.getValue()), Map::putAll);
				var u = ((HttpExchangeImpl) HttpServer.HTTP_EXCHANGE.get()).getUser();
				m.put("favorited", u != null && a.id() != null && persistence.crud(Article.class)
						.filter("favoriteList", new Object[] { u.id() }).stream().anyMatch(x -> x.equals(a.id())));
				m.put("favoritesCount",
						a.id() != null ? persistence.crud(User.class).count("favoriteList", new Object[] { a.id() })
								: 0);
				object = m;
			}
				break;
			case User u: {
				var m = JavaReflect.properties(User.class).filter(x -> !Set.of("hash", "id", "salt").contains(x.name()))
						.map(x -> {
							var v = x.get(u);
							return new AbstractMap.SimpleImmutableEntry<>(x.name(), v);
						}).collect(LinkedHashMap::new, (x, y) -> x.put(y.getKey(), y.getValue()), Map::putAll);
				var v = ((HttpExchangeImpl) HttpServer.HTTP_EXCHANGE.get()).getUser();
				m.put("following", v != null && persistence.crud(User.class)
						.filter("followList", new Object[] { v.id() }).stream().anyMatch(x -> x.equals(u.id())));
				object = m;
			}
				break;
			default:
				break;
			}
		return super.newValueIterator(object);
	}
}

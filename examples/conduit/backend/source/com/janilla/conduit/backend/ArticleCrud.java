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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.janilla.backend.persistence.DefaultCrud;
import com.janilla.backend.persistence.Persistence;

class ArticleCrud extends DefaultCrud<Long, Article> {

	public ArticleCrud(Persistence persistence) {
		super(Article.class, persistence.idConverter(Article.class), persistence);
	}

	public boolean favorite(Long id, Instant createdAt, Long user) {
		return persistence.database().perform(() -> {
			var x = persistence.database().index("User.favoriteList")
					.insert(Stream.of(id, createdAt, user).map(this::toDatabaseValue).toArray(), null);
			persistence.database().index("Article.favoriteList")
					.insert(Stream.of(user, createdAt, id).map(this::toDatabaseValue).toArray(), null);
			return x;
		}, true);
	}

	public boolean unfavorite(Long id, Instant createdAt, Long user) {
		return persistence.database().perform(() -> {
			var x = persistence.database().index("User.favoriteList")
					.delete(Stream.of(id, createdAt, user).map(this::toDatabaseValue).toArray(), null);
			persistence.database().index("Article.favoriteList")
					.delete(Stream.of(user, createdAt, id).map(this::toDatabaseValue).toArray(), null);
			return x;
		}, true);
	}

	@Override
	protected void updateIndexes(Article entity1, Article entity2) {
		super.updateIndexes(entity1, entity2);

		var dd = Stream.of(entity1, entity2).filter(Objects::nonNull).flatMap(x -> x.tagList().stream())
				.collect(Collectors.toMap(x -> x, _ -> 0, (x, _) -> x, LinkedHashMap::new));
		if (entity1 != null)
			for (var t : entity1.tagList())
				dd.compute(t, (_, x) -> x - 1);
		if (entity2 != null)
			for (var t : entity2.tagList())
				dd.compute(t, (_, x) -> x + 1);

		var i1 = persistence.database().index("TagCount", "table");
		var i2 = persistence.database().index("CountTag", "table");
		dd.entrySet().forEach(td -> {
			var t = td.getKey();
			var d = td.getValue().intValue();
			if (d != 0) {
				var k = new Object[] { toDatabaseValue(t) };
				var c = new int[2];
				i1.delete(k, x -> x.forEach(y -> c[0] = fromDatabaseValue(y.reduce((_, z) -> z).get(), Integer.class)));
				c[1] = c[0] + d;
				var v = new Object[] { toDatabaseValue(c[1]) };
				i1.insert(k, v);

				i2.delete(new Object[] { toDatabaseValue(c[0]), k[0] }, null);
				i2.insert(new Object[] { v[0], k[0] }, null);
			}
		});
	}
}

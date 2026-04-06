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

import java.util.List;

import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.backend.sqlite.TableColumn;
import com.janilla.java.Converter;
import com.janilla.persistence.Entity;

public class CustomPersistence extends Persistence {

	public CustomPersistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables,
//			TypeResolver typeResolver,
			Converter converter) {
		super(database, storables, converter);
	}

	@Override
	protected void createStoresAndIndexes() {
		database.perform(() -> {
			super.createStoresAndIndexes();

			database.createTable("Article.favoriteList",
					new TableColumn[] { new TableColumn("user", "NUMERIC", false),
							new TableColumn("createdAt", "TEXT", false), new TableColumn("id", "NUMERIC", false) },
					true);

			database.createTable("User.favoriteList",
					new TableColumn[] { new TableColumn("id", "NUMERIC", false),
							new TableColumn("createdAt", "TEXT", false), new TableColumn("user", "NUMERIC", false) },
					true);

			database.createTable("User.followList", new TableColumn[] { new TableColumn("user", "NUMERIC", false),
					new TableColumn("profile", "NUMERIC", false) }, true);

			database.createTable("TagCount", new TableColumn[] { new TableColumn("tag", "TEXT", false),
					new TableColumn("count", "NUMERIC", false) }, true);

			database.createTable("CountTag", new TableColumn[] { new TableColumn("count", "NUMERIC", false),
					new TableColumn("tag", "TEXT", false) }, true);

			return null;
		}, true);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		if (type == Article.class)
			return (Crud<?, E>) new ArticleCrud(this);
		if (type == User.class)
			return (Crud<?, E>) new UserCrud(this);
		return super.newCrud(type);
	}
}

/*
 * MIT License
 *
 * Copyright (c) 2024 Vercel, Inc.
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
package com.janilla.acmedashboard.backend;

import java.util.List;

import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.DefaultPersistence;
import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.backend.sqlite.TableColumn;
import com.janilla.java.Converter;
import com.janilla.persistence.Entity;

class CustomPersistence extends DefaultPersistence {

	public CustomPersistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables,
//			TypeResolver typeResolver,
			Converter converter) {
		super(database, storables, converter);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		return type == Invoice.class ? (Crud<?, E>) new InvoiceCrud(this) : super.newCrud(type);
	}

	@Override
	protected void createStoresAndIndexes() {
		database.perform(() -> {
			super.createStoresAndIndexes();

			database.createTable("StatusAmount", new TableColumn[] { new TableColumn("status", "TEXT", false),
					new TableColumn("amount", "NUMERIC", false) }, true);

			database.createTable("CustomerStatusAmount",
					new TableColumn[] { new TableColumn("customer", "TEXT", false),
							new TableColumn("status", "TEXT", false), new TableColumn("amount", "NUMERIC", false) },
					true);

			return null;
		}, true);
	}
}

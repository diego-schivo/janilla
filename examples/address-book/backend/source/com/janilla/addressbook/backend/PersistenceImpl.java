/*
 * MIT License
 *
 * Copyright (c) React Training LLC 2015-2019
 * Copyright (c) Remix Software Inc. 2020-2021
 * Copyright (c) Shopify Inc. 2022-2023
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
package com.janilla.addressbook.backend;

import java.util.List;

import com.janilla.addressbook.Contact;
import com.janilla.backend.persistence.IdHelper;
import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.blanktemplate.backend.BlankPersistence;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Copier;
import com.janilla.persistence.Entity;

class PersistenceImpl extends BlankPersistence<AddressBookBackendConfig> {

	PersistenceImpl(SqliteDatabase database, List<Class<? extends Entity<?>>> storables, DiFactory diFactory,
			AddressBookBackendConfig config, Class<?> seedDataClass, Copier copier) {
		super(database, storables, diFactory, config, seedDataClass, copier);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ID extends Comparable<ID>> IdHelper<ID> idHelper(Class<?> type) {
		return type == Contact.class ? (IdHelper<ID>) new ContactIdHelper() : super.idHelper(type);
	}

}

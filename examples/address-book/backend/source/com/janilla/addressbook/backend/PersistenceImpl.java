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

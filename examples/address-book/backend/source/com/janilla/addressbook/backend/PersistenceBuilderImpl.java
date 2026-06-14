package com.janilla.addressbook.backend;

import java.nio.file.Files;
import java.nio.file.Path;

import com.janilla.backend.web.BackendConfig;
import com.janilla.blanktemplate.backend.BlankPersistenceBuilder;

class PersistenceBuilderImpl extends BlankPersistenceBuilder {

	PersistenceBuilderImpl(Path databaseFile, BackendConfig config) {
		super(databaseFile, config);
	}

	@Override
	protected boolean seed() {
		return !Files.exists(databaseFile);
	}
}

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
package com.janilla.backend.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.backend.sqlite.TransactionalByteChannel;
import com.janilla.ioc.DiFactory;
import com.janilla.java.TypeResolver;

public class PersistenceBuilder {

	protected final Path databaseFile;

	public PersistenceBuilder(Path databaseFile) {
		this.databaseFile = databaseFile;
	}

	public Persistence build(List<Class<? extends Entity<?>>> storables, TypeResolver typeResolver) {
		var d = createDatabase();
		return new Persistence(d, storables, typeResolver);
	}

	public Persistence build(DiFactory diFactory) {
		var d = createDatabase();
		return diFactory.create(Persistence.class, Map.of("database", d));
	}

	protected SqliteDatabase createDatabase() {
		TransactionalByteChannel ch;
		try {
			var d = Files.createDirectories(databaseFile.getParent());
			var f1 = d.resolve(databaseFile.getFileName());
			var f2 = d.resolve(databaseFile.getFileName() + ".transaction");
			ch = new TransactionalByteChannel(
					FileChannel.open(f1, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE),
					FileChannel.open(f2, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return new SqliteDatabase(ch, 4096, 0);
	}
}

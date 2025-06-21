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
package com.janilla.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import com.janilla.database.BTree;
import com.janilla.database.BTreeMemory;
import com.janilla.database.BlockReference;
import com.janilla.database.Database;
import com.janilla.database.IdAndReference;
import com.janilla.database.KeyAndData;
import com.janilla.database.Store;
import com.janilla.io.ByteConverter;
import com.janilla.io.TransactionalByteChannel;
import com.janilla.reflect.Factory;

public class ApplicationPersistenceBuilder {

	protected final Path databaseFile;

	protected final Factory factory;

	protected Persistence persistence;

	public ApplicationPersistenceBuilder(Path databaseFile, Factory factory) {
		this.databaseFile = databaseFile;
		this.factory = factory;
	}

	public Persistence build() {
		try {
			var bto = 100;
			TransactionalByteChannel ch;
			{
				var d = Files.createDirectories(databaseFile.getParent());
				var f = d.resolve(databaseFile.getFileName());
				var tf = d.resolve(databaseFile.getFileName() + ".transaction");
				ch = new TransactionalByteChannel(
						FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
								StandardOpenOption.WRITE),
						FileChannel.open(tf, StandardOpenOption.CREATE, StandardOpenOption.READ,
								StandardOpenOption.WRITE));
			}
			var m = new BTreeMemory(bto, ch, BlockReference.read(ch), Math.max(3 * BlockReference.BYTES, ch.size()));
			var d = new Database(bto, ch, m, BlockReference.BYTES, 2 * BlockReference.BYTES,
					x -> newStore(bto, ch, m, x), x -> persistence.newIndex(x));
			persistence = factory.create(Persistence.class, Map.of("database", d));
			return persistence;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected <ID extends Comparable<ID>> Store<ID, String> newStore(int bTreeOrder, TransactionalByteChannel channel,
			BTreeMemory memory, KeyAndData<String> keyAndData) {
		@SuppressWarnings("unchecked")
		var s = (Store<ID, String>) new Store<>(new BTree<>(bTreeOrder, channel, memory,
				IdAndReference.byteConverter(ByteConverter.LONG), keyAndData.bTree()), ByteConverter.STRING);
//				});
		return s;
	}
}

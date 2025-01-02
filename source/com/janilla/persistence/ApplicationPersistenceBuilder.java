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

import com.janilla.database.BlockReference;
import com.janilla.database.Database;
import com.janilla.database.Memory;
import com.janilla.database.Store;
import com.janilla.io.ElementHelper;
import com.janilla.io.TransactionalByteChannel;
import com.janilla.reflect.Factory;

public class ApplicationPersistenceBuilder {

	protected Path file;

	protected int order = 100;

	protected Factory factory;

	public void setFile(Path file) {
		this.file = file;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public void setFactory(Factory factory) {
		this.factory = factory;
	}

	public Persistence build() {
		try {
			TransactionalByteChannel ch;
			{
				var d = Files.createDirectories(file.getParent());
				var f = d.resolve(file.getFileName());
				var tf = d.resolve(file.getFileName() + ".transaction");
				ch = new TransactionalByteChannel(
						FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ,
								StandardOpenOption.WRITE),
						FileChannel.open(tf, StandardOpenOption.CREATE, StandardOpenOption.READ,
								StandardOpenOption.WRITE));
			}

			var m = new Memory();
			{
				var t = m.getFreeBTree();
				t.setChannel(ch);
				t.setOrder(order);
				t.setRoot(BlockReference.read(ch, 0));
				m.setAppendPosition(Math.max(3 * BlockReference.BYTES, ch.size()));
			}

			var d = new Database();
			d.setBTreeOrder(order);
			d.setChannel(ch);
			d.setMemory(m);
			d.setStoresRoot(BlockReference.BYTES);
			d.setIndexesRoot(2 * BlockReference.BYTES);

			var p = factory.create(Persistence.class);
			p.database = d;
			p.configuration = new Persistence.Configuration();
			for (var t : factory.getTypes())
				p.configure(t);

			d.setInitializeStore((_, x) -> {
				@SuppressWarnings("unchecked")
				var s = (Store<String>) x;
				s.setElementHelper(ElementHelper.STRING);
				s.setIdSupplier(() -> {
					var v = x.getAttributes().get("nextId");
					var id = v != null ? (Long) v : 1L;
					x.getAttributes().put("nextId", id + 1);
					return id;
				});
			});
			d.setInitializeIndex((n, x) -> {
				if (p.initializeIndex(n, x))
					return;
			});

			p.createStoresAndIndexes();

			p.setTypeResolver(x -> {
				try {
					return Class.forName(getClass().getPackageName() + "." + x.replace('.', '$'));
				} catch (ClassNotFoundException f) {
					throw new RuntimeException(f);
				}
			});

			return p;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

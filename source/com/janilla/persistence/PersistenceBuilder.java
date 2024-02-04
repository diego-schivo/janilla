/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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
//package com.janilla.persistence;
//
//import java.io.IOException;
//import java.nio.channels.FileChannel;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.StandardOpenOption;
//import java.util.function.Supplier;
//
//import com.janilla.database.Database;
//import com.janilla.database.Memory;
//import com.janilla.database.Memory.BlockReference;
//import com.janilla.database.Store;
//import com.janilla.io.ElementHelper;
//import com.janilla.io.TransactionalByteChannel;
//import com.janilla.persistence.Persistence.Configuration;
//
//public class PersistenceBuilder {
//
//	private Path file;
//
//	private int order = 100;
//
//	private Iterable<Class<?>> types;
//
//	private Supplier<Persistence> persistence;
//
//	public void setFile(Path file) {
//		this.file = file;
//	}
//
//	public void setOrder(int order) {
//		this.order = order;
//	}
//
//	public void setTypes(Iterable<Class<?>> types) {
//		this.types = types;
//	}
//
//	public void setPersistence(Supplier<Persistence> persistence) {
//		this.persistence = persistence;
//	}
//
//	public Persistence build() throws IOException {
//		TransactionalByteChannel c;
//		{
//			var d = Files.createDirectories(file.getParent());
//			var f1 = d.resolve(file.getFileName());
//			var f2 = d.resolve(file.getFileName() + ".transaction");
//			c = new TransactionalByteChannel(
//					FileChannel.open(f1, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE),
//					FileChannel.open(f2, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE));
//		}
//
//		var m = new Memory();
//		{
//			var t = m.getFreeBTree();
//			t.setChannel(c);
//			t.setOrder(order);
//			t.setRoot(BlockReference.read(c, 0));
//			m.setAppendPosition(Math.max(3 * BlockReference.HELPER_LENGTH, c.size()));
//		}
//
//		var d = new Database();
//		d.setBTreeOrder(order);
//		d.setChannel(c);
//		d.setMemory(m);
//		d.setStoresRoot(BlockReference.HELPER_LENGTH);
//		d.setIndexesRoot(2 * BlockReference.HELPER_LENGTH);
//
//		var p = persistence != null ? persistence.get() : new Persistence();
//		p.database = d;
//		p.configuration = new Configuration();
//		for (var t : types)
//			p.configure(t);
//
//		d.setInitializeStore((n, s) -> {
//			@SuppressWarnings("unchecked")
//			var u = (Store<String>) s;
//			u.setElementHelper(ElementHelper.STRING);
//		});
//		d.setInitializeIndex((n, i) -> {
//			if (p.initializeIndex(n, i))
//				return;
//		});
//
//		p.createStoresAndIndexes();
//		return p;
//	}
//}

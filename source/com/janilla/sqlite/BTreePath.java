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
package com.janilla.sqlite;

import java.util.ArrayList;
import java.util.Collection;

public class BTreePath extends ArrayList<BTreePosition> {

	private static final long serialVersionUID = 8714727091836519387L;

	protected final BTree<?, ?> bTree;

	public BTreePath(BTree<?, ?> bTree) {
		this.bTree = bTree;
	}

	public BTreePath(BTree<?, ?> bTree, Collection<BTreePosition> path) {
		this.bTree = bTree;
		super(path);
	}

	public long pointer(int index) {
		if (index == 0)
			return bTree.rootNumber();
		var pi = get(index - 1);
		var p = (InteriorPage<?>) pi.page();
		return p.childPointer(pi.index());
	}

	public boolean next() {
		long n;
		if (isEmpty())
			n = bTree.rootNumber();
		else
			for (;;) {
				var pi = removeLast();
				var i = pi.index() + 1;
				if (i < pi.page().getCellCount() + (pi.page() instanceof InteriorPage ? 1 : 0)) {
					pi = new BTreePosition(pi.page(), i);
					add(pi);
					n = pi.page() instanceof InteriorPage<?> x
							? pi.index() == x.getCellCount() ? x.getRightMostPointer()
									: ((InteriorCell) pi.cell()).leftChildPointer()
							: 0;
					break;
				} else if (isEmpty())
					return false;
				else {
					pi = getLast();
					if (pi.index() < pi.page().getCellCount())
						return true;
				}
			}

		while (n != 0) {
			var p = BTreePage.read(n, bTree.database);
			add(new BTreePosition(p, 0));
			n = p instanceof InteriorPage<?> x
					? x.getCellCount() != 0 ? x.getCells().getFirst().leftChildPointer() : x.getRightMostPointer()
					: 0;
		}

		return true;
	}
}

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
 * Note that authoring this file involved dealing in other programs that are
 * provided under the following license:
 *
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
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
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.cms;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.janilla.persistence.Crud;
import com.janilla.persistence.IdHelper;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Reflection;

public class DocumentCrud<ID extends Comparable<ID>, D extends Document<ID>> extends Crud<ID, D> {

	protected final String versionTable;

	public DocumentCrud(Class<D> type, IdHelper<ID> idHelper, Persistence persistence) {
		super(type, idHelper, persistence);
		versionTable = type.isAnnotationPresent(Versions.class)
				? Version.class.getSimpleName() + "<" + type.getSimpleName() + ">"
				: null;
	}

	@Override
	public D create(D document) {
//		IO.println("DocumentCrud.create, document=" + document);
		return versionTable != null ? persistence.database().perform(() -> {
			var d = super.create(document);
			class A {
				Version<ID, D> v;
			}
			var a = new A();
			persistence.database().table(versionTable).insert(x -> {
				@SuppressWarnings("unchecked")
				var id = (ID) Long.valueOf(x);
				a.v = new Version<>(id, d);
				return new Object[] { x, format(a.v) };
			});
//			IO.println("DocumentCrud.create, a.v=" + a.v);
			updateVersionIndexes(null, List.of(a.v), d.id());
			return d;
		}, true) : super.create(document);
	}

	public D read(ID id, boolean drafts) {
//		IO.println("DocumentCrud.read, id=" + id + ", drafts=" + drafts);
		if (id == null)
			return null;
		return versionTable != null && drafts ? persistence.database().perform(() -> {
			var d = read(id);
			// IO.println("d=" + d);
			if (d != null) {
				var i = persistence.database().index(versionTable + ".documentId");
				class A {
					ID id;
				}
				var a = new A();
				i.select(new Object[] { id }, x -> {
					var oo = x.reduce((_, y) -> y).get();
//					IO.println("oo=" + Arrays.toString(oo));
					@SuppressWarnings("unchecked")
					var o = (ID) oo[1];
					a.id = o;
				});
				var v = readVersion(a.id);
//				IO.println("DocumentCrud.read, v=" + v);
				if (v != null && v.document().updatedAt().isAfter(d.updatedAt()))
					d = v.document();
			}
			return d;
		}, false) : read(id);
	}

	public List<D> read(List<ID> ids, boolean drafts) {
//		IO.println("DocumentCrud.read, ids=" + ids + ", drafts=" + drafts);
		if (ids == null || ids.isEmpty())
			return List.of();
		return versionTable != null && drafts
				? persistence.database().perform(() -> ids.stream().map(x -> read(x, true)).toList(), false)
				: read(ids);
	}

	public D update(ID id, D document, Set<String> include, boolean newVersion) {
//		IO.println("DocumentCrud.update, id=" + id + ", document=" + document + ", include=" + include + ", newVersion"
//				+ newVersion);
		var exclude = Set.of("id", "createdAt", "updatedAt");
		return versionTable != null ? persistence.database().perform(() -> {
			class A {
				D d1 = read(id, true);
				D d2;
				boolean nv = newVersion;
				ID id2;
				Version<ID, D> v1;
				Version<ID, D> v2;
			}
			var a = new A();
			switch (document.documentStatus()) {
			case DRAFT:
				a.d2 = Reflection.copy(document, a.d1,
						x -> (include == null || include.contains(x)) && !exclude.contains(x));
				a.d2 = Reflection.copy(Map.of("updatedAt", Instant.now()), a.d2);
				if (a.d2.documentStatus() != a.d1.documentStatus())
					a.nv = true;
				break;
			case PUBLISHED:
				a.d2 = update(id, x -> {
					var d2 = Reflection.copy(document, a.d1,
							y -> (include == null || include.contains(y)) && !exclude.contains(y));
					if (d2.documentStatus() != x.documentStatus())
						a.nv = true;
					return d2;
				});
				break;
			default:
				throw new RuntimeException();
			}

			if (a.nv)
				persistence.database().table(versionTable).insert(x -> {
					@SuppressWarnings("unchecked")
					var id2 = (ID) Long.valueOf(x);
					a.v2 = new Version<>(id2, a.d2);
					return new Object[] { x, format(a.v2) };
				});
			else {
				var i = persistence.database().index(versionTable + ".documentId");
				i.select(new Object[] { id }, x -> {
					var oo = x.reduce((_, y) -> y).get();
					@SuppressWarnings("unchecked")
					var o = (ID) oo[1];
					a.id2 = o;
				});
				persistence.database().table(versionTable).delete(new Object[] { a.id2 }, x -> {
					var oo = x.reduce((_, y) -> y).get();
					IO.println("oo=" + Arrays.toString(oo));
					@SuppressWarnings("unchecked")
					var v1 = (Version<ID, D>) parse((String) oo[1], Version.class);
					a.v1 = v1;
				});
				persistence.database().table(versionTable).insert(_ -> {
					a.v2 = new Version<>(a.id2, a.d2);
					return new Object[] { a.id2, format(a.v2) };
				});
			}
//			IO.println("DocumentCrud.update, a.v2=" + a.v2);
			updateVersionIndexes(a.v1 != null ? List.of(a.v1) : null, List.of(a.v2), id);
			return a.d2;
		}, true)
				: update(id, x -> Reflection.copy(document, x,
						y -> (include == null || include.contains(y)) && !exclude.contains(y)));
	}

	@Override
	public D delete(ID id) {
		return versionTable != null ? persistence.database().perform(() -> {
			var d = super.delete(id);
			var ids = new ArrayList<ID>();
			persistence.database().index(versionTable + ".documentId").select(new Object[] { id }, x -> x.map(oo -> {
				@SuppressWarnings("unchecked")
				var o = (ID) oo[1];
				return o;
			}).forEach(ids::add));
			var t = persistence.database().table(versionTable);
			var vv = new ArrayList<Version<ID, D>>(ids.size());
			ids.forEach(x -> t.delete(new Object[] { x }, y -> {
				var oo = y.findFirst().get();
				@SuppressWarnings("unchecked")
				var v = (Version<ID, D>) parse((String) oo[1], Version.class);
				vv.add(v);
			}));
			updateVersionIndexes(vv, null, id);
			return d;
		}, true) : super.delete(id);
	}

	public List<D> patch(List<ID> ids, D document, Set<String> include) {
		if (ids == null || ids.isEmpty())
			return List.of();
		return persistence.database().perform(
				() -> ids.stream().map(x -> update(x, document, include, versionTable != null)).toList(), true);
	}

	public List<Version<ID, D>> readVersions(ID id) {
		return persistence.database().perform(() -> {
			var ids = new ArrayList<ID>();
			persistence.database().index(versionTable + ".documentId").select(new Object[] { id }, x -> x.map(oo -> {
				@SuppressWarnings("unchecked")
				var y = (ID) oo[1];
				return y;
			}).forEach(ids::add));
			var vv = new ArrayList<Version<ID, D>>(ids.size());
			ids.forEach(x -> persistence.database().table(versionTable).select(new Object[] { x }, y -> {
				var oo = y.findFirst().get();
				@SuppressWarnings("unchecked")
				var v = (Version<ID, D>) parse((String) oo[1], Version.class);
				vv.add(v);
			}));
			return vv;
		}, false);
	}

	public Version<ID, D> readVersion(ID versionId) {
		return persistence.database().perform(() -> {
			class A {
				Version<ID, D> v;
			}
			var a = new A();
			persistence.database().table(versionTable).select(new Object[] { versionId }, x -> {
				var oo = x.findFirst().get();
//				IO.println("oo=" + Arrays.toString(oo));
				@SuppressWarnings("unchecked")
				var v = (Version<ID, D>) parse((String) oo[1], Version.class);
				a.v = v;
			});
			return a.v;
		}, false);
	}

	public D restoreVersion(ID versionId, DocumentStatus status) {
		return persistence.database().perform(() -> {
			var v1 = readVersion(versionId);
			var d = update(v1.document().id(), _ -> {
				var x = v1.document();
				if (status != x.documentStatus())
					x = Reflection.copy(Map.of("documentStatus", status), x);
				return x;
			});
			persistence.database().index(versionTable + ".documentId")
					.insert(new Object[] { d.id(), persistence.database().table(versionTable).insert(x -> {
						@SuppressWarnings("unchecked")
						var id = (ID) Long.valueOf(x);
						var v = new Version<>(id, d);
						return new Object[] { x, format(v) };
					}) }, null);
			return d;
		}, true);
	}

	protected void updateVersionIndexes(List<Version<ID, D>> vv1, List<Version<ID, D>> vv2, ID id) {
		class A {
			D d1;
			D d2;
		}
		var a = new A();
		var i = persistence.database().index(versionTable + ".documentId");
		if (vv1 != null)
			for (var v : vv1) {
				i.delete(new Object[] { id, v.id() }, null);
				a.d1 = v.document();
			}
		if (vv2 != null)
			for (var v : vv2) {
				i.insert(new Object[] { id, v.id() }, null);
				a.d2 = v.document();
			}
		updateIndexes(a.d1, a.d2, id,
				x -> persistence.database().index(type.getSimpleName() + "." + x + "Draft", "table"));
	}
}

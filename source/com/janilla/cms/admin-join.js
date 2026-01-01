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
import WebComponent from "web-component";

export default class AdminJoinField extends WebComponent {

	static get templateNames() {
		return ["admin-join"];
	}

	static get observedAttributes() {
		return ["data-array-key", "data-path", "data-updated-at"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("open-drawer", this.handleOpenDrawer);
		this.addEventListener("close-drawer", this.handleCloseDrawer);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("open-drawer", this.handleOpenDrawer);
		this.removeEventListener("close-drawer", this.handleCloseDrawer);
	}

	async updateDisplay() {
		const p = this.dataset.path;
		const s = this.state;
		s.field = this.closest("admin-edit").field(p);
		const a = this.closest("admin-element");

		s.slug = Object.entries(a.state.schema["Collections"])
			.find(([_, v]) => v.elementTypes[0] === s.field.referenceType)[0]
			.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-");
		const hh = a.headers(s.slug);

		this.appendChild(this.interpolateDom({
			$template: "",
			table: {
				$template: "table",
				headRow: {
					$template: "table-row",
					cells: hh.map(x => ({
						$template: "table-header-cell",
						content: x
					}))
				},
				bodyRows: s.field.data?.map(x => ({
					$template: "table-row",
					cells: hh.map(y => a.cell(x, y)).map(y => ({
						$template: "table-data-cell",
						content: typeof y === "object" ? {
							path: p,
							...y
						} : y
					}))
				}))
			},
			drawer: this.state.drawer ? {
				$template: "drawer",
				...this.state.drawer
			} : null
		}));
	}

	handleOpenDrawer = event => {
		const s = this.state;
		s.drawer = {
			slug: s.slug,
			id: event.detail.id
		};
		this.requestDisplay();
	}

	handleCloseDrawer = async () => {
		await this.closest("admin-edit").reloadFieldData(this.dataset.path);
		delete this.state.drawer;
		this.requestDisplay();
	}
}

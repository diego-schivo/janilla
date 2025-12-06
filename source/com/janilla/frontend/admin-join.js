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
import WebComponent from "./web-component.js";

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

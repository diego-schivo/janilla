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

export default class AdminRelationshipField extends WebComponent {

	static get templateNames() {
		return ["admin-relationship"];
	}

	static get observedAttributes() {
		return ["data-array-key", "data-path", "data-updated-at"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("change", this.handleChange);
		this.addEventListener("click", this.handleClick);
		this.addEventListener("close-drawer", this.handleCloseDrawer);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("click", this.handleClick);
		this.removeEventListener("close-drawer", this.handleCloseDrawer);
	}

	async updateDisplay() {
		const p = this.dataset.path;
		if (["variantOptions", "variants", "cart.items"].includes(p))
			return;
		const s = this.state;
		s.field ??= this.closest("admin-edit").field(p);
		s.complex ??= s.field.type === "DocumentReference";
		const m = s.field.type === "List";
		const a = this.closest("admin-element");
		s.data ??= Array.isArray(s.field.data) ? s.field.data : s.field.data ? [s.field.data] : [];
		this.appendChild(this.interpolateDom({
			$template: "",
			values: s.data.map(x => ({
				$template: "value",
				label: a.title(x),
				id: x.id
			})),
			options: s.options ? Object.values(s.options)[0].filter(x => !s.data?.some(y => y.id === x.id)).map(x => ({
				$template: "option",
				value: x.id,
				text: a.title(x)
			})) : null,
			hiddens: s.data?.map(x => ({
				$template: "hidden",
				name: p,
				value: x.id
			})),
			drawer: s.drawer ? {
				$template: "drawer",
				...s.drawer
			} : null
		}));
	}

	handleChange = event => {
		const el = event.target.closest("select");
		if (el) {
			const s = this.state;
			s.data = Object.values(s.options)[0].filter(x => x.id == el.value || s.data?.some(y => y.id === x.id));
			el.selectedIndex = 0;
			this.requestDisplay();
		}
	}

	handleClick = async event => {
		const a = this.closest("admin-element");
		const s = this.state;
		let el = event.target.closest("button");
		switch (el?.name) {
			case "add-new": {
				const t = s.field.referenceTypes?.[0] ?? s.field.referenceType;
				s.drawer = {
					slug: Object.entries(a.state.schema["Collections"])
						.find(([_, v]) => v.elementTypes[0] === t)[0]
						.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")
				};
				this.requestDisplay();
				break;
			}
			case "clear":
				s.data = [];
				this.requestDisplay();
				break;
			case "edit": {
				const t = s.data.find(x => x.id == el.value).$type;
				s.drawer = {
					slug: Object.entries(a.state.schema["Collections"])
						.find(([_, v]) => v.elementTypes[0] === t)[0]
						.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-"),
					id: el.value
				};
				this.requestDisplay();
				break;
			}
			case "remove":
				s.data = s.data.filter(x => x.id != el.value);
				this.requestDisplay();
				break;
		}
		el = event.target.closest("select");
		if (el) {
			const a = this.closest("admin-element");
			s.options ??= Object.fromEntries(await Promise.all((s.field.referenceTypes ?? [s.field.referenceType]).map(t => {
				const n = Object.entries(a.state.schema["Collections"]).find(([_, v]) => v.elementTypes[0] === t)[0];
				return fetch(`${a.dataset.apiUrl}/${n.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")}`).then(x => x.json()).then(x => [t, x]);
			})));
			this.requestDisplay();
		}
	}

	handleCloseDrawer = async () => {
		delete this.state.drawer;
		this.requestDisplay();
	}
}

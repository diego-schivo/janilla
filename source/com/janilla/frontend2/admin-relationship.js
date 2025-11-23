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
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("change", this.handleChange);
	}

	async updateDisplay() {
		const p = this.dataset.path;
		const s = this.state;
		s.field = this.closest("admin-edit").field(p);
		const a = this.closest("admin-element");
		const o = Object.fromEntries(await Promise.all((s.field.referenceTypes ?? [s.field.referenceType]).map(t => {
			const n = Object.entries(a.state.schema["Collections"]).find(([_, v]) => v.elementTypes[0] === t)[0];
			return fetch(`${a.dataset.apiUrl}/${n.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")}`).then(x => x.json()).then(x => [t, x]);
		})));
		this.appendChild(this.interpolateDom({
			$template: "",
			name: p,
			option: {
				$template: "option",
				selected: !s.field.data
			},
			optgroups: Object.entries(o).map(x => ({
				$template: "optgroup",
				label: x[0],
				options: x[1].map(y => ({
					$template: "option",
					value: `${x[0]}:${y.id}`,
					selected: x[0] === s.field.data?.$type && y.id == s.field.data?.id,
					text: y.title
				}))
			})),
			inputs: s.field.data ? ["$type", "id"].map(x => ({
				$template: "input",
				name: `${p}.${x}`,
				value: s.field.data[x]
			})) : null
		}));
	}

	handleChange = event => {
		const el = event.target.closest("select");
		if (el) {
			const a = this.closest("admin-element");
			const s = this.state;
			a.initField(s.field);
			const [t, id] = el.value.split(":");
			Object.assign(s.field.data, {
				$type: t,
				id
			});
			this.requestDisplay();
		}
	}
}

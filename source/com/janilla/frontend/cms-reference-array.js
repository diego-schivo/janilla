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

export default class CmsReferenceArray extends WebComponent {

	static get observedAttributes() {
		return ["data-array-key", "data-path", "data-updated-at"];
	}

	static get templateNames() {
		return ["cms-reference-array"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
	}

	handleClick = async event => {
		const s = this.state;
		let el = event.target.closest("button");
		if (el) {
			event.stopPropagation();
			switch (el.name) {
				case "add":
					s.dialog = true;
					break;
				case "close":
					delete s.dialog;
					break;
			}
			this.requestDisplay();
		}
		el = event.target.closest("a");
		if (el) {
			event.preventDefault();
			const id = parseInt(el.getAttribute("href").split("/").at(-1));
			if (s.dialog) {
				const a = this.closest("cms-admin");
				a.initField(s.field);
				const c = this.querySelector("dialog cms-collection");
				s.field.data.push(c.state.data.find(x => x.id === id));
				delete s.dialog;
			} else
				s.field.data.splice(s.field.data.findIndex(x => x.id === id), 1);
			this.requestDisplay();
			this.dispatchEvent(new CustomEvent("document-change", { bubbles: true }));
		}
	}

	async updateDisplay() {
		const a = this.closest("cms-admin");
		const p = this.dataset.path;
		const s = this.state;
		s.field ??= a.field(p);
		const cc = a.state.schema["Collections"];
		const cn = Object.entries(cc).find(([_, v]) => v.elementTypes[0] === this.dataset.type)[0];
		this.appendChild(this.interpolateDom({
			$template: "",
			...this.dataset,
			name: p,
			collection: cn,
			ids: s.field.data?.map(x => x.id).join(),
			inputs: s.field.data?.map((x, i) => ({
				$template: "input",
				name: `${p}.${i}`,
				value: x.id
			})),
			dialog: s.dialog ? {
				$template: "dialog",
				collection: cn
			} : null
		}));
	}
}

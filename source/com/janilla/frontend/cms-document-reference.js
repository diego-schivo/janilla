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

export default class CmsDocumentReference extends WebComponent {

	static get observedAttributes() {
		return ["data-array-key", "data-path", "data-updated-at"];
	}

	static get templateName() {
		return "cms-document-reference";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("change", this.handleChange);
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("click", this.handleClick);
	}

	attributeChangedCallback(name, oldValue, newValue) {
		const s = this.state;
		if (newValue !== oldValue && s?.field)
			delete s.field;
		super.attributeChangedCallback(name, oldValue, newValue);
	}

	handleChange = async event => {
		let el = event.target.closest("select");
		if (!el?.name) {
			event.stopPropagation();
			const s = this.state;
			s.collection = event.target.value;
			this.requestDisplay();
		}
	}

	handleClick = async event => {
		const s = this.state;
		let el = event.target.closest("button");
		if (el) {
			event.stopPropagation();
			switch (el.name) {
				case "choose":
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
				const c = event.target.closest("cms-collection");
				Object.assign(s.field.data, c.state.data.find(x => x.id === id));
				delete s.dialog;
			} else if (s.field.data)
				for (const x of Object.keys(s.field.data))
					delete s.field.data[x];
			this.requestDisplay();
		}
	}

	async updateDisplay() {
		const a = this.closest("cms-admin");
		const p = this.dataset.path;
		const s = this.state;
		s.field ??= a.field(p);
		const f = x => Object.entries(a.state.schema["Collections"]).find(([_, v]) => v.elementTypes[0] === x)[0];
		this.appendChild(this.interpolateDom({
			$template: "",
			p: !s.field.data?.id ? {
				$template: "p"
			} : null,
			collection: s.field.data?.id ? {
				$template: "collection",
				name: f(s.field.data.$type),
				ids: [s.field.data.id],
			} : null,
			inputs: s.field.data ? Object.entries({
				$type: "Document.Reference",
				type: s.field.data.$type,
				id: s.field.data.id
			}).map(([k, v]) => ({
				$template: "input",
				name: `${p}.${k}`,
				value: v
			})) : null,
			dialog: s.dialog ? {
				$template: "dialog",
				options: [null, ...this.dataset.types.split(",")].map(x => {
					var v = x ? f(x) : null;
					return {
						$template: "option",
						value: v,
						text: x,
						selected: v == s.collection
					};
				}),
				collection: s.collection ? {
					$template: "collection",
					name: s.collection
				} : null
			} : null
		}));
	}
}

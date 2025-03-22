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
import { UpdatableHTMLElement } from "./updatable-html-element.js";

export default class ReferenceControl extends UpdatableHTMLElement {

	static get observedAttributes() {
		return ["data-key", "data-path"];
	}

	static get templateName() {
		return "reference-control";
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
		const b = event.target.closest("button");
		if (b) {
			event.stopPropagation();
			switch (b.name) {
				case "choose":
					s.dialog = true;
					await this.updateDisplay();
					this.querySelector("dialog").showModal();
					break;
				case "close":
					b.closest("dialog").close();
					s.dialog = false;
					break;
			}
		}
		const a = event.target.closest("a");
		if (a) {
			event.preventDefault();
			const id = parseInt(a.getAttribute("href").split("/").at(-1));
			for (const x of Object.getOwnPropertyNames(s.field.data))
				delete s.field.data[x];
			if (s.dialog) {
				const cl = this.querySelector("dialog collection-list");
				Object.assign(s.field.data, cl.state.data.find(x => x.id === id));
				cl.closest("dialog").close();
				s.dialog = false;
			}
			this.requestUpdate();
		}
	}

	async updateDisplay() {
		const ap = this.closest("admin-panel");
		const p = this.dataset.path;
		const s = this.state;
		s.field ??= ap.field(p);
		const cc = ap.state.schema["Collections"];
		const cn = Object.entries(cc).find(([_, v]) => v.elementTypes[0] === this.dataset.type)[0];
		this.appendChild(this.interpolateDom({
			$template: "",
			...this.dataset,
			name: p,
			collection: cn,
			ids: s.field.data?.id ? [s.field.data.id] : [],
			input: {
				$template: "input",
				name: p,
				value: s.field.data?.id
			},
			dialog: s.dialog ? {
				$template: "dialog",
				collection: cn
			} : null
		}));
	}
}

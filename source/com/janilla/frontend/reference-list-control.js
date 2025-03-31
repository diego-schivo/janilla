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
import { WebComponent } from "./web-component.js";

export default class ReferenceListControl extends WebComponent {

	static get observedAttributes() {
		return ["data-key", "data-path"];
	}

	static get templateName() {
		return "reference-list-control";
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
				case "add":
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
			if (s.dialog) {
				const cl = this.querySelector("dialog cms-collection");
				s.field.data.push(cl.state.data.find(x => x.id === id));
				cl.closest("dialog").close();
				s.dialog = false;
			} else
				s.field.data.splice(s.field.data.findIndex(x => x.id === id), 1);
			this.requestDisplay();
		}
	}

	async updateDisplay() {
		const ap = this.closest("cms-admin");
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

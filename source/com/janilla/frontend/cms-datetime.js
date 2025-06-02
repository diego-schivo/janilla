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

export default class CmsDatetime extends WebComponent {

	static get observedAttributes() {
		return ["data-array-key", "data-path", "data-updated-at"];
	}

	static get templateName() {
		return "cms-datetime";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("input", this.handleInput);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("input", this.handleInput);
	}

	handleInput = event => {
		const el = event.target.closest("input");
		if (!el?.name) {
			this.querySelector('[type="hidden"]').value = (new Date(el.value)).toISOString();
			this.dispatchEvent(new CustomEvent("document-change", { bubbles: true }));
		}
	}

	async updateDisplay() {
		const a = this.closest("cms-admin");
		const p = this.dataset.path;
		const f = a.field(p);
		this.appendChild(this.interpolateDom({
			$template: "",
			name: p,
			value: f.data,
			localValue: f.data ? (() => {
				const d = new Date(f.data);
				const s1 = [d.getFullYear(), 1 + d.getMonth(), d.getDate()].map(x => x.toString().padStart(2, "0")).join("-");
				const s2 = [d.getHours(), d.getMinutes(), d.getSeconds()].map(x => x.toString().padStart(2, "0")).join(":");
				const s3 = d.getMilliseconds().toString().padStart(3, "0");
				return `${s1}T${s2}.${s3}`;
			})() : null
		}));
	}
}

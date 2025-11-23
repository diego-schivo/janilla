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

const documents = {};
const parser = new DOMParser();

export default class LucideIcon extends WebComponent {

	static get observedAttributes() {
		return ["data-name"];
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const s = this.state;
		if (this.dataset.name === s.name)
			return;
		s.name = this.dataset.name;
		while (this.firstChild)
			this.removeChild(this.lastChild);
		if (!s.name)
			return;
		documents[s.name] ??= fetch(`/lucide-0.475.0/icons/${s.name}.svg`).then(x => x.text()).then(x => {
			return parser.parseFromString(x, "image/svg+xml");
		});
		const d = await documents[s.name];
		this.appendChild(d.firstChild.cloneNode(true));
	}
}

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

export default class AdminCreateFirstUser extends WebComponent {

	static get templateNames() {
		return ["admin-create-first-user"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("submit", this.handleSubmit);
	}

	async updateDisplay() {
		document.title = "Create first user - Janilla";
		this.appendChild(this.interpolateDom({ $template: "" }));
	}

	handleSubmit = async event => {
		event.preventDefault();
		event.stopPropagation();
		const a = this.closest("admin-element");
		const o = Array.from(new FormData(event.target).entries()).reduce((x, y) => {
			if (y[0] === "roles")
				(x[y[0]] ??= []).push(y[1]);
			else
				x[y[0]] = y[1];
			return x;
		}, {});
		const r = await fetch(`${a.dataset.apiUrl}/users/first-register`, {
			method: "POST",
			headers: { "content-type": "application/json" },
			body: JSON.stringify(o)
		});
		const j = await r.json();
		if (r.ok) {
			this.dispatchEvent(new CustomEvent("user-change", {
				bubbles: true,
				detail: { user: j }
			}));
			history.pushState({}, "", "/admin");
			dispatchEvent(new CustomEvent("popstate"));
		} else
			a.renderToast(j, "error");
	}
}

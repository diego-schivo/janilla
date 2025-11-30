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

export default class AdminDashboard extends WebComponent {

	static get templateNames() {
		return ["admin-dashboard"];
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

	async updateDisplay() {
		const a = this.closest("admin-element");
		const s = a.state.schema;
		this.appendChild(this.interpolateDom({
			$template: "",
			groups: a.dashboardGroups().map(g => ({
				$template: "group",
				title: g.split(/(?=[A-Z])/).map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" "),
				cards: Object.entries(s[s["Data"][g].type]).map(([k, v]) => {
					return {
						$template: "card",
						href: `/admin/${g === "globals" ? g : "collections"}/${k.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")}`,
						text: k.split(/(?=[A-Z])/).map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" "),
						button: g !== "globals" && !a.isReadOnly(v.elementTypes[0]) ? {
							$template: "button",
							type: v.elementTypes[0]
						} : null
					};
				})
			}))
		}));
	}

	handleClick = async event => {
		const el = event.target.closest("button");
		if (el) {
			event.stopPropagation();
			event.preventDefault();
			const a = this.closest("admin-element");
			switch (el.name) {
				case "seed": {
					const r = await fetch(`${a.dataset.apiUrl}/seed`, {
						method: "POST",
						credentials: "include"
					});
					const j = await r.json();
					if (r.ok)
						a.renderToast("Database seeded!", "success");
					else
						a.renderToast(j, "error");
					break;
				}
				case "create": {
					const t = el.closest("a").getAttribute("href").split("/").at(-1);
					const r = await fetch(`${a.dataset.apiUrl}/${t}`, {
						method: "POST",
						credentials: "include",
						headers: { "content-type": "application/json" },
						body: JSON.stringify({ $type: el.dataset.type })
					});
					const j = await r.json();
					if (r.ok) {
						history.pushState({}, "", `/admin/collections/${t}/${j.id}`);
						dispatchEvent(new CustomEvent("popstate"));
					} else
						a.renderToast(j, "error");
					break;
				}
			}
		}
	}
}

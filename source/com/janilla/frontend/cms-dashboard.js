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

export default class CmsDashboard extends WebComponent {

	static get templateNames() {
		return ["cms-dashboard"];
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
		const el = event.target.closest("button");
		if (el) {
			event.stopPropagation();
			event.preventDefault();
			const a = this.closest("cms-admin");
			switch (el.name) {
				case "seed":
					await fetch(`${a.dataset.apiUrl}/seed`, { method: "POST" });
					a.renderToast("Database seeded!", "success");
					break;
				case "create":
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

	async updateDisplay() {
		const a = this.closest("cms-admin");
		const as = a.state;
		this.appendChild(this.interpolateDom({
			$template: "",
			groups: a.dashboardGroups().map(([k, v]) => ({
				$template: "group",
				name: k.split(/(?=[A-Z])/).map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" "),
				cards: Object.entries(as.schema[v.type]).map(([k2, v2]) => {
					return {
						$template: "card",
						href: `/admin/${k !== "globals" ? "collections" : k}/${k2.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")}`,
						name: k2.split(/(?=[A-Z])/).map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" "),
						button: k !== "globals" && !a.isReadOnly(v2.elementTypes[0]) ? {
							$template: "button",
							type: v2.elementTypes[0]
						} : null
					};
				})
			}))
		}));
	}
}

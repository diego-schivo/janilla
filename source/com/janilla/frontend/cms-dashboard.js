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

export default class CmsDashboard extends WebComponent {

	static get templateName() {
		return "cms-dashboard";
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
		const b = event.target.closest("button");
		if (!b)
			return;
		event.stopPropagation();
		switch (b.name) {
			case "seed":
				await fetch("/api/seed", { method: "POST" });
				break;
			case "create":
				const a = b.previousElementSibling;
				const t = a.getAttribute("href").split("/").at(-1);
				const d = await (await fetch(`/api/${t}`, {
					method: "POST",
					headers: { "content-type": "application/json" },
					body: JSON.stringify({})
				})).json();
				history.pushState(undefined, "", `/admin/collections/${t}/${d.id}`);
				dispatchEvent(new CustomEvent("popstate"));
				break;
		}
	}

	async updateDisplay() {
		const s = this.closest("cms-admin").state;
		this.appendChild(this.interpolateDom({
			$template: "",
			items: Object.entries(s.schema["Data"]).map(([k, v]) => ({
				$template: "group",
				name: k,
				items: Object.keys(s.schema[v.type]).map(x => {
					const nn = x.split(/(?=[A-Z])/).map(x => x.toLowerCase());
					return {
						$template: "card",
						href: `/admin/${k}/${nn.join("-")}`,
						name: nn.join(" "),
						button: k === "collections" ? { $template: "button" } : null
					};
				})
			}))
		}));
	}
}

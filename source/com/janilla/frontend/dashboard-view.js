/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import { UpdatableHTMLElement } from "./updatable-html-element.js";

export default class DashboardView extends UpdatableHTMLElement {

	static get templateName() {
		return "dashboard-view";
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
		const a = b.previousElementSibling;
		const t = a.getAttribute("href").split("/").at(-1);
		const e = await (await fetch(`/api/${t}`, {
			method: "POST",
			headers: { "content-type": "application/json" },
			body: JSON.stringify({})
		})).json();
		history.pushState(undefined, "", `/admin/collections/${t}/${e.id}`);
		dispatchEvent(new CustomEvent("popstate"));
	}

	async updateDisplay() {
		const s = this.closest("admin-panel").state;
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

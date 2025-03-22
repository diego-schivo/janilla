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

export default class CollectionList extends UpdatableHTMLElement {

	static get observedAttributes() {
		return ["data-ids", "data-name"];
	}

	static get templateName() {
		return "collection-list";
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
			case "create":
				const n = this.dataset.name;
				const e = await (await fetch(`/api/${n}`, {
					method: "POST",
					headers: { "content-type": "application/json" },
					body: JSON.stringify({ $type: this.closest("admin-panel").state.schema["Collections"][n].elementTypes[0] })
				})).json();
				history.pushState(undefined, "", `/admin/collections/${n}/${e.id}`);
				dispatchEvent(new CustomEvent("popstate"));
				break;
		}
	}

	async updateDisplay() {
		const s = this.state;
		const pe = this.parentElement;
		const pen = pe.tagName.toLowerCase();
		s.dialog ??= pen === "dialog";
		const n = this.dataset.name;
		switch (pen) {
			case "reference-control":
				s.data = pe.state.field.data?.id ? [pe.state.field.data] : [];
				break;
			case "reference-list-control":
				s.data ??= pe.state.field.data;
				break;
			default:
				s.data ??= await (await fetch(`/api/${n.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")}`)).json();
				break;
		}
		const ap = this.closest("admin-panel");
		const hh = ap.headers(n);
		this.appendChild(this.interpolateDom({
			$template: "",
			header: !pen.endsWith("-control") ? {
				$template: "header",
				title: n
			} : null,
			heads: hh.map(x => ({
				$template: "head",
				text: x
			})),
			rows: s.data.map(x => ({
				$template: "row",
				cells: (() => {
					const cc = hh.map(y => {
						const z = x[y];
						return {
							content: typeof z === "object" && z?.$type === "File" ? {
								$template: "media",
								...x
							} : y === "updatedAt" ? ap.dateTimeFormat.format(new Date(z)) : z
						};
					});
					cc[0].href = `/admin/collections/${n.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")}/${x.id}`;
					if (!cc[0].content)
						cc[0].content = x.id;
					return cc;
				})().map(y => ({
					$template: y.href ? "link-cell" : "cell",
					...y
				}))
			}))
		}));
	}
}

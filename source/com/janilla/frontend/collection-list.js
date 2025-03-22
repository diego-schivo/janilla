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
			rows: s.data?.map(x => ({
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

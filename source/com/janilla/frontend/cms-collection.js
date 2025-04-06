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

export default class CmsCollection extends WebComponent {

	static get observedAttributes() {
		return ["data-ids", "data-name"];
	}

	static get templateName() {
		return "cms-collection";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("change", this.handleChange);
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("click", this.handleClick);
	}

	attributeChangedCallback(name, oldValue, newValue) {
		if (newValue !== oldValue && this.state)
			delete this.state.computeState;
		super.attributeChangedCallback(name, oldValue, newValue);
	}

	handleChange = event => {
		const el = event.target.closest('[type="checkbox"]');
		if (!el)
			return;
		const s = this.state;
		if (el.matches("[value]")) {
			const id = parseInt(el.value);
			const i = s.selectionIds.indexOf(id);
			if (i >= 0)
				s.selectionIds.splice(i, 1);
			if (el.checked)
				s.selectionIds.push(id);
		} else if (el.checked)
			s.selectionIds = s.data.map(x => x.id);
		else
			s.selectionIds.length = 0;
		this.requestDisplay();
	}

	handleClick = async event => {
		const b = event.target.closest("button");
		if (!b)
			return;
		event.stopPropagation();
		const n = this.dataset.name;
		const s = this.state;
		let u;
		switch (b.name) {
			case "cancel-delete":
				delete s.deleteDialog;
				this.requestDisplay();
				break;
			case "cancel-publish":
				delete s.publishDialog;
				this.requestDisplay();
				break;
			case "cancel-unpublish":
				delete s.unpublishDialog;
				this.requestDisplay();
				break;
			case "confirm-delete":
				u = new URL(`/api/${n}`, location.href);
				Array.prototype.forEach.call(this.querySelectorAll("[value]:checked"), x => u.searchParams.append("id", x.value));
				await (await fetch(u, { method: "DELETE" })).json();
				delete s.deleteDialog;
				delete s.data;
				this.requestDisplay();
				break;
			case "confirm-publish":
				u = new URL(`/api/${n}`, location.href);
				Array.prototype.forEach.call(this.querySelectorAll("[value]:checked"), x => u.searchParams.append("id", x.value));
				await (await fetch(u, {
					method: "PATCH",
					headers: { "content-type": "application/json" },
					body: JSON.stringify({
						$type: this.closest("cms-admin").state.schema["Collections"][n].elementTypes[0],
						status: "PUBLISHED"
					})
				})).json();
				delete s.publishDialog;
				this.requestDisplay();
				break;
			case "confirm-unpublish":
				u = new URL(`/api/${n}`, location.href);
				Array.prototype.forEach.call(this.querySelectorAll("[value]:checked"), x => u.searchParams.append("id", x.value));
				await (await fetch(u, {
					method: "PATCH",
					headers: { "content-type": "application/json" },
					body: JSON.stringify({
						$type: this.closest("cms-admin").state.schema["Collections"][n].elementTypes[0],
						status: "DRAFT"
					})
				})).json();
				delete s.unpublishDialog;
				this.requestDisplay();
				break;
			case "create":
				const d = await (await fetch(`/api/${n}`, {
					method: "POST",
					headers: { "content-type": "application/json" },
					body: JSON.stringify({ $type: this.closest("cms-admin").state.schema["Collections"][n].elementTypes[0] })
				})).json();
				history.pushState(undefined, "", `/admin/collections/${n}/${d.id}`);
				dispatchEvent(new CustomEvent("popstate"));
				break;
			case "delete":
				s.deleteDialog = true;
				this.requestDisplay();
				break;
			case "publish":
				s.publishDialog = true;
				this.requestDisplay();
				break;
			case "unpublish":
				s.unpublishDialog = true;
				this.requestDisplay();
				break;
		}
	}

	async computeState() {
		const s = this.state;
		[
			"data"
		].forEach(x => delete s[x]);
		const pe = this.parentElement;
		const pen = pe.tagName.toLowerCase();
		//s.dialog = pen === "dialog";
		s.selectionIds = [];
		const n = this.dataset.name;
		switch (pen) {
			case "cms-reference":
			case "cms-document-reference":
				s.data = pe.state.field.data?.id ? [pe.state.field.data] : [];
				break;
			case "reference-list-control":
				s.data = pe.state.field.data;
				break;
			default:
				s.data = await (await fetch(`/api/${n.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")}`)).json();
				this.requestDisplay();
				break;
		}
	}

	async updateDisplay() {
		const s = this.state;
		s.computeState ??= this.computeState();
		const pe = this.parentElement;
		const pen = pe.tagName.toLowerCase();
		const a = this.closest("cms-admin");
		const n = this.dataset.name;
		const hh = a.headers(n);
		this.appendChild(this.interpolateDom({
			$template: "",
			header: !["cms-reference", "cms-document-reference", "reference-list-control"].includes(pen) ? {
				$template: "header",
				title: n.split(/(?=[A-Z])/).map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" "),
				selection: s.selectionIds?.length ? {
					$template: "selection",
					count: s.selectionIds.length
				} : null
			} : null,
			noResults: !s.data?.length ? {
				$template: "no-results",
				name: n
			} : null,
			table: s.data?.length ? {
				$template: "table",
				heads: (() => {
					const cc = hh.map(x => x.split(/(?=[A-Z])/).map(y => y.charAt(0).toUpperCase() + y.substring(1)).join(" "));
					if (!["cms-reference", "cms-document-reference", "dialog", "reference-list-control"].includes(pen))
						cc.unshift({
							$template: "checkbox",
							checked: s.selectionIds.length === s.data.length
						});
					return cc;
				})().map(x => ({
					$template: "head",
					content: x
				})),
				rows: s.data.map(x => ({
					$template: "row",
					cells: (() => {
						const cc = hh.map(y => {
							const z = x[y];
							return typeof z === "object" && z?.$type === "File" ? {
								$template: "media",
								...x
							} : y === "updatedAt" ? a.dateTimeFormat.format(new Date(z)) : z;
						});
						if (!cc[0])
							cc[0] = x.id;
						cc[0] = {
							$template: "link",
							href: `/admin/collections/${n.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")}/${x.id}`,
							content: cc[0]
						};
						if (!["cms-reference", "cms-document-reference", "dialog", "reference-list-control"].includes(pen))
							cc.unshift({
								$template: "checkbox",
								value: x.id,
								checked: s.selectionIds.includes(x.id)
							});
						return cc;
					})().map(y => ({
						$template: "cell",
						content: y
					}))
				}))
			} : null,
			publishDialog: s.publishDialog ? {
				$template: "publish-dialog",
				count: s.selectionIds.length,
				name: this.dataset.name.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join(" ")
			} : null,
			unpublishDialog: s.unpublishDialog ? {
				$template: "unpublish-dialog",
				count: s.selectionIds.length,
				name: this.dataset.name.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join(" ")
			} : null,
			deleteDialog: s.deleteDialog ? {
				$template: "delete-dialog",
				count: s.selectionIds.length,
				name: this.dataset.name.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join(" ")
			} : null
		}));
	}
}

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

export default class CmsEdit extends WebComponent {

	static get observedAttributes() {
		return ["data-updated-at"];
	}

	static get templateName() {
		return "cms-edit";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("change", this.handleChange);
		this.addEventListener("document-change", this.handleDocumentChange);
		this.addEventListener("input", this.handleInput);
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("document-change", this.handleDocumentChange);
		this.removeEventListener("input", this.handleInput);
		this.removeEventListener("submit", this.handleSubmit);
	}

	handleChange = async event => {
		const el = event.target;
		if (el.matches("select:not([name])")) {
			event.stopPropagation();
			const a = this.closest("cms-admin");
			const s = a.state;
			let r, j;
			switch (el.value) {
				case "create":
					r = await fetch(`/api/${s.collectionSlug}`, {
						method: "POST",
						headers: { "content-type": "application/json" },
						body: JSON.stringify({ $type: s.documentType })
					});
					j = await r.json();
					if (r.ok) {
						history.pushState(undefined, "", `/admin/collections/${s.collectionSlug}/${j.id}`);
						dispatchEvent(new CustomEvent("popstate"));
					} else
						a.renderToast(j, "error");
					break;
				case "duplicate":
					for (const [k, v] of [...new FormData(this.querySelector("form")).entries()]) {
						const i = k.lastIndexOf(".");
						let f = a.field(k.substring(0, i));
						a.initField(f);
						f.data[k.substring(i + 1)] = v;
					}
					r = await fetch(`/api/${s.collectionSlug}`, {
						method: "POST",
						headers: { "content-type": "application/json" },
						body: JSON.stringify(s.document)
					});
					j = await r.json();
					if (r.ok) {
						history.pushState(undefined, "", `/admin/collections/${s.collectionSlug}/${j.id}`);
						dispatchEvent(new CustomEvent("popstate"));
					} else
						a.renderToast(j, "error");
					break;
				case "delete":
					r = await fetch(s.documentUrl, { method: "DELETE" });
					j = await r.json();
					if (r.ok) {
						history.pushState(undefined, "", `/admin/${s.pathSegments.slice(0, 2).join("/")}`);
						dispatchEvent(new CustomEvent("popstate"));
					} else
						a.renderToast(j, "error");
					break;
			}
		}
	}

	handleDocumentChange = () => {
		const s = this.state;
		if (!s.changed) {
			s.changed = true;
			this.requestDisplay();
		}
		if (s.drafts)
			this.requestAutoSave();
	}

	handleInput = event => {
		const el = event.target;
		const s = this.state;
		if (el.matches("[name]")) {
			if (!s.changed) {
				s.changed = true;
				this.requestDisplay();
			}
			if (s.drafts)
				this.requestAutoSave();
		}
	}

	handleSubmit = async event => {
		event.preventDefault();
		await this.saveEntity(false);
	}

	requestAutoSave(delay = 1000) {
		const as = (this.autoSave ??= {});
		if (as.ongoing) {
			as.repeat = true;
			return;
		}
		if (typeof as.timeoutID === "number")
			clearTimeout(as.timeoutID);
		as.timeoutID = setTimeout(async () => await this.autoSaveTimeout(), delay);
	}

	async autoSaveTimeout() {
		const as = this.autoSave;
		as.timeoutID = undefined;
		as.ongoing = true;
		try {
			await this.saveEntity(true);
		} finally {
			as.ongoing = false;
		}
		if (as.repeat) {
			as.repeat = false;
			this.requestAutoSave(0);
		}
	}

	async saveEntity(auto) {
		const kkvv = [...new FormData(this.querySelector("form")).entries()];
		const kkvv2 = kkvv.filter(([_, x]) => x instanceof File);
		if (kkvv2.length) {
			const fd = new FormData();
			for (const [k, v] of kkvv2)
				fd.append(k, v);
			const xhr = new XMLHttpRequest();
			xhr.open("POST", "/api/files/upload", true);
			xhr.send(fd);
		}
		const a = this.closest("cms-admin");
		for (const [k, v] of kkvv) {
			const i = k.lastIndexOf(".");
			let f = a.field(k.substring(0, i));
			//if (!f.data)
			//f.parent.data[f.name] = f.data = {};
			a.initField(f);
			f.data[k.substring(i + 1)] = v instanceof File ? { name: v.name } : v;
			//console.log(k, v, f);
		}
		const s = a.state;
		//console.log("s", s);
		const u = new URL(s.documentUrl, location.href);
		if (auto) {
			u.searchParams.append("draft", true);
			u.searchParams.append("autosave", true);
		}
		const r = await fetch(u, {
			method: s.document.id ? "PUT" : "POST",
			headers: { "content-type": "application/json" },
			body: JSON.stringify(s.document)
		});
		const j = await r.json();
		if (r.ok) {
			delete this.state.changed;
			if (!auto)
				a.renderToast("Updated successfully.", "success");
			const copy = (x, y) => {
				//console.log("x", JSON.stringify(x, null, "\t"), "y", JSON.stringify(y, null, "\t"));
				for (const [k, v] of Object.entries(x))
					if (Array.isArray(v))
						for (let i = 0; i < v.length; i++)
							if (typeof v[i] === "object" && v[i] !== null)
								copy(v[i], y[k][i]);
							else
								y[k][i] = v[i];
					else if (typeof v === "object" && v !== null)
						copy(v, y[k]);
					else
						y[k] = v;
			};
			copy(j, s.document);
			//console.log(s.document);
			a.requestDisplay();
		} else
			a.renderToast(j, "error");
	}

	async updateDisplay() {
		const a = this.closest("cms-admin");
		const s = a.state;
		this.state.versions = Object.hasOwn(s.document, "versionCount");
		this.state.drafts = Object.hasOwn(s.document, "status");
		this.appendChild(this.interpolateDom({
			$template: "",
			entries: (() => {
				const kkvv = [];
				if (this.state.drafts)
					kkvv.push(["Status", s.document.status]);
				if (s.document.updatedAt)
					kkvv.push(["Last Modified", a.dateTimeFormat.format(new Date(s.document.updatedAt))]);
				if (s.document.createdAt)
					kkvv.push(["Created", a.dateTimeFormat.format(new Date(s.document.createdAt))]);
				return kkvv;
			})().map(x => ({
				$template: "entry",
				key: x[0],
				value: x[1]
			})),
			previewLink: (() => {
				const h = a.preview(s.document);
				return h ? {
					$template: "preview-link",
					href: h
				} : null;
			})(),
			button: {
				$template: "button",
				...(this.state.drafts ? {
					name: "publish",
					disabled: s.document.status === "PUBLISHED" && !this.state.changed,
					text: "Publish Changes"
				} : {
					name: "save",
					disabled: !this.state.changed,
					text: "Save"
				})
			},
			select: s.collectionSlug ? {
				$template: "select",
				options: [{
					text: "\u22ee"
				}, {
					value: "create",
					text: "Create New"
				}, {
					value: "duplicate",
					text: "Duplicate"
				}, {
					value: "delete",
					text: "Delete"
				}].map((x, i) => ({
					$template: "option",
					...x,
					selected: i === 0
				}))
			} : null
		}));
	}
}

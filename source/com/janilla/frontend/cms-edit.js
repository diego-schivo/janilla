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

	static get templateName() {
		return "cms-edit";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("change", this.handleChange);
		this.addEventListener("input", this.handleInput);
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("input", this.handleInput);
		this.removeEventListener("submit", this.handleSubmit);
	}

	handleChange = async event => {
		const el = event.target;
		//if (el.matches("[name]") && this.state.drafts)
			//this.requestAutoSave();
		if (el.matches(":not([name])")) {
			event.stopPropagation();
			if (el.value === "delete") {
				const a = this.closest("cms-admin");
				const s = a.state;
				await (await fetch(s.entityUrl, { method: "DELETE" })).json();
				history.pushState(undefined, "", `/admin/${s.pathSegments.slice(0, 2).join("/")}`);
				dispatchEvent(new CustomEvent("popstate"));
			}
		}
	}

	handleInput = event => {
		const el = event.target;
		if (el.matches("[name]") && this.state.drafts)
			this.requestAutoSave();
	}

	handleSubmit = async event => {
		event.preventDefault();
		await this.saveEntity(false);
	}

	requestAutoSave(delay = 1000) {
		const as = (this.janillas.autoSave ??= {});
		if (as.ongoing) {
			as.repeat = true;
			return;
		}
		if (typeof as.timeoutID === "number")
			clearTimeout(as.timeoutID);
		as.timeoutID = setTimeout(async () => await this.autoSaveTimeout(), delay);
	}

	async autoSaveTimeout() {
		const as = this.janillas.autoSave;
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
			console.log(k, v, f);
		}
		const s = a.state;
		console.log("s", s);
		const u = new URL(s.entityUrl, location.href);
		if (auto) {
			u.searchParams.append("draft", true);
			u.searchParams.append("autosave", true);
		}
		//s.entity = await (await fetch(u, {
		await (await fetch(u, {
			method: "PUT",
			headers: { "content-type": "application/json" },
			body: JSON.stringify(s.entity)
		})).json();
		a.requestDisplay();
	}

	async updateDisplay() {
		const ap = this.closest("cms-admin");
		const s = ap.state;
		this.state.versions = Object.hasOwn(s.entity, "versionCount");
		this.state.drafts = Object.hasOwn(s.entity, "status");
		this.appendChild(this.interpolateDom({
			$template: "",
			status: this.state.drafts ? {
				$template: "status",
				text: s.entity.status,
			} : null,
			updatedAt: ap.dateTimeFormat.format(new Date(s.entity.updatedAt)),
			createdAt: ap.dateTimeFormat.format(new Date(s.entity.createdAt)),
			saveButton: !this.state.drafts ? { $template: "save-button" } : null,
			publishButton: this.state.drafts ? { $template: "publish-button" } : null,
			previewLink: (() => {
				const h = ap.preview(s.entity);
				return h ? {
					$template: "preview-link",
					href: h
				} : null;
			})()
		}));
	}
}

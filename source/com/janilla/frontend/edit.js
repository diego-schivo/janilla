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

export default class EditView extends UpdatableHTMLElement {

	static get templateName() {
		return "edit";
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
		if (el.matches("select:not([name])")) {
			event.stopPropagation();
			const p = this.dataset.entityPath;
			const nn = p ? p.split(".") : [];
			await (await fetch(`/api/${nn[1]}/${nn[2]}`, { method: "DELETE" })).json();
			return;
		}

		if (el.matches("select"))
			this.requestAutoSave();
	}

	handleInput = () => {
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
		const f = this.querySelector("form");
		const kkvv = [...new FormData(f).entries()];
		for (const [k, v] of kkvv.filter(([_, x]) => x instanceof File)) {
			const fd = new FormData();
			fd.append(k, v);
			const xhr = new XMLHttpRequest();
			xhr.open("POST", "/api/upload", true);
			xhr.send(fd);
		}
		const ap = this.closest("admin-panel");
		for (const [k, v] of kkvv) {
			const i = k.lastIndexOf(".");
			let f = ap.field(k.substring(0, i));
			if (!f.data)
				f.parent.data[f.name] = f.data = {};
			f.data[k.substring(i + 1)] = v instanceof File ? { name: v.name } : v;
		}
		const s = ap.state;
		const u = new URL(s.entityUrl, location.href);
		if (auto) {
			u.searchParams.append("draft", true);
			u.searchParams.append("autosave", true);
		}
		s.entity = await (await fetch(u, {
			method: "PUT",
			headers: { "content-type": "application/json" },
			body: JSON.stringify(s.entity)
		})).json();
		ap.requestUpdate();
	}

	async updateDisplay() {
		const ap = this.closest("admin-panel");
		const s = ap.state;
		this.appendChild(this.interpolateDom({
			$template: "",
			loading: !Object.hasOwn(s, "entity") ? { $template: "loading" } : null,
			form: s.entity ? {
				$template: "form",
				saveButton: null,
				publishButton: { $template: "publish-button" },
				previewLink: (() => {
					const h = ap.preview(s.entity);
					return h ? {
						$template: "preview-link",
						href: h
					} : null;
				})()
			} : null
		}));
	}
}

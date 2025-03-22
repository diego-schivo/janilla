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

export default class VersionView extends UpdatableHTMLElement {

	static get templateName() {
		return "version";
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
		if (!b?.name)
			return;
		event.stopPropagation();
		switch (b.name) {
			case "cancel":
				this.querySelector("dialog").close();
				break;
			case "confirm":
				const ap = this.closest("admin-panel");
				const s = ap.state;
				s.entity = await (await fetch(`${s.entityUrl.substring(0, s.entityUrl.lastIndexOf("/"))}/versions/${s.version.id}`, { method: "POST" })).json();
				ap.querySelector("toast-container").renderToast("Restored successfully.");
				history.pushState(undefined, "", `/admin/${ap.dataset.entityPath.replaceAll(".", "/")}`);
				dispatchEvent(new CustomEvent("popstate"));
				break;
			case "restore":
				this.querySelector("dialog").showModal();
				break;
		}
	}

	async updateDisplay() {
		const ap = this.closest("admin-panel");
		const s = ap.state;
		const ua = ap.dateTimeFormat.format(new Date(s.version.updatedAt));
		this.appendChild(this.interpolateDom({
			$template: "",
			title: ua,
			json: JSON.stringify(s.version.entity, null, "  "),
			dialog: {
				$template: "dialog",
				dateTime: ua
			}
		}));
	}
}

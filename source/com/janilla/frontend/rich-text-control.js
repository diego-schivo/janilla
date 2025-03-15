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

export default class RichTextControl extends UpdatableHTMLElement {

	static get observedAttributes() {
		return ["data-key", "data-path"];
	}

	static get templateName() {
		return "rich-text-control";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
		this.addEventListener("keyup", this.handleKeyUp);
		this.addEventListener("input", this.handleInput);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
		this.removeEventListener("keyup", this.handleKeyUp);
		this.removeEventListener("input", this.handleInput);
	}

	handleClick = event => {
		const b = event.target.closest("button");
		const ce = event.target.closest("[contenteditable]");
		if (b) {
			event.stopPropagation();
			switch (b.name) {
				case "type":
					document.execCommand("formatBlock", false, "<p>");
					break;
				case "heading-1":
				case "heading-2":
				case "heading-3":
				case "heading-4":
					document.execCommand("formatBlock", false, `<h${b.name.charAt(b.name.length - 1)}>`);
					break;
				case "bold":
				case "italic":
				case "underline":
					document.execCommand(b.name);
					break;
				case "link":
					const u = prompt("URL");
					if (u)
						document.execCommand("createLink", false, u);
					break;
			}
		} else if (!ce)
			return;
		this.requestUpdate();
	}

	handleKeyUp = event => {
		const ce = event.target.closest("[contenteditable]");
		if (!ce)
			return;
		this.requestUpdate();
	}

	handleInput = event => {
		const ce = event.target.closest("[contenteditable]");
		if (!ce)
			return;
		if (!ce.firstElementChild)
			document.execCommand("formatBlock", false, "<p>");
	}

	async updateDisplay() {
		const ap = this.closest("admin-panel");
		const p = this.dataset.path;
		const f = ap.field(p);
		const ce = this.querySelector("[contenteditable]");
		this.appendChild(this.interpolateDom({
			$template: "",
			items: ["type", "heading-1", "heading-2", "heading-3", "heading-4", "bold", "italic", "underline", "link"].map(x => ({
				$template: "item",
				name: x,
				class: ["bold", "italic", "underline"].includes(x) && document.queryCommandState(x) ? "active" : null
			})),
			name: p,
			value: ce?.innerHTML ?? f.data
		}));
		if (!ce)
			this.querySelector("[contenteditable]").innerHTML = f.data;
	}
}

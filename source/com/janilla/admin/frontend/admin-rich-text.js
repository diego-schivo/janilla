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
 * Note that authoring this file involved dealing in other programs that are
 * provided under the following license:
 *
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
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
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
import WebComponent from "web-component";

export default class AdminRichText extends WebComponent {

	static get templateNames() {
		return ["admin-rich-text"];
	}

	static get observedAttributes() {
		return ["data-array-key", "data-path", "data-updated-at"];
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

	async updateDisplay() {
		const p = this.dataset.path;
		const f = this.closest("admin-edit").field(p);
		const ce = this.querySelector("[contenteditable]");
		const h = this.querySelector('[type="hidden"]');
		const v = h?.value;
		this.appendChild(this.interpolateDom({
			$template: "",
			items: ["type", "heading-1", "heading-2", "heading-3", "heading-4", "bold", "italic", "underline", "link"].map(x => ({
				$template: "item",
				name: x,
				class: ["bold", "italic", "underline"].includes(x) && document.queryCommandState(x) ? "active" : null
			})),
			path: p,
			data: f.data
		}));
		if (!ce)
			this.querySelector("[contenteditable]").innerHTML = f.data ?? "";
		if (h && h.value !== v)
			h.dispatchEvent(new InputEvent("input", {
				view: window,
				bubbles: true,
				cancelable: true
			}));
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
		this.requestDisplay();
	}

	handleKeyUp = event => {
		const ce = event.target.closest("[contenteditable]");
		if (!ce)
			return;
		this.requestDisplay();
	}

	handleInput = event => {
		const ce = event.target.closest("[contenteditable]");
		if (!ce)
			return;
		if (!ce.firstElementChild)
			document.execCommand("formatBlock", false, "<p>");
	}
}

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

export default class ReferenceListControl extends UpdatableHTMLElement {

	static get observedAttributes() {
		return ["data-key", "data-path"];
	}

	static get templateName() {
		return "reference-list-control";
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
		const s = this.state;
		const b = event.target.closest("button");
		if (b) {
			event.stopPropagation();
			switch (b.name) {
				case "add":
					s.dialog = true;
					await this.updateDisplay();
					this.querySelector("dialog").showModal();
					break;
				case "close":
					b.closest("dialog").close();
					s.dialog = false;
					break;
			}
		}
		const a = event.target.closest("a");
		if (a) {
			event.preventDefault();
			const id = parseInt(a.getAttribute("href").split("/").at(-1));
			if (s.dialog) {
				const cl = this.querySelector("dialog collection-list");
				s.field.data.push(cl.state.data.find(x => x.id === id));
				cl.closest("dialog").close();
				s.dialog = false;
			} else
				s.field.data.splice(s.field.data.findIndex(x => x.id === id), 1);
			this.requestUpdate();
		}
	}

	async updateDisplay() {
		const ap = this.closest("admin-panel");
		const p = this.dataset.path;
		const s = this.state;
		s.field ??= ap.field(p);
		const cc = ap.state.schema["Collections"];
		const cn = Object.entries(cc).find(([_, v]) => v.elementTypes[0] === this.dataset.type)[0];
		this.appendChild(this.interpolateDom({
			$template: "",
			...this.dataset,
			name: p,
			collection: cn,
			ids: s.field.data.map(x => x.id).join(),
			inputs: s.field.data.map((x, i) => ({
				$template: "input",
				name: `${p}.${i}`,
				value: x.id
			})),
			dialog: s.dialog ? {
				$template: "dialog",
				collection: cn
			} : null
		}));
	}
}

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

export default class SlugControl extends UpdatableHTMLElement {

	static get observedAttributes() {
		return ["data-key", "data-path"];
	}

	static get templateName() {
		return "slug-control";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.state.adminPanel = this.closest("admin-panel");
		this.state.adminPanel.addEventListener("input", this.handleInput);
	}

	disconnectedCallback() {
		this.state.adminPanel.removeEventListener("input", this.handleInput);
		super.disconnectedCallback();
	}

	handleInput = event => {
		const el = event.target.closest('input[name="title"]');
		if (!el)
			return;
		const s = this.state;
		s.field.parent.data[s.field.name] = el.value.split(/\W+/).filter(x => x).map(x => x.toLowerCase()).join("-");
		s.field = s.adminPanel.field(s.field.name, s.field.parent);
		this.requestUpdate();
	}

	async updateDisplay() {
		const p = this.dataset.path;
		const s = this.state;
		s.field ??= s.adminPanel.field(p);
		this.appendChild(this.interpolateDom({
			$template: "",
			name: p,
			value: s.field.data
		}));
	}
}

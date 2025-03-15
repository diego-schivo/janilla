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

export default class Tabs extends UpdatableHTMLElement {

	static get templateName() {
		return "tabs";
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
		this.state.index = parseInt(b.getAttribute("aria-controls").substring("foo-".length));
		this.requestUpdate();
	}

	async updateDisplay() {
		const tt = ["edit", "versions"];
		const s = this.state;
		s.index ??= 0;
		const el = this.interpolateDom({
			$template: "",
			buttons: tt.map((x, i) => ({
				$template: "tabs-button",
				panel: "foo",
				index: i,
				selected: `${i === s.index}`,
				label: x
			})),
			panels: tt.map((_, i) => ({
				$template: "tabs-panel",
				panel: "foo",
				index: i,
				hidden: i !== s.index
			}))
		});
		const nn = el.janillas?.originalChildNodes;
		if (nn)
			el.append(...el.janillas.originalChildNodes);
		Array.prototype.forEach.call(el.firstElementChild.children, (x, i) => {
			if (i > 0)
				x.appendChild(this.closest("admin-panel").tabsPanel(tt[i - 1]));
		});
		this.appendChild(el);
	}
}

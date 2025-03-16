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

	static get observedAttributes() {
		return ["data-active-tab", "data-name", "data-tab"];
	}

	static get templateName() {
		return "tabs";
	}

	constructor() {
		super();
		this.attachShadow({ mode: "open" });
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
		const b = event.composedPath().find(x => x instanceof Element && x.matches("button"));
		if (!b)
			return;
		event.stopPropagation();
		const s = this.state;
		const i = Array.prototype.findIndex.call(b.parentElement.children, x => x === b);
		const t = s.tabs[i];
		if (this.dispatchEvent(new CustomEvent("select-tab", {
			bubbles: true,
			cancelable: true,
			detail: { tab: t }
		}))) {
			s.activeTab = t;
			this.requestUpdate();
		}
	}

	async updateDisplay() {
		const s = this.state;
		s.tabs = this.dataset.tabs.split(",");
		s.activeTab = this.dataset.activeTab ?? s.tabs[0];
		this.shadowRoot.appendChild(this.interpolateDom({
			$template: "",
			buttons: s.tabs.map(x => ({
				$template: "tabs-button",
				panel: this.dataset.name,
				tab: x,
				selected: `${x === s.activeTab}`
			})),
			panels: s.tabs.map(x => ({
				$template: "tabs-panel",
				panel: this.dataset.name,
				tab: x,
				hidden: x !== s.activeTab
			}))
		}));
	}
}

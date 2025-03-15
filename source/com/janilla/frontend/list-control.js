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

export default class ListControl extends UpdatableHTMLElement {

	static get observedAttributes() {
		return ["data-key", "data-path"];
	}

	static get templateName() {
		return "list-control";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("change", this.handleChange);
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("change", this.handleChange);
		this.removeEventListener("click", this.handleClick);
	}

	handleChange = event => {
		const s = this.state;
		if (event.target.matches("select")) {
			event.stopPropagation();
			const li = event.target.closest("li");
			const i = Array.prototype.indexOf.call(li.parentElement.children, li);
			s.field.data.splice(i, 1);
			s.keys.splice(i, 1);
			this.requestUpdate();
		} else if (event.target.matches('[type="radio"]')) {
			event.stopPropagation();
			event.target.closest("dialog").close();
			s.field.data.push({ $type: event.target.value });
			s.keys.push(s.nextKey++);
			this.requestUpdate();
		}
	}

	handleClick = event => {
		if (!event.target.matches('[type="button"]'))
			return;
		event.stopPropagation();
		this.querySelector(":scope > dialog").showModal();
	}

	async updateDisplay() {
		const p = this.dataset.path;
		const s = this.state;
		s.field ??= (() => {
			const af = this.closest("admin-panel");
			return af.field(p);
		})();
		s.nextKey ??= s.field.data?.length ?? 0;
		s.keys ??= Array.from({ length: s.nextKey }, (_, i) => i);
		this.appendChild(this.interpolateDom({
			$template: "",
			items: s.field.data?.map((x, i) => ({
				$template: "item",
				type: x.$type,
				field: {
					$template: "object",
					path: `${p}.${i}`,
					key: s.keys[i]
				}
			})),
			types: s.field.elementTypes.map(x => ({
				$template: "type",
				name: x,
				checked: false
			}))
		}));
	}
}

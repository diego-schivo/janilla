/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Diego Schivo
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
import WebComponent from "./web-component.js";

export default class TagsInput extends WebComponent {

	static get observedAttributes() {
		return ["data-values"];
	}

	static get templateNames() {
		return ["tags-input"];
	}

	constructor() {
		super();
	}

	get values() {
		return this.dataset.values?.split(",") ?? [];
	}

	set values(x) {
		this.dataset.values = x.join();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
		this.addEventListener("keydown", this.handleKeyDown);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
		this.removeEventListener("keydown", this.handleKeyDown);
	}

	async updateDisplay() {
		this.appendChild(this.interpolateDom({
			$template: "",
			tags: this.values.map(value => ({
				$template: "tag",
				value
			}))
		}));
	}

	handleClick = event => {
		const el = event.target.closest(".ion-close-round");
		if (el) {
			event.preventDefault();
			const x = el.nextElementSibling.textContent;
			this.values = this.values.filter(y => y !== x);
		}
	}

	handleKeyDown = event => {
		if (event.key === "Enter") {
			event.preventDefault();
			const el = event.target;
			if (!this.values.includes(el.value))
				this.values = [...this.values, el.value];
			el.value = "";
		}
	}
}

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

export default class PaginationNav extends WebComponent {

	static get observedAttributes() {
		return ["data-pages-count", "data-page-number"];
	}

	static get templateNames() {
		return ["pagination-nav"];
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

	async updateDisplay() {
		this.appendChild(this.interpolateDom({
			$template: "",
			items: (() => {
				const pc = this.dataset.pagesCount ? parseInt(this.dataset.pagesCount) : 0;
				const pn = this.dataset.pageNumber ? parseInt(this.dataset.pageNumber) : 1;
				return pc > 1 ? Array.from({ length: pc }, (_, i) => ({
					$template: "item",
					active: i + 1 === pn ? "active" : null,
					number: i + 1
				})) : null;
			})()
		}));
	}

	handleClick = event => {
		const el = event.target.closest(".page-item");
		if (!el)
			return;
		event.preventDefault();
		event.stopPropagation();
		if (!el.classList.contains("active"))
			this.dispatchEvent(new CustomEvent("select-page", {
				bubbles: true,
				detail: { pageNumber: parseInt(el.textContent.trim()) }
			}));
	}
}

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

export default class FollowButton extends WebComponent {

	static get observedAttributes() {
		return ["data-active", "data-username"];
	}

	static get templateNames() {
		return ["follow-button"];
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
		const a = this.dataset.active != null;
		this.appendChild(this.interpolateDom({
			$template: "",
			...this.dataset,
			secondary: a ? "btn-secondary" : "btn-outline-secondary",
			text: `${a ? "Unfollow" : "Follow"} ${this.dataset.username}`
		}));
	}

	handleClick = async event => {
		event.stopPropagation();
		const { dataset: { apiUrl }, customState: { apiHeaders, user } } = this.closest("app-element");
		if (user) {
			const r = await fetch(`${apiUrl}/profiles/${this.dataset.username}/follow`, {
				method: this.dataset.active != null ? "DELETE" : "POST",
				headers: apiHeaders
			});
			if (r.ok) {
				const o = await r.json();
				this.dispatchEvent(new CustomEvent("toggle-follow", {
					bubbles: true,
					detail: o
				}));
			}
		} else
			location.hash = "#/login";
	}
}

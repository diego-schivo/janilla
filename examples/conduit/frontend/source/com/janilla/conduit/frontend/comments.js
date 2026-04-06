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

export default class Comments extends WebComponent {

	static get templateNames() {
		return ["comments"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
		this.removeEventListener("submit", this.handleSubmit);
	}

	async updateDisplay() {
		const { customState: { user } } = this.closest("app-element");
		this.appendChild(this.interpolateDom({
			$template: "",
			form: user ? {
				$template: "authenticated",
				...user,
				errorMessages: this.customState.errorMessages
			} : { $template: "unauthenticated" },
			cards: history.state.comments.map(x => ({
				$template: "card",
				...x,
				modOptions: x.author.username === user?.username
					? { $template: "mod-options" }
					: null
			}))
		}));
	}

	handleClick = async event => {
		if (event.target.matches(".ion-trash-a")) {
			event.preventDefault();
			const el = event.target.closest(".card");
			const els = el.parentElement.querySelectorAll(":scope > .card");
			const i = Array.prototype.findIndex.call(els, x => x === el);
			const hs = history.state;
			const c = hs.comments[i];
			const { dataset: { apiUrl }, customState: { apiHeaders } } = this.closest("app-element");
			const r = await fetch(`${apiUrl}/articles/${hs.article.slug}/comments/${c.id}`, {
				method: "DELETE",
				headers: apiHeaders
			});
			if (r.ok)
				this.dispatchEvent(new CustomEvent("remove-comment", {
					bubbles: true,
					detail: { comment: c }
				}));
		}
	}

	handleSubmit = async event => {
		event.preventDefault();
		const { dataset: { apiUrl }, customState: { apiHeaders } } = this.closest("app-element");
		const r = await fetch(`${apiUrl}/articles/${history.state.article.slug}/comments`, {
			method: "POST",
			headers: {
				...apiHeaders,
				"Content-Type": "application/json"
			},
			body: JSON.stringify({ comment: Object.fromEntries(new FormData(event.target)) })
		});
		if (r.ok) {
			event.target.elements["body"].value = "";
			const { comment } = await r.json();
			this.dispatchEvent(new CustomEvent("add-comment", {
				bubbles: true,
				detail: { comment }
			}));
		} else {
			const o = await r.json();
			this.customState.errorMessages = Object.entries(o).flatMap(([k, v]) => v.map(x => `${k} ${x}`));
			this.requestDisplay();
		}
	}
}

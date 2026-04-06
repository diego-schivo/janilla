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

export default class Editor extends WebComponent {

	static get observedAttributes() {
		return ["data-slug", "slot"];
	}

	static get templateNames() {
		return ["editor"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("submit", this.handleSubmit);
	}

	async updateDisplay() {
		const hs = history.state ?? {};
		if (hs.article) {
			this.appendChild(this.interpolateDom({
				$template: "",
				...hs.article,
				tagList: hs.article.tagList?.join(),
				errorMessages: this.customState.errorMessages?.join(";")
			}));
		} else {
			if (this.dataset.slug) {
				const { dataset: { apiUrl }, customState: { apiHeaders } } = this.closest("app-element");
				const { article } = await (await fetch(`${apiUrl}/articles/${this.dataset.slug}`, { headers: apiHeaders })).json();
				hs.article = article;
			} else
				hs.article = {};
			history.replaceState(hs, "");
			dispatchEvent(new CustomEvent("popstate"));
		}
	}

	handleSubmit = async event => {
		event.preventDefault();

		const a = [...new FormData(event.target).entries()].reduce((x, y) => {
			switch (y[0]) {
				case "tagList":
					x.tagList ??= [];
					x.tagList.push(y[1]);
					break;
				default:
					x[y[0]] = y[1];
					break;
			}
			return x;
		}, {});
		const { dataset: { apiUrl }, customState: { apiHeaders } } = this.closest("app-element");
		const r = await fetch(new URL([apiUrl, "articles", this.dataset.slug].filter(x => x).join("/"), location.href), {
			method: this.dataset.slug ? "PUT" : "POST",
			headers: {
				...apiHeaders,
				"Content-Type": "application/json"
			},
			body: JSON.stringify({ article: a })
		});

		if (r.ok) {
			const { article } = await r.json();
			location.hash = `#/article/${article.slug}`;
		} else {
			const o = await r.json();
			this.customState.errorMessages = Object.entries(o).flatMap(([k, v]) => v.map(x => `${k} ${x}`));
			this.requestDisplay();
		}
	}
}

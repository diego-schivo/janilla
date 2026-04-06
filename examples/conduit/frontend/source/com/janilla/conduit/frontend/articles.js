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

export default class ArticleList extends WebComponent {

	static get observedAttributes() {
		return ["data-api-url"];
	}

	static get templateNames() {
		return ["articles"];
	}

	constructor() {
		super();
		this.attachShadow({ mode: "open" });
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("select-page", this.handleSelectPage);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("select-page", this.handleSelectPage);
	}

	attributeChangedCallback(name, oldValue, newValue) {
		if (name === "data-api-url" && newValue !== oldValue)
			history.replaceState({
				...history.state,
				articles: null
			}, "");
		super.attributeChangedCallback(name, oldValue, newValue);
	}

	async updateDisplay() {
		const hs = history.state ?? {};
		const f = this.interpolateDom(!hs.articles
			? {
				$template: "",
				loadingSlot: "content"
			}
			: !hs.articles.length
				? {
					$template: "",
					emptySlot: "content"
				}
				: {
					$template: "",
					slot: "content",
					previews: hs.articles.map(({ slug }, index) => ({
						$template: "preview",
						index,
						slug
					})),
					pagination: hs.articlesCount > 10 ? {
						$template: "pagination",
						pagesCount: Math.ceil(hs.articlesCount / 10),
						pageNumber: hs.articlesPage
					} : null
				});
		this.shadowRoot.append(...f.querySelectorAll("slot"));
		this.appendChild(f);

		if (!hs.articles) {
			const u = new URL(this.dataset.apiUrl, location.href);
			u.searchParams.append("skip", ((hs.articlesPage ?? 1) - 1) * 10);
			u.searchParams.append("limit", 10);
			const { customState: { apiHeaders } } = this.closest("app-element");
			const { articles, articlesCount } = await (await fetch(u, { headers: apiHeaders })).json();
			history.replaceState({
				...history.state,
				articles,
				articlesCount
			}, "");
			this.requestDisplay(0);
		}
	}

	handleSelectPage = event => {
		history.pushState({
			...history.state,
			articles: null,
			articlesPage: event.detail.pageNumber
		}, "");
		this.requestDisplay();
	}
}

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

export default class Home extends WebComponent {

	static get templateNames() {
		return ["home"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
		this.addEventListener("select-tag", this.handleSelectTag);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
		this.removeEventListener("select-tag", this.handleSelectTag);
	}

	async updateDisplay() {
		const hs = history.state ?? {};
		const { dataset: { apiUrl }, customState: { user } } = this.closest("app-element");
		if (!hs.tab) {
			hs.tab = user ? "feed" : "all";
			history.replaceState(hs, "");
		}
		this.appendChild(this.interpolateDom({
			$template: "",
			banner: user ? null : { $template: "banner" },
			tabItems: [user ? {
				href: "#feed",
				text: "Your Feed"
			} : null, {
				href: "#all",
				text: "Global Feed"
			}, hs.tab === "tag" ? {
				href: "#tag",
				icon: "ion-pound",
				text: hs.tag
			} : null].filter(x => x).map(x => ({
				$template: "tab-item",
				...x,
				active: x.href.substring(1) === hs.tab ? "active" : null,
			})),
			articlesUrl: (() => {
				const x = new URL([apiUrl, "articles", hs.tab === "feed" ? "feed" : null].filter(x => x).join("/"), location.href);
				if (hs.tab === "tag")
					x.searchParams.append("tag", hs.tag);
				return x;
			})()
		}));
	}

	handleClick = event => {
		const el = event.target.closest("nav-link");
		if (el) {
			event.preventDefault();
			event.stopPropagation();
			if (!el.classList.contains("active")) {
				history.pushState({
					...history.state,
					tab: el.dataset.href.substring(1),
					tag: null
				}, "");
				this.requestDisplay();
			}
		}
	}

	handleSelectTag = event => {
		const { tag } = event.detail;
		history.pushState({
			...history.state,
			tab: "tag",
			tag,
			articlesPage: undefined
		}, "");
		console.log(history.state);
		this.requestDisplay();
	}
}

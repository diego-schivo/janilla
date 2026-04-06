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

export default class Profile extends WebComponent {

	static get observedAttributes() {
		return ["data-username", "slot"];
	}

	static get templateNames() {
		return ["profile"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
		this.addEventListener("toggle-follow", this.handleToggleFollow);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
		this.removeEventListener("toggle-follow", this.handleToggleFollow);
	}

	async updateDisplay() {
		const hs = history.state ?? {};
		const { dataset: { apiUrl }, customState: { apiHeaders, user } } = this.closest("app-element");
		if (hs.profile) {
			this.appendChild(this.interpolateDom({
				$template: "",
				...hs.profile,
				action: hs.profile.username === user?.username
					? { $template: "can-modify" }
					: {
						$template: "cannot-modify",
						...hs.profile
					},
				tabItems: [{
					href: "#author",
					text: "My Articles"
				}, {
					href: "#favorited",
					text: "Favorited Articles"
				}].map(x => ({
					$template: "tab-item",
					...x,
					active: x.href.substring(1) === hs.profileTab ? "active" : null
				})),
				articlesUrl: (() => {
					const x = new URL(`${apiUrl}/articles`, location.href);
					x.searchParams.append(hs.profileTab, hs.profile.username);
					return x;
				})()
			}));
		} else {
			const { profile } = await (await fetch(`${apiUrl}/profiles/${this.dataset.username}`, { headers: apiHeaders })).json();
			history.replaceState({
				...history.state,
				profile,
				profileTab: "author"
			}, "");
			dispatchEvent(new CustomEvent("popstate"));
		}
	}

	handleClick = event => {
		const el = event.target.closest("nav-link");
		if (el) {
			event.preventDefault();
			event.stopPropagation();
			if (!el.classList.contains("active")) {
				history.pushState({
					...history.state,
					profileTab: el.dataset.href.substring(1)
				}, "");
				this.requestDisplay();
			}
		}
	}

	handleToggleFollow = event => {
		const { profile } = event.detail;
		history.replaceState({
			...history.state,
			profile
		}, "");
		this.requestDisplay();
	}
}

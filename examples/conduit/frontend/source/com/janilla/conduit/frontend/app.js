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

export default class App extends WebComponent {

	static get templateNames() {
		return ["app"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		addEventListener("popstate", this.handlePopState);
		this.addEventListener("click", this.handleClick);
		this.addEventListener("set-current-user", this.handleSetCurrentUser);
		if (!location.hash)
			location.hash = "#/";
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		removeEventListener("popstate", this.handlePopState);
		this.removeEventListener("click", this.handleClick);
		this.removeEventListener("set-current-user", this.handleSetCurrentUser);
	}

	async updateDisplay() {
		const s = this.customState;
		if (!Object.hasOwn(s, "user")) {
			const t = localStorage.getItem("jwtToken");
			if (t) {
				const { user } = await (await fetch(`${this.dataset.apiUrl}/user`, {
					headers: { Authorization: `Token ${t}` }
				})).json();
				s.user = user;
			} else
				s.user = null;
			s.apiHeaders = s.user?.token ? { Authorization: `Token ${s.user.token}` } : {};
		}
		const p = location.hash.substring(1);
		const nn = p.split("/");
		const hs = history.state ?? {};
		this.appendChild(this.interpolateDom({
			$template: "",
			header: ({
				$template: "header",
				navItems: (() => {
					const ii = [{
						href: "#/",
						text: "Home"
					}];
					const u = s.user;
					if (u)
						ii.push({
							href: "#/editor",
							icon: "ion-compose",
							text: "New Article"
						}, {
							href: "#/settings",
							icon: "ion-gear-a",
							text: "Settings"
						}, {
							href: `#/@${u.username}`,
							image: u.image,
							text: u.username
						});
					else
						ii.push({
							href: "#/login",
							text: "Sign in"
						}, {
							href: "#/register",
							text: "Sign up"
						});
					return ii.map(x => ({
						$template: "nav-item",
						...x,
						active: x.href === location.hash ? "active" : null,
					}));
				})()
			}),
			path: p,
			loading: (() => {
				switch (nn[1]) {
					case "article":
					case "editor":
						return !hs.article;
					default:
						if (nn[1]?.startsWith("@"))
							return !hs.profile;
						return false;
				}
			})(),
			footer: { $template: "footer" }
		}));
	}

	handleClick = event => {
		const a = event.composedPath().find(x => x instanceof Element && x.matches("a"));
		if (!a?.href)
			return;
		event.preventDefault();
		location.hash = new URL(a.href).hash;
	}

	handlePopState = _ => {
		this.requestDisplay();
	}

	handleSetCurrentUser = event => {
		const { user } = event.detail;
		const s = this.customState;
		s.user = user;
		if (user?.token) {
			localStorage.setItem("jwtToken", user.token);
			s.apiHeaders = { Authorization: `Token ${user.token}` };
		} else {
			localStorage.removeItem("jwtToken");
			s.apiHeaders = {};
		}
		location.hash = "#/";
	}
}

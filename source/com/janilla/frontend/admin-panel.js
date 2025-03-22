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

export default class AdminPanel extends UpdatableHTMLElement {

	static get observedAttributes() {
		return ["data-path"];
	}

	static get templateName() {
		return "admin-panel";
	}

	dateTimeFormat = new Intl.DateTimeFormat("en-US", {
		dateStyle: "medium",
		timeStyle: "medium"
	});

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
		this.addEventListener("select-tab", this.handleSelectTab);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
		this.removeEventListener("select-tab", this.handleSelectTab);
	}

	attributeChangedCallback(name, oldValue, newValue) {
		const s = this.state;
		if (newValue !== oldValue && s)
			delete s.computeState;
		super.attributeChangedCallback(name, oldValue, newValue);
	}

	handleClick = async event => {
		const b = event.target.closest("button");
		if (!b?.name)
			return;
		event.stopPropagation();
		switch (b.name) {
			case "open-menu":
				this.querySelector("dialog").showModal();
				break;
			case "close-menu":
				this.querySelector("dialog").close();
				break;
			case "logout":
				await fetch("/api/users/logout", { method: "POST" });
				//b.closest("dialog").close();
				location.href = "/admin";
				break;
		}
	}

	handleSelectTab = event => {
		event.preventDefault();
		const t = event.detail.tab;
		const s = this.state;
		const nn = s.pathSegments.slice(0, 3);
		if (t !== "edit")
			nn.push(t);
		history.pushState(undefined, "", `/admin/${nn.join("/")}`);
		dispatchEvent(new CustomEvent("popstate"));
	}

	async computeState() {
		const s = this.state;
		[
			"pathSegments",
			"collectionSlug",
			"globalSlug",
			"entityType",
			"entityUrl",
			"documentSubview",
			"versionId",
			"entity",
			"versions",
			"version"
		].forEach(x => delete s[x]);
		s.me ??= await (await fetch("/api/users/me")).json();
		let p = this.dataset.path;
		if (p === "/account")
			p = `/collections/users/${s.me.id}`;
		s.pathSegments = p.length > 1 ? p.substring(1).split("/") : [];
		if (!s.me) {
			if (!["/login", "/create-first-user"].includes(p)) {
				history.pushState(undefined, "", "/admin/login");
				dispatchEvent(new CustomEvent("popstate"));
				return;
			}
		} else {
			if (["/login", "/create-first-user"].includes(p)) {
				history.pushState(undefined, "", "/admin");
				dispatchEvent(new CustomEvent("popstate"));
				return;
			}

			s.schema ??= await (await fetch("/api/schema")).json();

			if (s.pathSegments[0] === "collections") {
				s.collectionSlug = s.pathSegments[1];
				s.entityType = s.schema["Collections"][s.pathSegments[1].split("-").map((y, i) => i ? y.charAt(0).toUpperCase() + y.substring(1) : y).join("")].elementTypes[0];
				s.entityUrl = s.pathSegments.length >= 3 ? `/api/${s.collectionSlug}/${s.pathSegments[2]}` : null;
			}
			if (s.pathSegments[0] === "globals") {
				s.globalSlug = s.pathSegments[1];
				s.entityType = s.schema["Globals"][s.pathSegments[1].split("-").map((y, i) => i ? y.charAt(0).toUpperCase() + y.substring(1) : y).join("")].type;
				s.entityUrl = s.pathSegments.length >= 2 ? `/api/${s.globalSlug}` : null;
			}

			s.documentSubview = s.entityUrl ? s.pathSegments[s.collectionSlug ? 3 : 2] ?? "default" : null;
			s.versionId = s.documentSubview === "versions" ? s.pathSegments[s.collectionSlug ? 4 : 3] : null;
			if (s.versionId) {
				s.documentSubview = "version";
				s.versionId = parseInt(s.versionId);
			}
			await Promise.all([
				s.entityUrl ? fetch(s.entityUrl).then(async x => s.entity = await x.json()) : null,
				s.documentSubview === "versions" ? fetch(`${s.entityUrl}/versions`).then(async x => s.versions = await x.json()) : null,
				s.documentSubview === "version" ? fetch(`${s.entityUrl.substring(0, s.entityUrl.lastIndexOf("/"))}/versions/${s.versionId}`).then(async x => s.version = await x.json()) : null
			]);
		}
		this.requestUpdate();
	}

	async updateDisplay() {
		const s = this.state;
		s.computeState ??= this.computeState();
		this.appendChild(this.interpolateDom({
			$template: "",
			header: s.me ? {
				$template: "header",
				items: (() => {
					const xx = [];
					xx.push({
						href: "/admin",
						icon: "house"
					});
					switch (s.pathSegments[0]) {
						case "collections":
							xx.push({
								href: `/admin/collections/${s.pathSegments[1]}`,
								text: s.pathSegments[1]
							});
							if (s.pathSegments[2]) {
								const h = `/admin/collections/${s.pathSegments[1]}/${s.pathSegments[2]}`;
								xx.push({
									href: h,
									text: (s.entity ? this.title(s.entity) : null) ?? s.pathSegments[2]
								});
								if (s.documentSubview !== "default")
									xx.push({
										href: `${h}/${s.documentSubview === "version" ? "versions" : s.documentSubview}`,
										text: s.documentSubview === "version" ? "versions" : s.documentSubview
									});
								if (s.versionId)
									xx.push({
										href: `${h}/versions/${s.versionId}`,
										text: s.version?.updatedAt ? this.dateTimeFormat.format(new Date(s.version.updatedAt)) : s.versionId
									});
							}
							break;
						case "globals":
							xx.push({
								href: `/admin/globals/${s.pathSegments[1]}`,
								text: s.pathSegments[1]
							});
							break;
					}
					delete xx[xx.length - 1].href;
					return xx;
				})().map(x => ({
					$template: x.href ? "link-item" : "item",
					...x,
					content: x.icon ? {
						$template: "icon",
						name: x.icon
					} : x.text
				}))
			} : null,
			content: (() => {
				switch (this.dataset.path) {
					case "/create-first-user":
						return { $template: "create-first-user" };
					case "/login":
						return { $template: "login" };
				}
				if (!s.pathSegments)
					return { $template: "loading" };
				switch (s.pathSegments[0]) {
					case "collections":
						return s.pathSegments.length === 2 ? {
							$template: "collection",
							name: s.pathSegments[1]
						} : s.entity ? {
							$template: "document",
							subview: s.documentSubview,
							updatedAt: s.entity.updatedAt
						} : { $template: "loading" };
					case "globals":
						return s.entity ? {
							$template: "document",
							subview: s.documentSubview,
							updatedAt: s.entity.updatedAt
						} : { $template: "loading" };
					default:
						return { $template: "dashboard" };
				}
			})(),
			dialog: s.me && s.schema ? {
				$template: "dialog",
				groups: Object.entries(s.schema["Data"]).map(([k, v]) => ({
					$template: "group",
					name: k,
					checked: true,
					links: Object.keys(s.schema[v.type]).map(x => ({
						$template: "link",
						href: `/admin/${k}/${x}`,
						name: x
					}))
				}))
			} : null
		}));
	}

	field(path, parent) {
		const s = this.state;
		let f = parent ?? {
			type: s.entityType,
			properties: s.schema[s.entityType],
			data: s.entity
		};
		if (path)
			for (const n of path.split("."))
				if (f.type === "List") {
					const i = parseInt(n);
					const d = f.data[i] ??= {};
					const t = d.$type ?? f.elementTypes[0];
					f = {
						parent: f,
						index: i,
						type: t,
						properties: s.schema[t],
						data: d
					};
				} else {
					const p = f.properties[n];
					f = {
						parent: f,
						name: n,
						...p,
						properties: p.type !== "List" && p.type !== "String" ? s.schema[p.type] : null,
						/*
						data: f.data[n] ??= (() => {
							switch (p.type) {
								case "List":
									return [];
								case "Long":
									return p.referenceType ? {} : null;
								default:
									return null;
							}
						})()
						*/
						data: f.data?.[n]
					};
				}
		return f;
	}

	label(path) {
		return path.split(".").at(-1).split(/(?=[A-Z])/).map(x => x.toLowerCase()).join(" ");
	}

	title(entity) {
		switch (entity.$type) {
			case "Media":
				return entity.file?.name;
			case "User":
				return entity.name;
			default:
				return entity.title;
		}
	}

	headers(entitySlug) {
		switch (entitySlug) {
			case "media":
				return ["file", "alt"];
			case "users":
				return ["name", "email"];
			default:
				return ["title"];
		}
	}

	controlTemplate(field) {
		switch (field.type) {
			case "Boolean":
				return "checkbox-control";
			case "File":
				return "file-control";
			case "Instant":
			case "Integer":
				return "text-control";
			case "String":
				return field.options ? "radio-group-control"
					: field.name === "slug" ? "slug-control" : "text-control";
			case "List":
				return field.referenceType ? "reference-list-control" : "list-control";
			case "Long":
				return field.referenceType ? "reference-control" : "text-control";
			default:
				return "object-control";
		}
	}

	options(field) {
		return field.options ?? [];
	}

	tabs(type) {
		return null;
	}

	preview(entity) {
		return null;
	}
}

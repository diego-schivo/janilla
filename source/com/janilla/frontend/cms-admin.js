/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
import WebComponent from "./web-component.js";

export default class CmsAdmin extends WebComponent {

	static get observedAttributes() {
		return ["data-email", "data-path"];
	}

	static get templateNames() {
		return ["cms-admin"];
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
		if (newValue !== oldValue) {
			const s = this.state;
			if (s?.computeState)
				delete s.computeState;
		}
		super.attributeChangedCallback(name, oldValue, newValue);
	}

	handleClick = async event => {
		const el = event.target.closest("button");
		if (el?.name) {
			event.stopPropagation();
			switch (el.name) {
				case "open-menu":
					this.querySelector("dialog").showModal();
					break;
				case "close-menu":
					this.querySelector("dialog").close();
					break;
				case "logout":
					await fetch(`${this.closest("app-element").dataset.apiUrl}/users/logout`, { method: "POST" });
					history.pushState({}, "", "/admin");
					dispatchEvent(new CustomEvent("popstate"));
					break;
			}
		}
	}

	handleSelectTab = event => {
		if (event.detail.name !== "document-subview")
			return;
		event.preventDefault();
		const v = event.detail.value;
		const s = this.state;
		const nn = s.pathSegments.slice(0, 3);
		if (v !== "edit")
			nn.push(v);
		history.pushState({}, "", `/admin/${nn.join("/")}`);
		dispatchEvent(new CustomEvent("popstate"));
	}

	async updateDisplay() {
		const hs = history.state;
		let s = hs.cmsAdmin
		const as = this.closest("app-element").state;
		if (!s) {
			let p = this.dataset.path;
			if (as.user && p === "/account")
				p = `/collections/users/${as.user.id}`;
			s = { pathSegments: p.length > 1 ? p.substring(1).split("/") : [] };
			this.foo(p, s);
		}
		const gg = as.user && s.schema ? Object.entries(s.schema["Data"]).map(([k, v]) => ({
			$template: "group",
			label: k.split(/(?=[A-Z])/).map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" "),
			checked: true,
			links: Object.keys(s.schema[v.type]).map(x => ({
				$template: "link",
				href: `/admin/${k}/${x}`,
				text: x.split(/(?=[A-Z])/).map(y => y.charAt(0).toUpperCase() + y.substring(1)).join(" ")
			}))
		})) : null;
		this.appendChild(this.interpolateDom({
			$template: "",
			p: as.user ? {
				$template: "p",
				checked: true
			} : null,
			aside: as.user && s.schema ? {
				$template: "aside",
				groups: gg
			} : null,
			header: as.user ? {
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
								text: s.pathSegments[1].split("-").map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" ")
							});
							if (s.pathSegments[2]) {
								const h = `/admin/collections/${s.pathSegments[1]}/${s.pathSegments[2]}`;
								let t = s.document ? this.title(s.document) : null;
								if (!t?.length)
									t = s.pathSegments[2];
								xx.push({
									href: h,
									text: t
								});
								if (s.documentSubview && s.documentSubview !== "default")
									xx.push({
										href: `${h}/${s.documentSubview === "version" ? "versions" : s.documentSubview}`,
										text: s.documentSubview === "version" ? "Versions" : s.documentSubview.split("-").map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" ")
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
						return { $template: "first-user" };
					case "/forgot":
						return { $template: "forgot-password" };
					case "/login":
						return { $template: "login" };
					case "/unauthorized":
						return { $template: "unauthorized" };
				}
				if (this.dataset.path.startsWith("/reset/"))
					return {
						$template: "reset-password",
						token: this.dataset.path.substring("/reset/".length)
					};
				if (!s.pathSegments)
					return { $template: "loading" };
				switch (s.pathSegments[0]) {
					case "collections":
						return s.pathSegments.length === 2 ? {
							$template: "collection",
							name: s.pathSegments[1]
						} : s.document ? {
							$template: "document",
							subview: s.documentSubview,
							document: s.document
						} : { $template: "loading" };
					case "globals":
						return s.document ? {
							$template: "document",
							subview: s.documentSubview,
							document: s.document
						} : { $template: "loading" };
					default:
						return { $template: s.schema ? "dashboard" : "loading" };
				}
			})(),
			dialog: as.user && s.schema ? {
				$template: "dialog",
				groups: gg
			} : null
		}));
	}

	async foo(p, s) {
		const a = this.closest("app-element");
		const as = a.state;
		let hs = history.state;
		if (as.user) {
			if (["/create-first-user", "/login", "/forgot"].includes(p) || p.startsWith("/reset/")) {
				history.pushState({}, "", "/admin");
				dispatchEvent(new CustomEvent("popstate"));
				return;
			}

			if (p === "/logout") {
				await fetch(`${a.dataset.apiUrl}/users/logout`, { method: "POST" });
				//delete this.closest("app-element").state.user;
				history.pushState({}, "", "/admin");
				dispatchEvent(new CustomEvent("popstate"));
				return;
			}

			if (p !== "/unauthorized" && !as.user.roles?.some(x => x.name === "ADMIN")) {
				history.pushState({}, "", "/admin/unauthorized");
				dispatchEvent(new CustomEvent("popstate"));
				return;
			}

			s.schema = await (await fetch(`${a.dataset.apiUrl}/schema`)).json();

			if (s.pathSegments[0] === "collections") {
				s.collectionSlug = s.pathSegments[1];
				s.documentType = s.schema["Collections"][s.pathSegments[1].split("-").map((y, i) => i ? y.charAt(0).toUpperCase() + y.substring(1) : y).join("")].elementTypes[0];
				s.documentUrl = s.pathSegments.length >= 3 ? `${a.dataset.apiUrl}/${s.collectionSlug}/${s.pathSegments[2]}` : null;
			}
			if (s.pathSegments[0] === "globals") {
				s.globalSlug = s.pathSegments[1];
				s.documentType = s.schema["Globals"][s.pathSegments[1].split("-").map((y, i) => i ? y.charAt(0).toUpperCase() + y.substring(1) : y).join("")].type;
				s.documentUrl = s.pathSegments.length >= 2 ? `${a.dataset.apiUrl}/${s.globalSlug}` : null;
			}

			s.documentSubview = s.documentUrl ? s.pathSegments[s.collectionSlug ? 3 : 2] ?? "default" : null;
			s.versionId = s.documentSubview === "versions" ? s.pathSegments[s.collectionSlug ? 4 : 3] : null;
			if (s.versionId) {
				s.documentSubview = "version";
				s.versionId = parseInt(s.versionId);
			}
			await Promise.all([
				s.documentUrl ? fetch(s.documentUrl).then(async x => s.document = x.ok ? await x.json() : { $type: s.documentType }) : null,
				s.documentSubview === "versions" ? fetch(`${s.documentUrl}/versions`).then(async x => s.versions = await x.json()) : null,
				s.documentSubview === "version" ? fetch(`${s.collectionSlug ? s.documentUrl.substring(0, s.documentUrl.lastIndexOf("/")) : s.documentUrl}/versions/${s.versionId}`).then(async x => s.version = await x.json()) : null
			]);
		} else if (!["/create-first-user", "/forgot", "/login", "/reset"].includes(p) && !p.startsWith("/reset/")) {
			//location.href = "/admin/login";
			console.log("/admin/login");
			return;
		}
		history.replaceState(hs = {
			...hs,
			cmsAdmin: s
		}, "");
		this.requestDisplay();
	}

	field(path, parent) {
		const hs = history.state;
		const s = hs.cmsAdmin;
		let f = parent ?? {
			type: s.documentType,
			properties: s.schema[s.documentType],
			data: s.document
		};
		if (path)
			for (const n of path.split("."))
				if (f.type === "List") {
					const i = parseInt(n);
					const d = f.data?.[i];
					const t = d?.$type;
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
						properties: p && !["List", "String"].includes(p.type) ? s.schema[p.type] : null,
						data: f.data?.[n]
					};
				}
		//console.log("CmsAdmin.field", path, f);
		return f;
	}

	initField(field) {
		if (field.data)
			return;
		this.initField(field.parent);
		field.parent.data[Array.isArray(field.parent.data) ? field.index : field.name] = field.data = field.type === "List" ? [] : {};
	}

	label(path) {
		return path.split(".").at(-1).split(/(?=[A-Z])/).map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" ");
	}

	title(document) {
		const hs = history.state;
		const s = hs.cmsAdmin;
		if (Object.values(s.schema.Globals ?? {}).some(x => x.type === document.$type))
			return document.$type.split(/(?=[A-Z])/).join(" ");
		switch (document.$type) {
			case "Media":
				return document.file?.name;
			case "User":
				return document.name;
			default:
				return document.title;
		}
	}

	headers(documentSlug) {
		switch (documentSlug) {
			case "media":
				return ["file", "alt", "caption", "updatedAt"];
			case "users":
				return ["name", "email"];
			default:
				return ["title"];
		}
	}

	cell(object, key) {
		const x = object[key];
		if (key === "updatedAt")
			return this.dateTimeFormat.format(new Date(x));
		if (typeof x === "object" && x?.$type === "File")
			return {
				$template: "media",
				...object
			};
		return x;
	}

	controlTemplate(field) {
		switch (field.type) {
			case "BigDecimal":
			case "Integer":
			case "UUID":
				return "text";
			case "Boolean":
				return "checkbox";
			case "Document.Reference":
				return "document-reference";
			case "File":
				return "file";
			case "Instant":
				return "datetime";
			case "List":
				return field.referenceType ? "reference-array" : "array";
			case "Long":
				return field.referenceType ? "reference" : "text";
			case "Set":
				return "checkbox2";
			case "String":
				return field.options
					? "select"
					: field.name === "slug"
						? "slug"
						: "text";
			default:
				return "object";
		}
	}

	options(field) {
		return field.options ?? [];
	}

	tabs(type) {
		return null;
	}

	preview(document) {
		return null;
	}

	sidebar(type) {
		return null;
	}

	renderToast() {
		const el = this.querySelector("cms-toasts");
		el.renderToast.apply(el, arguments);
	}

	isReadOnly(type) {
		return false;
	}

	setValue(object, key, value, field) {
		if (field.type === "Set") {
			if (Object.hasOwn(object, key))
				object[key].push(value);
			else
				object[key] = [value];
		} else
			object[key] = value instanceof File ? { name: value.name } : value;
	}

	formProperties(field) {
		return field.properties ? Object.entries(field.properties).filter(([k, _]) => k !== "id") : [];
	}
}

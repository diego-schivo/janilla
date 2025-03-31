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
import { WebComponent } from "./web-component.js";

export default class CmsAdmin extends WebComponent {

	static get observedAttributes() {
		return ["data-email", "data-path"];
	}

	static get templateName() {
		return "cms-admin";
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
		if (newValue !== oldValue && this.state)
			delete this.state.computeState;
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
				delete this.state.me;
				history.pushState(undefined, "", "/admin");
				dispatchEvent(new CustomEvent("popstate"));
				break;
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
		if (this.dataset.email !== s.me?.email) {
			this.closest("root-element").requestDisplay();
			return;
		}
		let p = this.dataset.path;
		if (s.me && p === "/account")
			p = `/collections/users/${s.me.id}`;
		s.pathSegments = p.length > 1 ? p.substring(1).split("/") : [];
		if (!s.me) {
			if (!["/create-first-user", "/login", "/forgot", "/reset"].includes(p) && !p.startsWith("/reset/")) {
				history.pushState(undefined, "", "/admin/login");
				dispatchEvent(new CustomEvent("popstate"));
				return;
			}
		} else {
			if (["/create-first-user", "/login", "/forgot"].includes(p) || p.startsWith("/reset/")) {
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
		this.requestDisplay();
	}

	async updateDisplay() {
		const s = this.state;
		s.computeState ??= this.computeState();
		this.appendChild(this.interpolateDom({
			$template: "",
			aside: s.me && s.schema ? {
				$template: "aside",
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
			} : null,
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
								let t = s.entity ? this.title(s.entity) : null;
								if (!t?.length)
									t = s.pathSegments[2];
								xx.push({
									href: h,
									text: t
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
						return { $template: "first-user" };
					case "/login":
						return { $template: "login" };
					case "/forgot":
						return { $template: "forgot-password" };
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
						return { $template: s.schema ? "dashboard" : "loading" };
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
					const d = f.data?.[i];
					const t = d?.$type; // ?? f.elementTypes[0];
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
						data: f.data?.[n]
					};
				}
		return f;
	}

	initField(field) {
		if (field.data)
			return;
		this.initField(field.parent);
		field.parent.data[Array.isArray(field.parent.data) ? field.index : field.name] = field.data = field.type === "List" ? [] : {};
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
				return ["file", "alt", "caption", "updatedAt"];
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
				return "cms-file";
			case "Instant":
			case "Integer":
				return "cms-text";
			case "String":
				return field.options ? "radio-group-control"
					: field.name === "slug" ? "cms-slug" : "cms-text";
			case "List":
				return field.referenceType ? "reference-list-control" : "cms-array";
			case "Long":
				return field.referenceType ? "cms-reference" : "cms-text";
			default:
				return "cms-object";
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

	sidebar(type) {
		return null;
	}
}

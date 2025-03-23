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
import { UpdatableHTMLElement } from "./updatable-html-element.js";

export default class CmsDocument extends UpdatableHTMLElement {

	static get observedAttributes() {
		return ["data-subview", "data-updated-at"];
	}

	static get templateName() {
		return "cms-document";
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const ap = this.closest("cms-admin");
		const s = ap.state;
		this.appendChild(this.interpolateDom({
			$template: "",
			title: (() => {
				let t = ap.title(s.entity);
				if (!t?.length)
					t = s.pathSegments[2];
				return t;
			})(),
			tabs: [
				"edit",
				Object.hasOwn(s.entity, "versionCount") ? "versions" : null,
				"api"
			].filter(x => x).join(),
			activeTab: (() => {
				switch (this.dataset.subview) {
					case "default":
						return "edit";
					case "version":
						return "versions";
					default:
						return this.dataset.subview;
				}
			})(),
			edit: {
				$template: "edit",
				panel: this.dataset.subview === "default" ? { $template: "edit-panel" } : null
			},
			versions: Object.hasOwn(s.entity, "versionCount") ? {
				$template: "versions",
				count: s.entity.versionCount,
				panel: ["versions", "version"].includes(this.dataset.subview) ? {
					$template: "versions-panel",
					versionsView: this.dataset.subview === "versions" ? { $template: "versions-view" } : null,
					versionView: this.dataset.subview === "version" ? { $template: "version-view" } : null
				} : null
			} : null,
			api: {
				$template: "api",
				panel: this.dataset.subview === "api" ? {
					$template: "api-panel",
					json: JSON.stringify(s.entity, null, "  ")
				} : null
			}
		}));
	}
}

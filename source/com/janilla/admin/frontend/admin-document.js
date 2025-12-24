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
 * Note that authoring this file involved dealing in other programs that are
 * provided under the following license:
 *
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
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
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
import WebComponent from "web-component";

export default class AdminDocument extends WebComponent {

	static get templateNames() {
		return ["admin-document"];
	}

	static get observedAttributes() {
		return ["data-subview", "data-updated-at"];
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const a = this.closest("admin-element");
		const as = a.state;
		this.appendChild(this.interpolateDom({
			$template: "",
			title: (() => {
				let t = a.title(as.document);
				if (!t?.length)
					t = as.pathSegments[2];
				return t;
			})(),
			tabs: [
				"edit",
				Object.hasOwn(as.document, "versionCount") ? "versions" : null,
				"api"
			].filter(x => x).join(),
			tab: (() => {
				switch (this.dataset.subview) {
					case "default":
						return "edit";
					case "version":
						return "versions";
					default:
						return this.dataset.subview;
				}
			})(),
			editButton: { $template: "edit-button" },
			versionsButton: Object.hasOwn(as.document, "versionCount") ? {
				$template: "versions-button",
				count: as.document.versionCount
			} : null,
			apiButton: { $template: "api-button" },
			editPanel: this.dataset.subview === "default" ? (() => {
				return {
					$template: "edit-panel",
					slug: as.pathSegments[1],
					id: as.pathSegments[2],
					updatedAt: this.dataset.updatedAt
				};
			})() : null,
			versionsPanel: ["versions", "version"].includes(this.dataset.subview) ? {
				$template: "versions-panel",
				versionsView: this.dataset.subview === "versions" ? { $template: "versions" } : null,
				versionView: this.dataset.subview === "version" ? { $template: "version" } : null
			} : null,
			apiPanel: this.dataset.subview === "api" ? {
				$template: "api-panel",
				json: JSON.stringify(as.document, null, "  ")
			} : null
		}));
	}
}

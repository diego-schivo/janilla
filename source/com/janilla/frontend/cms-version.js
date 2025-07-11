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

export default class CmsVersion extends WebComponent {

	static get templateNames() {
		return ["cms-version"];
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
	}

	handleClick = async event => {
		const b = event.target.closest("button");
		if (!b?.name)
			return;
		event.stopPropagation();
		switch (b.name) {
			case "cancel":
				this.querySelector("dialog").close();
				break;
			case "confirm":
				const a = this.closest("cms-admin");
				const s = a.state;
				s.document = await (await fetch(`${s.collectionSlug ? s.documentUrl.substring(0, s.documentUrl.lastIndexOf("/")) : s.documentUrl}/versions/${s.version.id}`, { method: "POST" })).json();
				a.renderToast("Restored successfully.");
				history.pushState(undefined, "", `/admin/${s.pathSegments.slice(0, 3).join("/")}`);
				dispatchEvent(new CustomEvent("popstate"));
				break;
			case "restore":
				this.querySelector("dialog").showModal();
				break;
		}
	}

	async updateDisplay() {
		const ap = this.closest("cms-admin");
		const s = ap.state;
		const ua = ap.dateTimeFormat.format(new Date(s.version.updatedAt));
		this.appendChild(this.interpolateDom({
			$template: "",
			title: ua,
			json: JSON.stringify(s.version.document, null, "  "),
			dialog: {
				$template: "dialog",
				dateTime: ua
			}
		}));
	}
}

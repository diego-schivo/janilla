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

export default class CmsTabs extends WebComponent {

	static get templateNames() {
		return ["cms-tabs"];
	}

	static get observedAttributes() {
		return ["data-active-tab", "data-name", "data-no-tab-list", "data-no-tab-panels", "data-tab"];
	}

	constructor() {
		super();
		this.attachShadow({ mode: "open" });
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("click", this.handleClick);
	}

	attributeChangedCallback(name, oldValue, newValue) {
		if (newValue !== oldValue && this.state)
			delete this.state.activeTab;
		super.attributeChangedCallback(name, oldValue, newValue);
	}

	async updateDisplay() {
		const s = this.state;
		s.tabs = this.dataset.tabs.split(",");
		s.activeTab ??= this.dataset.activeTab ?? s.tabs[0];
		this.shadowRoot.appendChild(this.interpolateDom({
			$template: "",
			tablist: this.dataset.noTabList === undefined ? {
				$template: "tablist",
				buttons: s.tabs.map(x => ({
					$template: "tabs-button",
					panel: this.dataset.name,
					tab: x,
					selected: `${x === s.activeTab}`
				}))
			} : null,
			panels: this.dataset.noTabPanels === undefined ? s.tabs.map(x => ({
				$template: "tabs-panel",
				panel: this.dataset.name,
				tab: x,
				hidden: x !== s.activeTab
			})) : null
		}));
	}

	handleClick = async event => {
		const b = event.composedPath().find(x => x instanceof Element && x.matches("button"));
		if (b?.role !== "tab")
			return;
		event.stopPropagation();
		const s = this.state;
		const i = Array.prototype.findIndex.call(b.parentElement.children, x => x === b);
		const t = s.tabs[i];
		if (this.dispatchEvent(new CustomEvent("select-tab", {
			bubbles: true,
			cancelable: true,
			detail: {
				name: this.dataset.name,
				value: t
			}
		}))) {
			s.activeTab = t;
			this.requestDisplay();
		}
	}
}

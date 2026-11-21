/*
 * Copyright (c) 2024, 2026, Diego Schivo. All rights reserved.
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
import WebComponent from "base/web-component";

export default class AdminTabs extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-tabs"];
    }

    static get observedAttributes() {
        return ["data-name", "data-tabs", "data-tab", "data-no-buttons", "data-no-panels"];
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
        this.removeEventListener("click", this.handleClick);

        super.disconnectedCallback();
    }

    attributeChangedCallback(name, oldValue, newValue) {
        if (newValue !== oldValue && this.customState)
            delete this.customState.tab;

        super.attributeChangedCallback(name, oldValue, newValue);
    }

    async updateDisplay() {
        const a = this.shadowClosest("app-element");
        const s = this.customState;
        s.tabs = this.dataset.tabs.split(",");
        s.tab ??= this.dataset.tab ?? s.tabs[0];
        this.shadowRoot.appendChild(this.interpolateDom({
            ...a.baseInput,
            $template: "",
            tablist: this.dataset.noButtons === undefined ? {
                $template: "tablist",
                buttons: s.tabs.map(x => ({
                    $template: "button",
                    panel: this.dataset.name,
                    tab: x,
                    selected: `${x === s.tab}`
                }))
            } : null,
            panels: this.dataset.noPanels === undefined ? s.tabs.map(x => ({
                $template: "panel",
                panel: this.dataset.name,
                tab: x,
                hidden: x !== s.tab
            })) : null
        }));
    }

    handleClick = event => {
        const b = event.composedPath().find(x => x instanceof Element && x.matches("button"));
        if (b?.role === "tab") {
            event.stopPropagation();
            const i = Array.prototype.findIndex.call(b.parentElement.children, x => x === b);
            const s = this.customState;
            if (this.dispatchEvent(new CustomEvent("tabselected", {
                bubbles: true,
                cancelable: true,
                detail: {
                    name: this.dataset.name,
                    value: s.tabs[i]
                }
            }))) {
                s.tab = s.tabs[i];
                this.requestDisplay();
            }
        }
    }
}

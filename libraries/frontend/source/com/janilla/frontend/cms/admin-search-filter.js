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

export default class AdminSearchFilter extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-search-filter"];
    }

    static get observedAttributes() {
        return ["data-search"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("input", this.handleInput);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("input", this.handleInput);
    }

    async updateDisplay() {
        const a2 = this.shadowClosest("admin-element");
        const l = this.closest("admin-list");
        this.appendChild(this.interpolateDom({
            $template: "",
            value: this.dataset.search,
            placeholder: `Search by ${a2.titleName(a2.documentType(l.dataset.slug))}`
        }));
    }

    handleInput = event => {
        const el = event.target.closest("input");
        if (el) {
            const s = this.customState;
            if (typeof s.inputTimeout !== "undefined")
                clearTimeout(s.inputTimeout);

            s.inputTimeout = setTimeout(() => {
                delete s.inputTimeout;
                this.dispatchEvent(new CustomEvent("searchchanged", {
                    bubbles: true,
                    detail: el.value
                }));
            }, 300);
        }
    }
}

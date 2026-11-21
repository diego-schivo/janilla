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

export default class AdminPagination extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-pagination"];
    }

    static get observedAttributes() {
        return ["data-page", "data-total-pages"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("click", this.handleClick);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("click", this.handleClick);
    }

    async updateDisplay() {
        const p = parseInt(this.dataset.page);
        const tp = parseInt(this.dataset.totalPages);
        this.appendChild(this.interpolateDom({
            $template: "",
            arrows: [-1, 1].map(x => ({
                $template: "arrow",
                page: p + x,
                disabled: x === -1 ? p === 1 : p === tp,
                icon: x === -1 ? "chevron-left" : "chevron-right"
            })),
            pages: Array.from({ length: tp }).map((_, x) => ({
                $template: "page",
                active: (x + 1) === p ? "active" : null,
                page: x + 1
            }))
        }));
    }

    handleClick = event => {
        const el = event.target.closest("button");
        if (el)
            this.dispatchEvent(new CustomEvent("pageselected", {
                bubbles: true,
                detail: parseInt(el.dataset.page)
            }));
    }
}

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

export default class AdminSlug extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-slug"];
    }

    static get observedAttributes() {
        return ["data-array-key", "data-path", "data-updated-at"];
    }

    connectedCallback() {
        super.connectedCallback();
        const s = this.customState;
        s.edit = this.closest("admin-edit");
        s.edit.addEventListener("input", this.handleInput);
    }

    disconnectedCallback() {
        const s = this.customState;
        s.edit.removeEventListener("input", this.handleInput);
        super.disconnectedCallback();
    }

    async updateDisplay() {
        const s = this.customState;
        const p = this.dataset.path;
        s.field ??= s.edit.field(p);
        this.appendChild(this.interpolateDom({
            $template: "",
            inputs: [{
                $template: "input",
                name: p,
                value: s.field.data
            }, {
                $template: "input",
                type: "hidden",
                name: p,
                value: s.field.data,
                disabled: true
            }]
        }));
    }

    handleInput = event => {
        const el = event.target.closest('input[name="title"]');
        if (el) {
            this.customState.field.data = el.value.split(/\W+/).filter(x => x).map(x => x.toLowerCase()).join("-");
            this.requestDisplay();
        }
    }
}

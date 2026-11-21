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

export default class AdminFile extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-file"];
    }

    static get observedAttributes() {
        return ["data-array-key", "data-path", "data-updated-at"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("change", this.handleChange);
        this.addEventListener("click", this.handleClick);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("change", this.handleChange);
        this.removeEventListener("click", this.handleClick);
    }

    async updateDisplay() {
        const s = this.customState;
        const p = this.dataset.path;
        s.field = this.closest("admin-edit").field(p);
        this.appendChild(this.interpolateDom({
            $template: "",
            content: s.field.data ? {
                $template: "update",
				src: `${this.shadowClosest("app-element").customEnv.apiUrl}/media/file/${s.field.data.name}`,
                data: s.field.data
            } : {
                $template: "create",
                name: p,
                dropzone: !s.file ? {
                    $template: "dropzone",
                    name: p
                } : null,
                input: s.file ? {
                    $template: "input",
                    value: s.file.name
                } : null
            }
        }));
    }

    handleChange = event => {
        const el = event.target.closest('[type="file"]');
        if (el) {
            this.customState.file = el.files[0];
            this.requestDisplay();
        }
    }

    handleClick = event => {
        const el = event.target.closest("button");
        const s = this.customState;
        switch (el?.name) {
            case "remove":
                this.shadowClosest("admin-element").setFieldData(s.field, null);
                this.requestDisplay();
                break;
        }
    }
}

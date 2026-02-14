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
import WebComponent from "base/web-component";

export default class AdminRelationshipField extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-relationship"];
    }

    static get observedAttributes() {
        return ["data-array-key", "data-path", "data-updated-at"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("change", this.handleChange);
        this.addEventListener("click", this.handleClick);
        this.addEventListener("close-drawer", this.handleCloseDrawer);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("change", this.handleChange);
        this.removeEventListener("click", this.handleClick);
        this.removeEventListener("close-drawer", this.handleCloseDrawer);
    }

    async updateDisplay() {
        const p = this.dataset.path;
        //		if (["variantOptions", "variants", "cart.items"].includes(p))
        //			return;
        const s = this.customState;
        s.field ??= this.closest("admin-edit").field(p);
        s.complex ??= s.field.type === "DocumentReference";
        //		const m = s.field.type === "List";
        const a = this.closest("admin-element");
        s.data ??= Array.isArray(s.field.data) ? s.field.data
            : s.field.data ? [s.field.data] : [];
        this.appendChild(this.interpolateDom({
            $template: "",
            values: s.data.map(x => ({
                $template: "value",
                label: a.title(x),
                value: `${x.$type}:${x.id}`
            })),
            /*
            options: s.options ? Object.values(s.options)[0].filter(x => !s.data.some(y => y.id === x.id)).map(x => ({
                $template: "option",
                value: x.id,
                text: a.title(x)
            })) : null,
            */
            optgroups: s.options && Object.keys(s.options).length > 1 ? Object.entries(s.options).map(x => ({
                $template: "optgroup",
                label: x[0],
                options: x[1].map(y => ({
                    $template: "option",
                    value: `${x[0]}:${y.id}`,
                    text: a.title(y)
                }))
            })) : null,
            hiddens: s.complex ? s.data.flatMap(x => Object.entries({
                $type: "DocumentReference",
                type: x.$type,
                id: x.id
            })).map(x => ({
                $template: "hidden",
                name: `${p}.${x[0]}`,
                value: x[1]
            })) : s.data.map(x => ({
                $template: "hidden",
                name: p,
                value: x.id
            })),
            drawer: s.drawer ? {
                $template: "drawer",
                ...s.drawer
            } : null
        }));
    }

    handleChange = event => {
        const el = event.target.closest("select");
        if (el) {
            const s = this.customState;
            //s.data = Object.values(s.options)[0].filter(x => x.id == el.value || s.data.some(y => y.id === x.id));
            s.data = Object.values(s.options).flatMap(x => x)
                .filter(x => `${x.$type}:${x.id}` === el.value || s.data.some(y => y.$type == x.$type && y.id === x.id));
            el.selectedIndex = 0;
            this.requestDisplay();
        }
    }

    handleClick = async event => {
        let el = event.target.closest("button");
        const a = this.closest("admin-element");
        const s = this.customState;
        switch (el?.name) {
            case "add-new": {
                const t = s.field.referenceTypes?.[0] ?? s.field.referenceType;
                s.drawer = {
                    slug: Object.entries(a.customState.schema["Collections"])
                        .find(([_, v]) => v.elementTypes[0] === t)[0]
                        .split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")
                };
                this.requestDisplay();
                break;
            }
            case "clear":
                s.data = [];
                this.requestDisplay();
                break;
            case "edit": {
                const t = s.data.find(x => x.id == el.value).$type;
                s.drawer = {
                    slug: Object.entries(a.customState.schema["Collections"])
                        .find(([_, v]) => v.elementTypes[0] === t)[0]
                        .split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-"),
                    id: el.value
                };
                this.requestDisplay();
                break;
            }
            case "remove":
                s.data = s.data.filter(x => x.id != el.value);
                this.requestDisplay();
                break;
        }

        el = event.target.closest("select");
        if (el) {
            const a1 = this.closest("app-element");
            const a2 = this.closest("admin-element");
            s.options ??= Object.fromEntries(await Promise.all((s.field.referenceTypes ?? [s.field.referenceType]).map(t => {
                const n = Object.entries(a2.customState.schema["Collections"]).find(([_, v]) => v.elementTypes[0] === t)[0];
                return fetch(`${a1.dataset.apiUrl}/${n.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")}`).then(x => x.json()).then(x => [t, x]);
            })));
            this.requestDisplay();
        }
    }

    handleCloseDrawer = async () => {
        delete this.customState.drawer;
        this.requestDisplay();
    }
}

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
import WebComponent from "web-component";

export default class AdminArray extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-array"];
    }

    static get observedAttributes() {
        return ["data-array-key", "data-path", "data-updated-at"];
    }

    constructor() {
        super();
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
        const p = this.dataset.path;
        const s = this.customState;
        s.field ??= this.closest("admin-edit").field(p);
        s.nextKey ??= s.field.data?.length ?? 0;
        s.items ??= Array.from({ length: s.nextKey }, (_, i) => ({
            key: i,
            expand: false
        }));
        this.appendChild(this.interpolateDom({
            $template: "",
            label: this.dataset.label,
            items: s.field.data?.map((x, i, xx) => ({
                $template: "item",
                title: x.$type.split(/(?=[A-Z])/).join(" "),
                options: (() => {
                    const oo = [];
                    oo.push({ text: "\u2026" });
                    if (i > 0)
                        oo.push({
                            value: "move-up",
                            text: "Move Up"
                        });
                    if (i < xx.length - 1)
                        oo.push({
                            value: "move-down",
                            text: "Move Down"
                        });
                    oo.push({
                        value: "remove",
                        text: "Remove"
                    });
                    return oo;
                })().map((y, j) => ({
                    $template: "option",
                    ...y,
                    selected: j === 0
                })),
                checked: s.items[i].expand,
                field: {
                    //$template: "upload",
                    $template: s.field.elementTypes[0] === "Long" ? "upload" : "fields",
                    path: `${p}.${i}`,
                    updatedAt: this.dataset.updatedAt,
                    arrayKey: s.items[i].key
                }
            })),
            dialog: s.dialog ? {
                $template: "dialog",
                types: s.field.elementTypes.map(x => ({
                    $template: "type",
                    label: x.split(/(?=[A-Z])/).join(" "),
                    value: x,
                    checked: false
                }))
            } : null
        }));
    }

    handleChange = event => {
        const el = event.target;
        const s = this.customState;
        //if (el.matches("[name]"))
        if (el.matches("select:not([name])")) {
            event.stopPropagation();
            const li = el.closest("li");
            const i = Array.prototype.indexOf.call(li.parentElement.children, li);
            const swap = (oo, i1, i2) => [oo[i1], oo[i2]] = [oo[i2], oo[i1]];
            switch (el.value) {
                case "move-down":
                    swap(s.field.data, i, i + 1);
                    swap(s.items, i, i + 1);
                    break;
                case "move-up":
                    swap(s.field.data, i, i - 1);
                    swap(s.items, i, i - 1);
                    break;
                case "remove":
                    s.field.data.splice(i, 1);
                    s.items.splice(i, 1);
                    break;
            }
            this.dispatchEvent(new CustomEvent("document-change", { bubbles: true }));
            this.requestDisplay();
        } else if (s.dialog && el.matches('[type="radio"]')) {
            event.stopPropagation();
            delete s.dialog;
            const a = this.closest("admin-element");
            a.initField(s.field);
            s.field.data.push({ $type: el.value });
            //console.log("x", s.field.data);
            s.items.push({
                key: s.nextKey++,
                expand: true
            });
            this.dispatchEvent(new CustomEvent("document-change", { bubbles: true }));
            this.requestDisplay();
        } else if (el.matches('[type="checkbox"]:not([name])')) {
            event.stopPropagation();
            const li = el.closest("li");
            const i = Array.prototype.indexOf.call(li.parentElement.children, li);
            s.items[i].expand = el.checked;
        }
    }

    handleClick = event => {
        const el = event.target.closest("button");
        if (el) {
            event.stopPropagation();
            const s = this.customState;
            switch (el.name) {
                case "add":
                    if (s.field.elementTypes.length === 1) {
                        const a = this.closest("admin-element");
                        a.initField(s.field);
                        s.field.data.push({ $type: s.field.elementTypes[0] });
                        s.items.push({
                            key: s.nextKey++,
                            expand: true
                        });
                        this.dispatchEvent(new CustomEvent("document-change", { bubbles: true }));
                    } else
                        s.dialog = true;
                    break;
                case "close":
                    delete s.dialog;
                    break;
                case "collapse-all":
                    s.items.forEach(x => x.expand = false);
                    break;
                case "show-all":
                    s.items.forEach(x => x.expand = true);
                    break;
            }
            this.requestDisplay();
        }
    }
}

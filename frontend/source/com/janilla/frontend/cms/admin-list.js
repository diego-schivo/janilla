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

export default class AdminList extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-list"];
    }

    static get observedAttributes() {
        return ["data-slug", "data-enable-selection"];
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
        const t = this.dataset.slug.split(/(?=[A-Z])/).map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" ");
        document.title = `${t} - Janilla`;
        const s = this.customState;
        const a = this.closest("app-element");
        s.data ??= await (await fetch(`${a.dataset.apiUrl}/${this.dataset.slug}`)).json();
        s.selectionIds ??= [];
        const a2 = this.closest("admin-element");
        const hh = a2.headers(this.dataset.slug);
        this.appendChild(this.interpolateDom({
            $template: "",
            header: {
                $template: "header",
                title: t,
                selection: s.selectionIds?.length ? {
                    $template: "selection",
                    count: s.selectionIds.length
                } : null
            },
            noResults: !s.data?.length ? {
                $template: "no-results",
                name: this.dataset.slug
            } : null,
            table: s.data?.length ? {
                $template: "table",
                heads: (() => {
                    const cc = hh.map(x => x.split(/(?=[A-Z])/).map(y => y.charAt(0).toUpperCase() + y.substring(1)).join(" "));
                    if (this.dataset.enableSelection === "true")
                        cc.unshift({
                            $template: "checkbox",
                            checked: s.selectionIds.length === s.data.length
                        });
                    return cc;
                })().map(x => ({
                    $template: "head",
                    content: x
                })),
                rows: s.data.map(x => ({
                    $template: "row",
                    cells: (() => {
                        const cc = hh.map(y => a2.cell(x, y));

                        if (!cc[0])
                            cc[0] = x.id;
                        cc[0] = this.closest("admin-drawer") ? {
                            $template: "button",
                            name: "select",
                            value: x.id,
                            content: cc[0]
                        } : {
                            $template: "link",
                            href: `/admin/collections/${this.dataset.slug}/${x.id}`,
                            content: cc[0]
                        };

                        if (this.dataset.enableSelection === "true")
                            cc.unshift({
                                $template: "checkbox",
                                value: x.id,
                                checked: s.selectionIds.includes(x.id)
                            });
                        return cc;
                    })().map(y => ({
                        $template: "cell",
                        content: y
                    }))
                }))
            } : null,
            publishDialog: s.publishDialog ? {
                $template: "publish-dialog",
                count: s.selectionIds.length,
                name: this.dataset.slug.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join(" ")
            } : null,
            unpublishDialog: s.unpublishDialog ? {
                $template: "unpublish-dialog",
                count: s.selectionIds.length,
                name: this.dataset.slug.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join(" ")
            } : null,
            deleteDialog: s.deleteDialog ? {
                $template: "delete-dialog",
                count: s.selectionIds.length,
                name: this.dataset.slug.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join(" ")
            } : null
        }));
    }

    handleChange = event => {
        const el = event.target.closest('[type="checkbox"]');
        if (!el)
            return;
        const s = this.customState;
        if (el.matches("[value]")) {
            const id = parseInt(el.value);
            const i = s.selectionIds.indexOf(id);
            if (i >= 0)
                s.selectionIds.splice(i, 1);
            if (el.checked)
                s.selectionIds.push(id);
        } else if (el.checked)
            s.selectionIds = s.data.map(x => x.id);
        else
            s.selectionIds.length = 0;
        this.requestDisplay();
    }

    handleClick = async event => {
        const el = event.target.closest("button");
        const a = this.closest("app-element");
        const a2 = this.closest("admin-element");
        const n = this.dataset.slug;
        const s = this.customState;
        switch (el?.name) {
            case "select":
                this.dispatchEvent(new CustomEvent("documentselected", {
                    bubbles: true,
                    detail: s.data.find(x => x.id == b.value)
                }));
                break;
            case "cancel-delete":
                delete s.deleteDialog;
                this.requestDisplay();
                break;
            case "cancel-publish":
                delete s.publishDialog;
                this.requestDisplay();
                break;
            case "cancel-unpublish":
                delete s.unpublishDialog;
                this.requestDisplay();
                break;
            case "confirm-delete": {
                const u = new URL(`${a.dataset.apiUrl}/${n}`, location.href);
                Array.prototype.forEach.call(this.querySelectorAll("[value]:checked"), x => u.searchParams.append("id", x.value));
                const j = await (await fetch(u, {
                    method: "DELETE",
                    credentials: "include"
                })).json();
                if (j.some(x => x.$type === "User" && x.id === a.currentUser.id)) {
                    a.currentUser = null;
                    a.navigate(new URL(`/admin/collections/${n}`, location.href));
                } else {
                    delete s.deleteDialog;
                    delete s.data;
                    this.requestDisplay();
                }
                break;
            }
            case "confirm-publish": {
                const u = new URL(`${a.dataset.apiUrl}/${n}`, location.href);
                Array.prototype.forEach.call(this.querySelectorAll("[value]:checked"), x => u.searchParams.append("id", x.value));
                await (await fetch(u, {
                    method: "PATCH",
                    credentials: "include",
                    headers: { "content-type": "application/json" },
                    body: JSON.stringify({
                        $type: a.customState.schema["Collections"][n].elementTypes[0],
                        documentStatus: "PUBLISHED"
                    })
                })).json();
                delete s.publishDialog;
                this.requestDisplay();
                break;
            }
            case "confirm-unpublish": {
                const u = new URL(`${a.dataset.apiUrl}/${n}`, location.href);
                Array.prototype.forEach.call(this.querySelectorAll("[value]:checked"), x => u.searchParams.append("id", x.value));
                await (await fetch(u, {
                    method: "PATCH",
                    credentials: "include",
                    headers: { "content-type": "application/json" },
                    body: JSON.stringify({
                        $type: a.customState.schema["Collections"][n].elementTypes[0],
                        documentStatus: "DRAFT"
                    })
                })).json();
                delete s.unpublishDialog;
                this.requestDisplay();
                break;
            }
            case "create": {
                /*
                const r = await fetch(`${a.dataset.apiUrl}/${n}`, {
                    method: "POST",
                    credentials: "include",
                    headers: { "content-type": "application/json" },
                    body: JSON.stringify({ $type: a2.customState.schema["Collections"][n].elementTypes[0] })
                });
                const j = await r.json();
                if (r.ok)
                    a.navigate(new URL(`/admin/collections/${n}/${j.id}`, location.href));
                else
                    a2.error(j);
                */
                await a2.createDocument(n);
                break;
            }
            case "delete":
                s.deleteDialog = true;
                this.requestDisplay();
                break;
            case "publish":
                s.publishDialog = true;
                this.requestDisplay();
                break;
            case "unpublish":
                s.unpublishDialog = true;
                this.requestDisplay();
                break;
        }
    }
}

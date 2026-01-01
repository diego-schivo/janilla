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

export default class AdminEdit extends WebComponent {

    static get templateNames() {
        return ["admin-edit"];
    }

    static get observedAttributes() {
        return ["data-updated-at"];
    }

    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("change", this.handleChange);
        this.addEventListener("document-change", this.handleDocumentChange);
        this.addEventListener("input", this.handleInput);
        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("change", this.handleChange);
        this.removeEventListener("document-change", this.handleDocumentChange);
        this.removeEventListener("input", this.handleInput);
        this.removeEventListener("submit", this.handleSubmit);
    }

    async updateDisplay() {
        const s = this.state;
        const a = this.closest("app-element");
        s.document ??= await (await fetch([a.dataset.apiUrl, this.dataset.slug, this.dataset.id].filter(x => x).join("/"))).json();
        s.versions = Object.hasOwn(s.document, "versionCount");
        s.drafts = Object.hasOwn(s.document, "documentStatus");
        const a2 = this.closest("admin-element");
        this.appendChild(this.interpolateDom({
            $template: "",
            entries: (() => {
                const kkvv = [];
                if (!s.document.id)
                    kkvv.push([null, "Creating new Media"])
                if (s.drafts)
                    kkvv.push(["Status", s.document.documentStatus.name]);
                if (s.document.updatedAt)
                    kkvv.push(["Last Modified", a2.dateTimeFormat.format(new Date(s.document.updatedAt))]);
                if (s.document.createdAt)
                    kkvv.push(["Created", a2.dateTimeFormat.format(new Date(s.document.createdAt))]);
                return kkvv;
            })().map(x => ({
                $template: "entry",
                key: x[0],
                value: x[1]
            })),
            previewLink: (() => {
                const h = a2.preview(s.document);
                return h ? {
                    $template: "preview-link",
                    href: h
                } : null;
            })(),
            button: {
                $template: "button",
                ...(s.drafts ? {
                    name: "publish",
                    disabled: s.document.documentStatus === "PUBLISHED" && !s.changed,
                    text: "Publish Changes"
                } : {
                    name: "save",
                    disabled: !s.changed,
                    text: "Save"
                })
            },
            //select: as.collectionSlug ? {
            select: false ? {
                $template: "select",
                options: [{
                    text: "\u22ee"
                }, {
                    value: "create",
                    text: "Create New"
                }, {
                    value: "duplicate",
                    text: "Duplicate"
                }, {
                    value: "delete",
                    text: "Delete"
                }].map((x, i) => ({
                    $template: "option",
                    ...x,
                    selected: i === 0
                }))
            } : null
        }));
    }

    handleChange = async event => {
        const el = event.target;
        const a = this.closest("app-element");
        const a2 = this.closest("admin-element");
        switch (el.closest("select:not([name])")?.value) {
            case "create": {
                const r = await fetch(`${a.dataset.apiUrl}/${a2.state.collectionSlug}`, {
                    method: "POST",
                    credentials: "include",
                    headers: { "content-type": "application/json" },
                    body: JSON.stringify({ $type: a2.state.documentType })
                });
                const j = await r.json();
                if (r.ok)
                    a.navigate(new URL(`/admin/collections/${a2.state.collectionSlug}/${j.id}`, location.href));
                else
                    a2.error(j);
                break;
            }
            case "duplicate": {
                for (const [k, v] of [...new FormData(this.querySelector("form")).entries()]) {
                    const i = k.lastIndexOf(".");
                    const f = a2.field(k.substring(0, i));
                    a2.initField(f);
                    f.data[k.substring(i + 1)] = v;
                }
                const r = await fetch(`${a.dataset.apiUrl}/${a2.state.collectionSlug}`, {
                    method: "POST",
                    credentials: "include",
                    headers: { "content-type": "application/json" },
                    body: JSON.stringify(a2.state.document)
                });
                const j = await r.json();
                if (r.ok)
                    a.navigate(new URL(`/admin/collections/${a2.state.collectionSlug}/${j.id}`, location.href));
                else
                    a2.error(j);
                break;
            }
            case "delete": {
                const r = await fetch(a2.state.documentUrl, {
                    method: "DELETE",
                    credentials: "include"
                });
                const j = await r.json();
                if (r.ok) {
                    if (j.$type === "User" && j.id === a.user.id)
                        a.user = null;
                    a.navigate(new URL(`/admin/${a2.state.pathSegments.slice(0, 2).join("/")}`, location.href));
                } else
                    a2.error(j);
                break;
            }
        }
    }

    handleDocumentChange = () => {
        const s = this.state;
        //delete s.document;
        if (!s.changed) {
            s.changed = true;
            this.requestDisplay();
        }
        if (s.drafts)
            this.requestAutoSave();
    }

    handleInput = event => {
        const el = event.target;
        const s = this.state;
        if (el.matches("[name]")) {
            event.stopPropagation();
            if (!s.changed) {
                s.changed = true;
                this.requestDisplay();
            }
            if (s.drafts)
                this.requestAutoSave();
        }
    }

    handleSubmit = async event => {
        event.preventDefault();
        await this.saveDocument(false);
    }

    async autoSaveTimeout() {
        const s = this.state;
        s.autoSave.timeoutID = undefined;
        s.autoSave.ongoing = true;
        try {
            await this.saveDocument(true);
        } finally {
            s.autoSave.ongoing = false;
        }
        if (s.autoSave.repeat) {
            s.autoSave.repeat = false;
            this.requestAutoSave(0);
        }
    }

    field(path, document) {
        const a = this.closest("admin-element");
        const t = a.state.schema["Globals"]?.[this.dataset.slug.split("-").map((x, i) => i ? x.charAt(0).toUpperCase() + x.substring(1) : x).join("")]?.type
            ?? a.state.schema["Collections"][this.dataset.slug.split("-").map((x, i) => i ? x.charAt(0).toUpperCase() + x.substring(1) : x).join("")].elementTypes[0];
        return a.field(path, {
            type: t,
            properties: a.state.schema[t],
            data: document ?? this.state.document
        });
    }

    async reloadFieldData(path) {
        const a = this.closest("app-element");
        const a2 = this.closest("admin-element");
        const d = await (await fetch([a.dataset.apiUrl, this.dataset.slug, this.dataset.id].filter(x => x).join("/"))).json();
        a2.setFieldData(this.field(path), this.field(path, d).data);
    }

    requestAutoSave(delay = 1000) {
        const s = this.state;
        s.autoSave ??= {};
        if (s.autoSave.ongoing)
            s.autoSave.repeat = true;
        else {
            if (typeof s.autoSave.timeoutId === "number")
                clearTimeout(s.autoSave.timeoutId);
            s.autoSave.timeoutId = setTimeout(() => this.autoSaveTimeout(), delay);
        }
    }

    async saveDocument(auto) {
        //console.log('this.closest("admin-element").state.document', this.closest("admin-element").state.document);
        const m = new Map(Array.from(new FormData(this.querySelector("form")).entries())
            .map(([k, v]) => [k.split("."), v]).sort(([k1, _], [k2, __]) => {
                if (k1.length !== k2.length)
                    return k1.length - k2.length;
                for (let i = 0;i < k1.length;i++)
                    if (k1[i] < k2[i])
                        return -1;
                    else if (k1[i] > k2[i])
                        return 1;
                return 0;
            }));
        const fee = Array.from(m.entries()).filter(([_, x]) => x instanceof File);
        const a = this.closest("app-element");
        const a2 = this.closest("admin-element");
        if (fee.length) {
            const fd = new FormData();
            for (const [k, v] of fee)
                fd.append(k.join("."), v);
            const xhr = new XMLHttpRequest();
            xhr.open("POST", `${a.dataset.apiUrl}/files/upload`, true);
            xhr.withCredentials = true;
            xhr.send(fd);
        }

        const o = new Map();
        //for (const [k, v] of ee.filter(([k, _]) => k.split(".").at(-1) !== "$type")) {
        for (const [kk, v] of m) {
            //console.log("k", k, "v", v);
            const k = kk.join(".");
            a2.setValue(o, k, v, this.field(k));
        }
        const s = this.state;
        s.document = {};
        for (const [k, v] of o) {
            //console.log("k", k, "v", v);
            const f = this.field(k);
            a2.initField(f.parent);
            f.parent.data[k.substring(k.lastIndexOf(".") + 1)] = v;
        }
        //console.log("s", s);
        const u = new URL([a.dataset.apiUrl, this.dataset.slug, this.dataset.id].filter(x => x).join("/"), location.href);
        if (auto) {
            u.searchParams.append("draft", true);
            u.searchParams.append("autosave", true);
        }
        const r = await fetch(u, {
            method: s.document.id ? "PUT" : "POST",
            credentials: "include",
            headers: { "content-type": "application/json" },
            body: JSON.stringify(s.document)
        });
        const j = await r.json();
        if (r.ok) {
            //if (this.dispatchEvent(new CustomEvent("documentsaved", {
            //bubbles: true,
            //cancelable: true,
            //detail: j
            //}))) {
            delete s.changed;
            if (!auto)
                a2.success("Updated successfully.");
            const f = (x, y) => {
                //console.log("x", JSON.stringify(x, null, "\t"), "y", JSON.stringify(y, null, "\t"));
                for (const [k, v] of Object.entries(x))
                    if (Array.isArray(v)) {
                        y[k] ??= [];
                        for (let i = 0;i < v.length;i++)
                            if (v[i] !== null && typeof v[i] === "object" && y[k][i] !== null && typeof y[k][i] === "object")
                                f(v[i], y[k][i]);
                            else
                                y[k][i] = v[i];
                    } else if (v !== null && typeof v === "object" && y[k] !== null && typeof y[k] === "object")
                        f(v, y[k]);
                    else
                        y[k] = v;
            };
            //console.log("s.document", s.document);
            f(j, s.document);
            //console.log("s.document", s.document);
            //}
			a2.currentDocument = j;
        } else
            a2.error(j);
    }
}

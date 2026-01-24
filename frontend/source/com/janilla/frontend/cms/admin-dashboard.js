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

export default class AdminDashboard extends WebComponent {

    static get templateNames() {
        return ["admin-dashboard"];
    }

    constructor() {
        super();
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
        document.title = "Dashboard - Janilla";
        const a = this.closest("admin-element");
        this.appendChild(this.interpolateDom({
            $template: "",
            groups: a.dashboardGroups().map(g => ({
                $template: "group",
                title: g.split(/(?=[A-Z])/).map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" "),
                cards: Object.entries(a.customState.schema[a.customState.schema["Data"][g].type]).map(([k, v]) => {
                    return {
                        $template: "card",
                        href: `/admin/${g === "globals" ? g : "collections"}/${k.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-")}`,
                        text: k.split(/(?=[A-Z])/).map(x => x.charAt(0).toUpperCase() + x.substring(1)).join(" "),
                        button: g !== "globals" && !a.isReadOnly(v.elementTypes[0]) ? {
                            $template: "button",
                            type: v.elementTypes[0]
                        } : null
                    };
                })
            }))
        }));
    }

    handleClick = async event => {
        const el = event.target.closest("button");
        const a = this.closest("app-element");
        const a2 = this.closest("admin-element");
        switch (el?.name) {
            case "create": {
                /*
                const t = el.closest("a").getAttribute("href").split("/").at(-1);
                const r = await fetch(`${a.dataset.apiUrl}/${t}`, {
                    method: "POST",
                    credentials: "include",
                    headers: { "content-type": "application/json" },
                    body: JSON.stringify({ $type: el.dataset.type })
                });
                const j = await r.json();
                if (r.ok)
                    a.navigate(new URL(`/admin/collections/${t}/${j.id}`, location.href));
                else
                    a2.error(j);
                */
                const n = el.closest("a").getAttribute("href").split("/").at(-1);
                await a2.createDocument(n);
                break;
            }
            case "seed": {
                const r = await fetch(`${a.dataset.apiUrl}/seed`, {
                    method: "POST",
                    credentials: "include"
                });
                if (r.ok)
                    a2.success("Database seeded!");
                else
                    a2.error(await r.json());
                break;
            }
        }
    }
}

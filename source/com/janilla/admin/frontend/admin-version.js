/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
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
import WebComponent from "web-component";

export default class AdminVersion extends WebComponent {

    static get templateNames() {
        return ["admin-version"];
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
        const a = this.closest("admin-element");
        const t = a.dateTimeFormat.format(new Date(a.state.version.updatedAt));
        this.appendChild(this.interpolateDom({
            $template: "",
            title: t,
            json: JSON.stringify(a.state.version.document, null, "  "),
            dialog: {
                $template: "dialog",
                dateTime: t
            }
        }));
    }

    handleClick = async event => {
        const el = event.target.closest("button");
        switch (el?.name) {
            case "cancel":
                this.querySelector("dialog").close();
                break;
            case "confirm": {
                const a = this.closest("app-element");
                const a2 = this.closest("admin-element");
                const j = await (await fetch(`${a2.state.collectionSlug
                    ? a2.state.documentUrl.substring(0, a2.state.documentUrl.lastIndexOf("/"))
                    : a2.state.documentUrl}/versions/${a2.state.version.id}`, { method: "POST" })).json();
                a2.currentDocument = j;
                a2.renderToast("Restored successfully.");
                a.navigate(`/admin/${a2.state.pathSegments.slice(0, 3).join("/")}`);
                break;
            }
            case "restore":
                this.querySelector("dialog").showModal();
                break;
        }
    }
}

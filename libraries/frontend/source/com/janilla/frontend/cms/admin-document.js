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

export default class AdminDocument extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-document"];
    }

    static get observedAttributes() {
        return ["data-subview", "data-updated-at"];
    }

    connectedCallback() {
        super.connectedCallback();
        const s = this.customState;
        s.admin = this.shadowClosest("admin-element");
        s.admin.addEventListener("documentchanged", this.handleDocumentChanged);
    }

    disconnectedCallback() {
        const s = this.customState;
        super.disconnectedCallback();
        s.admin?.removeEventListener("documentchanged", this.handleDocumentChanged);
    }

    async updateDisplay() {
        const a = this.shadowClosest("admin-element");
        {
            const x = ({
                edit: "Editing",
                versions: "Versions",
                api: "API"
            })[a.customState.pathSegments[a.customState.collectionSlug ? 3 : 2] ?? "edit"];
            document.title = `${x} - ${a.customState.document.$type} - Janilla`;
        }
        this.appendChild(this.interpolateDom({
            $template: "",
            title: [a.title(a.customState.document)].map(x => x?.length ? x : a.customState.pathSegments[2])[0],
            tabs: [
                "edit",
                Object.hasOwn(a.customState.document, "versionCount") ? "versions" : null,
                "api"
            ].filter(x => x).join(),
            tab: [({
                default: "edit",
                version: "versions"
            })[this.dataset.subview]].map(x => x ?? this.dataset.subview)[0],
            editButton: { $template: "edit-button" },
            versionsButton: Object.hasOwn(a.customState.document, "versionCount") ? {
                $template: "versions-button",
                count: a.customState.document.versionCount
            } : null,
            apiButton: { $template: "api-button" },
            editPanel: this.dataset.subview === "default" ? {
                $template: "edit-panel",
                slug: a.customState.pathSegments[1],
                id: a.customState.pathSegments[2],
                updatedAt: this.dataset.updatedAt
            } : null,
            versionsPanel: ["versions", "version"].includes(this.dataset.subview) ? {
                $template: "versions-panel",
                versions: this.dataset.subview === "versions" ? { $template: "versions" } : null,
                version: this.dataset.subview === "version" ? { $template: "version" } : null
            } : null,
            apiPanel: this.dataset.subview === "api" ? {
                $template: "api-panel",
                json: JSON.stringify(a.customState.document, null, "  ")
            } : null
        }));
    }

    handleDocumentChanged = () => {
        this.requestDisplay();
    }
}

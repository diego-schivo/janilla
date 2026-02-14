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

export default class AdminUpload extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["admin-upload"];
    }

    static get observedAttributes() {
        return ["data-array-key", "data-path", "data-updated-at"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("click", this.handleClick);
		this.addEventListener("documentsaved", this.handleDocumentSaved);
        this.addEventListener("documentselected", this.handleDocumentSelected);
        this.addEventListener("drawerclosed", this.handleDrawerClosed);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("click", this.handleClick);
		this.removeEventListener("documentsaved", this.handleDocumentSaved);
        this.removeEventListener("documentselected", this.handleDocumentSelected);
        this.removeEventListener("drawerclosed", this.handleDrawerClosed);
    }

    async updateDisplay() {
        const p = this.dataset.path;
        const s = this.customState;
        s.field = this.closest("admin-edit").field(p);
        this.appendChild(this.interpolateDom({
            $template: "",
            input: {
                $template: "input",
                name: p,
                value: s.field.data?.id
            },
            card: s.field.data ? {
                $template: "card",
                ...s.field.data
            } : null,
            dropzone: !s.field.data ? {
                $template: "dropzone"
            } : null,
            drawer: s.drawer === "list" ? {
                $template: "list-drawer",
                slug: "media"
            } : s.drawer === "edit" ? {
                $template: "edit-drawer",
                title: s.field.data?.file?.name,
                id: s.field.data?.id,
                slug: "media"
            } : null
        }));
    }

    handleClick = event => {
        const el = event.target.closest("button");
        const s = this.customState;
        switch (el?.name) {
            case "choose":
                s.drawer = "list";
                this.requestDisplay();
                break;
            case "create":
            case "edit":
                s.drawer = "edit";
                this.requestDisplay();
                break;
            case "remove":
                this.closest("admin-element").setFieldData(s.field, null);
                this.requestDisplay();
                break;
        }
    }

	handleDocumentSaved = event => {
	    event.preventDefault();
	    const s = this.customState;
	    this.closest("admin-element").setFieldData(s.field, event.detail);
	    delete s.drawer;
	    this.requestDisplay();
	}

    handleDocumentSelected = event => {
        const s = this.customState;
        this.closest("admin-element").setFieldData(s.field, event.detail);
        delete s.drawer;
        this.requestDisplay();
    }

    handleDrawerClosed = () => {
        delete this.customState.drawer;
        this.requestDisplay();
    }
}

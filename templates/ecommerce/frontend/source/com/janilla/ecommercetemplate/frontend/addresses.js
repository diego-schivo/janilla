/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
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
 */
import WebComponent from "base/web-component";

export default class Addresses extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["addresses"];
    }

    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("click", this.handleClick);
        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("click", this.handleClick);
        this.removeEventListener("submit", this.handleSubmit);
    }

    async updateDisplay() {
        const s = this.customState;
        const a = this.closest("app-element");
        a.updateSeo({ title: "Addresses" });
        this.appendChild(this.interpolateDom({
            $template: "",
            nav: {
                $template: "nav",
                path: a.currentPath
            },
            results: a.currentUser.addresses.length ? {
                $template: "results",
                items: a.currentUser.addresses.map(x => ({
                    $template: "result",
                    ...x
                }))
            } : { $template: "no-results" },
            dialog: s.dialog ? {
                $template: "dialog",
                ...s.dialog
            } : null
        }));
    }

    handleClick = event => {
        const b = event.target.closest("button");
        const s = this.customState;
        switch (b?.name) {
            case "add":
            case "edit":
                event.stopPropagation();
                s.dialog = { id: b.value };
                this.requestDisplay();
                break;
            case "close":
                event.stopPropagation();
                delete s.dialog;
                this.requestDisplay();
                break;
        }
    }

    handleSubmit = async event => {
        event.preventDefault();
        const f = event.target;
        const a = this.closest("app-element");
        const o = {
            customer: a.currentUser.id,
            ...Object.fromEntries(new FormData(f))
        };
        const s = this.customState;
        const r = await fetch(`${a.dataset.apiUrl}/addresses${s.dialog.id ? `/${s.dialog.id}` : ""}`, {
            method: s.dialog.id ? "PUT" : "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify(o)
        });
        const j = await r.json();
        if (r.ok) {
            delete this.customState.dialog;
            this.requestDisplay();
            this.dispatchEvent(new CustomEvent("user-change", {
                bubbles: true,
                detail: { user: j.customer }
            }));
        }
    }
}

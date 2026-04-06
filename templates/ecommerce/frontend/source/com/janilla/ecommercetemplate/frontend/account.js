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

export default class Account extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["account"];
    }

    static get observedAttributes() {
        return ["data-success", "data-warning"];
    }

    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("input", this.handleInput);
        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("input", this.handleInput);
        this.removeEventListener("submit", this.handleSubmit);
    }

    async updateDisplay() {
        const s = this.customState;
        const a = this.closest("app-element");
        s.orders ??= await (await fetch(`${a.dataset.apiUrl}/orders`)).json();
        a.updateSeo({ title: "Account" });
        this.appendChild(this.interpolateDom({
            $template: "",
            success: this.dataset.success ? {
                $template: "message",
                class: "success",
                text: this.dataset.success
            } : null,
            warning: this.dataset.warning ? {
                $template: "message",
                class: "warning",
                text: this.dataset.warning
            } : null,
            nav: {
                $template: "nav",
                path: a.currentPath
            },
            user: a.currentUser,
            orders: s.orders?.length ? {
                $template: "list",
                items: s.orders.map(x => ({
                    $template: "item",
                    item: JSON.stringify(x)
                }))
            } : { $template: "empty" }
        }));
    }

    handleInput = event => {
        const o = Object.fromEntries(new FormData(event.target.form));
        const u = this.closest("app-element").currentUser;
        this.querySelector("button").disabled = o.email === u.email && o.name === u.name;
    }

    handleSubmit = async event => {
        event.preventDefault();
        const u = this.closest("app-element").currentUser;
        const o = Object.fromEntries(new FormData(event.target));
        const r = await fetch(`/api/users/${u.id}`, {
            method: "PATCH",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({
                $type: "User",
                ...o
            })
        });
        if (r.ok) {
            this.dispatchEvent(new CustomEvent("user-change", {
                bubbles: true,
                detail: { user: await r.json() }
            }));
            this.requestDisplay();
        }
    }
}

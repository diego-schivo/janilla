/*
 * MIT License
 *
 * Copyright (c) 2024 Vercel, Inc.
 * Copyright (c) 2024-2026 Diego Schivo
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

export default class DashboardNav extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["dashboard-nav"];
    }

    connectedCallback() {
        super.connectedCallback();

        addEventListener("statepushed", this.handleStatePushed);
        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        removeEventListener("statepushed", this.handleStatePushed);
        this.removeEventListener("submit", this.handleSubmit);

        super.disconnectedCallback();
    }

    async updateDisplay() {
        this.appendChild(this.interpolateDom({
            $template: "",
            items: [{
                href: "/dashboard",
                icon: "home",
                text: "Home"
            }, {
                href: "/dashboard/invoices",
                icon: "document-duplicate",
                text: "Invoices"
            }, {
                href: "/dashboard/customers",
                icon: "user-group",
                text: "Customers"
            }].map(x => ({
                $template: "item",
                ...x,
                active: x.href === location.pathname ? "active" : ""
            }))
        }));
    }

    handleStatePushed = () => {
        this.requestDisplay();
    }

    handleSubmit = async event => {
        event.preventDefault();
        if (event.submitter.getAttribute("aria-disabled") === "true")
            return;
        event.submitter.setAttribute("aria-disabled", "true");
        try {
            const a = this.getRootNode().host.closest("app-element");
            await fetch(`${a.dataset.apiUrl}/authentication`, {
                method: "DELETE",
                credentials: "include"
            });
            a.navigate(new URL("/login", location.href));
        } finally {
            event.submitter.setAttribute("aria-disabled", "false");
        }
    }
}

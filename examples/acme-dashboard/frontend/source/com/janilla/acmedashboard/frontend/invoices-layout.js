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

export default class InvoicesLayout extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["invoices-layout"];
    }

    static get observedAttributes() {
        return ["data-uri", "slot"];
    }

    constructor() {
        super();
        this.attachShadow({ mode: "open" });
    }

    async updateDisplay() {
        const p = location.pathname;
        const pp = new URLSearchParams(location.search);
        const o = {
            $template: "",
            ...this.dataset,
            invoices: {
                $template: "invoices",
                slot: p === "/dashboard/invoices" ? "content" : null,
                title: "Invoices",
                href: "/dashboard/invoices",
                query: pp.get("query"),
                page: pp.get("page")
            },
            invoice: (() => {
                const m = p === "/dashboard/invoices/create" ? [] : p?.match(/\/dashboard\/invoices\/([^/]+)\/edit/);
                return {
                    $template: "invoice",
                    slot: m ? "content" : null,
                    title: m ? (m[1] ? "Edit Invoice" : "Create Invoice") : null,
                    id: m?.[1]
                };
            })()
        };
        o.breadcrumbItems = [o.invoices, o.invoice].slice(0, o.invoice.slot ? 2 : 1).map((x, i, xx) => ({
            ...x,
            $template: i < xx.length - 1 ? "breadcrumb-link" : "breadcrumb-heading",
            slot: `item-${i}`
        }));
        const f = this.interpolateDom(o);
        this.shadowRoot.append(...f.querySelectorAll("link, breadcrumb-nav, slot"));
        this.appendChild(f);
    }
}

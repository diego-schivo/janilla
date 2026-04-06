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

export default class DashboardLayout extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["dashboard-layout"];
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
        const f = this.interpolateDom({
            $template: "",
            dashboard: {
                $template: "dashboard",
                slot: p === "/dashboard" ? "content" : null
            },
            invoices: (() => {
                const nn = p.split("/");
                const x = {
                    $template: "invoices",
                    slot: nn[1] === "dashboard" && nn[2] === "invoices" ? "content" : null
                };
                if (x.slot)
                    x.uri = p + location.search;
                return x;
            })(),
            customers: {
                $template: "customers",
                slot: p === "/dashboard/customers" ? "content" : null,
                query: pp.get("query")
            }
        });
        this.shadowRoot.append(...f.querySelectorAll("link, aside, main"));
        this.appendChild(f);
    }
}

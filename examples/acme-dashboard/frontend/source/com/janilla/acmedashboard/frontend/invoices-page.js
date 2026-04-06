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

export default class InvoicesPage extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["invoices-page"];
    }

    static get observedAttributes() {
        return ["data-page", "data-query", "slot"];
    }

    connectedCallback() {
        super.connectedCallback();

        this.addEventListener("input", this.handleInput);
        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        this.removeEventListener("input", this.handleInput);
        this.removeEventListener("submit", this.handleSubmit);

        super.disconnectedCallback();
    }

    async updateDisplay() {
        const s = history.state;
        const u = new URL("/dashboard/invoices", location.href);
        const q = this.dataset.query;
        if (q)
            u.searchParams.append("query", q);
        const p = this.dataset.page;

        this.appendChild(this.interpolateDom({
            $template: "",
            ...this.dataset,
            articles: this.slot && s.invoices ? s.invoices.elements.map(x => ({
                $template: "article",
                ...x,
                href: `/dashboard/invoices/${x.id}/edit`
            })) : Array.from({ length: 6 }).map(() => ({ $template: "article-skeleton" })),
            rows: this.slot && s.invoices ? s.invoices.elements.map(x => ({
                $template: "row",
                ...x,
                href: `/dashboard/invoices/${x.id}/edit`
            })) : Array.from({ length: 6 }).map(() => ({ $template: "row-skeleton" })),
            pagination: this.slot && s.invoices ? {
                href: u.pathname + u.search,
                page: p ?? 1,
                pageCount: Math.ceil((s.invoices.totalSize ?? 0) / 6)
            } : null
        }));

        if (this.slot && !s.invoices) {
            const a = this.closest("app-element");
            const u = new URL(`${a.dataset.apiUrl}/invoices`, a.dataset.apiUrl.startsWith("/") ? location.href : undefined);
            ["query", "page"].forEach(x => {
                if (this.dataset[x])
                    u.searchParams.append(x, this.dataset[x]);
            });

            const x = await (await fetch(u, { credentials: "include" })).json();
            history.replaceState({
                ...history.state,
                invoices: x ?? []
            }, "");

            this.requestDisplay();
        }
    }

    handleInput = event => {
        if (typeof this.inputTimeout === "number")
            clearTimeout(this.inputTimeout);
        const q = event.target.value;
        this.inputTimeout = setTimeout(() => {
            this.inputTimeout = undefined;
            const u = new URL(location.href);
            u.searchParams.delete("page");
            const q0 = u.searchParams.get("query");
            if (q)
                u.searchParams.set("query", q);
            else
                u.searchParams.delete("query");
            history[q0 ? "replaceState" : "pushState"]({
                ...history.state,
                invoices: undefined
            }, "", u.pathname + u.search);
            dispatchEvent(new CustomEvent("popstate"));
        }, 1000);
    }

    handleSubmit = async event => {
        event.preventDefault();
        if (event.submitter.getAttribute("aria-disabled") === "true")
            return;
        event.submitter.setAttribute("aria-disabled", "true");
        try {
            const a = this.closest("app-element");
            const fd = new FormData(event.target);
            const r = await fetch(`${a.dataset.apiUrl}/invoices/${fd.get("id")}`, {
                method: "DELETE",
                credentials: "include"
            });
            if (r.ok) {
                history.replaceState({
                    ...history.state,
                    invoices: undefined
                }, "");
                await this.updateDisplay();
            } else {
                const x = await r.text();
                alert(x);
            }
        } finally {
            event.submitter.setAttribute("aria-disabled", "false");
        }
    }
}

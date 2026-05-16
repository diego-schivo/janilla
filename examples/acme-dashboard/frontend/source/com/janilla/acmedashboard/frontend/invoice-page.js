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

export default class InvoicePage extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["invoice-page"];
    }

    static get observedAttributes() {
        return ["data-id", "slot"];
    }

    connectedCallback() {
        super.connectedCallback();

        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        this.removeEventListener("submit", this.handleSubmit);

        super.disconnectedCallback();
    }

    async updateDisplay() {
        const hs = history.state;
        this.appendChild(this.interpolateDom({
            $template: "",
            ...this.dataset,
            ...hs.invoice,
            customerOptions: this.slot && hs.invoice ? hs.invoice.customers?.map(x => ({
                $template: "customer-option",
                ...x,
                selected: x.key == hs.invoice.customer.id
            })) : null,
            statusItems: this.slot && hs.invoice ? ["PENDING", "PAID"].map(x => ({
                $template: "status-item",
                value: x,
                checked: x === hs.invoice.status?.name
            })) : null
        }));
        if (this.slot && !Object.hasOwn(hs, "invoice")) {
            const a = this.shadowClosest("app-element");
            Promise.all([
                this.dataset.id
                    ? fetch(`${a.customEnv.apiUrl}/invoices/${this.dataset.id}`,
                        { credentials: "include" }).then(x => x.json())
                    : null,
                fetch(`${a.customEnv.apiUrl}/customers/names`,
                    { credentials: "include" }).then(x => x.json())
            ]).then(([i, nn]) => {
                history.replaceState({
                    ...history.state,
                    invoice: {
                        ...i,
                        customers: nn
                    }
                }, "");
                this.requestDisplay();
            });
        }
    }

    handleSubmit = async event => {
        event.preventDefault();

        if (event.submitter.getAttribute("aria-disabled") === "true")
            return;
        event.submitter.setAttribute("aria-disabled", "true");

        try {
            const d = Array.from(new FormData(event.target)).reduce((o, [k, v]) => {
                const o0 = o;
                const kk = k.split(".");
                const l = kk.length;
                for (let i = 0;i < l;i++)
                    if (i == l - 1)
                        o[kk[i]] = v;
                    else
                        o = o[kk[i]] ?? (o[kk[i]] = {});
                return o0;
            }, {});

            const mm = {
                customer: !d.customer?.id ? "Please select a customer." : "",
                amount: !d.amount ? "Please enter an amount greater than $0." : "",
                status: !d.status ? "Please select an invoice status." : ""
            };
            Object.entries(mm).forEach(([k, v]) => this.querySelector(`.${k}-error`).innerHTML = v.length ? `<p>${v}</p>` : "");
            const v = Object.values(mm).every(x => !x.length);
            this.querySelector(".error").innerHTML = v ? "" : `<p>Missing Fields. Failed to ${this.dataset.title}.</p>`;
            if (!v)
                return;

            const a = this.shadowClosest("app-element");
            const r = await fetch(this.dataset.id ? `${a.customEnv.apiUrl}/invoices/${this.dataset.id}` : `${a.customEnv.apiUrl}/invoices`, {
                method: this.dataset.id ? "PUT" : "POST",
                credentials: "include",
                headers: { "content-type": "application/json" },
                body: JSON.stringify(d)
            });

            if (r.ok)
                a.navigateTo(new URL(`${a.customEnv.basePath}/dashboard/invoices`, location.href));
            else {
                const t = await r.text();
                this.querySelector(".error").innerHTML = `<p>${t}</p>`;
            }
        } finally {
            event.submitter.setAttribute("aria-disabled", "false");
        }
    }
}

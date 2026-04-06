/*
 * MIT License
 *
 * Copyright (c) React Training LLC 2015-2019
 * Copyright (c) Remix Software Inc. 2020-2021
 * Copyright (c) Shopify Inc. 2022-2023
 * Copyright (c) Diego Schivo 2024-2026
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

export default class ContactPage extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["contact"];
    }

    static get observedAttributes() {
        return ["data-id", "data-loading", "slot"];
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
        if (this.slot) {
            let hs = history.state;
            const a = this.closest("app-element");

            if (!Object.hasOwn(hs, "contact"))
                history.replaceState(hs = {
                    ...hs,
                    contact: a.serverState.contact
                }, "");

            this.appendChild(this.interpolateDom({
                $template: "",
                ...hs.contact,
                name: hs.contact ? {
                    $template: hs.contact.full ? "name" : "no-name",
                    ...hs.contact
                } : null,
                twitter: hs.contact?.twitter ? {
                    $template: "twitter",
                    ...hs.contact
                } : null,
                notes: hs.contact?.notes ? {
                    $template: "notes",
                    ...hs.contact
                } : null
            }));

            if (!hs.contact || this.dataset.id != hs.contact.id) {
                const c = await (await fetch(`${a.dataset.apiUrl}/contacts/${this.dataset.id}`)).json();

                history.replaceState({
                    ...history.state,
                    contact: c
                }, "");

                a.navigate();
            }
        } else
            history.replaceState(Object.fromEntries(Object.entries(history.state)
                .filter(([k, _]) => k !== "contact")), "");
    }

    handleSubmit = async event => {
        event.preventDefault();
        event.stopPropagation();

        const hs = history.state;
        const a = this.closest("app-element");

        switch (event.target.method) {
            case "get":
                const u = new URL(`/contacts/${hs.contact.id}/edit`, location.href);
                const q = new URLSearchParams(location.search).get("q");
                if (q)
                    u.searchParams.append("q", q);
                a.navigate(u);
                break;

            case "post":
                if (confirm("Please confirm you want to delete this record.")) {
                    const r = await fetch(`${a.dataset.apiUrl}/contacts/${hs.contact.id}`, { method: "DELETE" });
                    if (r.ok)
                        a.navigate(new URL("/", location.href));
                    else
                        alert(await r.text());
                }
                break;
        }
    }

    async toggleFavorite(favorite) {
        const hs = history.state;
        const a = this.closest("app-element");

        hs.contact.favorite = favorite;
        this.requestDisplay();

        const r = await fetch(`${a.dataset.apiUrl}/contacts/${hs.contact.id}/favorite`, {
            method: "PUT",
            headers: { "content-type": "application/json" },
            body: JSON.stringify(hs.contact.favorite)
        });
        if (r.ok)
            a.navigate(new URL("/", location.href));
        else
            alert(await r.text());
    }
}

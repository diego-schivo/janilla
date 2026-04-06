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

export default class SidebarLayout extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["sidebar-layout"];
    }

    static get observedAttributes() {
        return ["data-href", "data-loading", "data-pending", "slot"];
    }

    constructor() {
        super();
        this.attachShadow({ mode: "open" });
    }

    connectedCallback() {
        super.connectedCallback();

        this.shadowRoot.addEventListener("submit", this.handleSubmit);
        this.shadowRoot.addEventListener("input", this.handleInput);
    }

    disconnectedCallback() {
        this.shadowRoot.removeEventListener("submit", this.handleSubmit);
        this.shadowRoot.removeEventListener("input", this.handleInput);

        super.disconnectedCallback();
    }

    async updateDisplay() {
        if (this.slot) {
            let hs = history.state;
            const a = this.closest("app-element");

            if (!Object.hasOwn(hs, "contacts"))
                history.replaceState(hs = {
                    ...hs,
                    contacts: a.serverState.contacts
                }, "");

            const c = location.pathname.match(/\/contacts\/([^/]+)(\/edit)?/);
            const q = new URLSearchParams(location.search).get("q");
            const o = {
                $template: "",
                search: (() => {
                    const l = q && this.dataset.loading != null;
                    return {
                        input: {
                            class: l ? "loading" : null,
                            value: q
                        },
                        spinner: { hidden: !l }
                    };
                })(),
                contacts: {
                    $template: hs.contacts?.length ? "contacts" : "no-contacts",
                    items: hs.contacts?.map(x => ({
                        $template: "item",
                        ...x,
                        href: (() => {
                            const u = new URL(`/contacts/${x.id}`, location.href);
                            if (q)
                                u.searchParams.append("q", q);
                            return u.pathname + u.search;
                        })(),
                        class: x.id === hs.contact?.id ? "active"
                            : `${location.pathname}/`.startsWith(`/contacts/${x.id}/`) ? "pending" : null,
                        name: {
                            $template: x.full ? "name" : "no-name",
                            ...x
                        },
                        favorite: x.favorite ? { $template: "favorite" } : null
                    }))
                },
                home: {
                    $template: "home",
                    slot: !(c && hs.contact) ? "content" : null
                },
                contact: (() => {
                    const x = {
                        $template: "contact",
                        slot: c && !c[2] ? (hs.contact ? "content" : "new-content") : null
                    };
                    if (x.slot) {
                        x.id = c[1];
                        x.loading = x.id != hs.contact?.id;
                    }
                    return x;
                })(),
                editContact: (() => {
                    const x = {
                        $template: "edit-contact",
                        slot: c && c[2] ? (hs.contact ? "content" : "new-content") : null
                    };
                    if (x.slot) {
                        x.id = c[1];
                        x.loading = x.id != hs.contact?.id;
                    }
                    return x;
                })()
            };
            o.detail = { class: ["contact", "editContact"].some(x => o[x].loading) ? "loading" : null };

            const df = this.interpolateDom(o);
            this.shadowRoot.append(...df.querySelectorAll("link, #sidebar, #detail"));
            this.appendChild(df);

            if (!hs.contacts) {
                const u = new URL(`${a.dataset.apiUrl}/contacts`, location.href);
                if (q)
                    u.searchParams.append("query", q);
                const cc = await (await fetch(u)).json();

                history.replaceState({
                    ...history.state,
                    contacts: cc
                }, "");

                a.navigate();
            }
        } else
            history.replaceState(Object.fromEntries(Object.entries(history.state)
                .filter(([k, _]) => k !== "contacts")), "");
    }

    handleInput = event => {
        const el = event.target.closest("#q");
        if (el) {
            const q = el.value;
            const q0 = new URLSearchParams(location.search).get("q");
            if (q != q0) {
                if (typeof this.inputTimeout === "number")
                    clearTimeout(this.inputTimeout);
                this.inputTimeout = setTimeout(() => {
                    delete this.customState.contacts;
                    const u = new URL(location.href);
                    u.searchParams.set("q", q);
                    if (!q0)
                        history.pushState(history.state, "", u.pathname + u.search);
                    else
                        history.replaceState(history.state, "", u.pathname + u.search);
                    dispatchEvent(new CustomEvent("popstate"));
                }, 500);
            }
        }
    }

    handleSubmit = async event => {
        const el = event.target.closest("#sidebar");
        if (el) {
            event.preventDefault();
            event.stopPropagation();
            const a = this.closest("app-element");
            const r = await fetch(`${a.dataset.apiUrl}/contacts`, {
                method: "POST",
                headers: { "content-type": "application/json" },
                body: JSON.stringify({})
            });
            if (r.ok) {
                const c = await r.json();
                delete this.customState.contacts;
                history.pushState({
                    ...history.state,
                    contact: c
                }, "", `/contacts/${c.id}/edit`);
                dispatchEvent(new CustomEvent("popstate"));
            } else
                alert(await r.text());
        }
    }
}

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

export default class EditContact extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["edit-contact"];
    }

    static get observedAttributes() {
        return ["data-id", "data-loading", "slot"];
    }

    connectedCallback() {
        super.connectedCallback();

        this.addEventListener("submit", this.handleSubmit);
        this.addEventListener("click", this.handleClick);
    }

    disconnectedCallback() {
        this.removeEventListener("submit", this.handleSubmit);
        this.removeEventListener("click", this.handleClick);

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
                ...hs.contact
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

    handleClick = event => {
        if (event.target.matches('[type="button"]')) {
            event.stopPropagation();
            history.back();
        }
    }

    handleSubmit = async event => {
        event.preventDefault();
        event.stopPropagation();

        const hs = history.state;
        const a = this.closest("app-element");

        const r = await fetch(`${a.dataset.apiUrl}/contacts/${hs.contact.id}`, {
            method: "PUT",
            headers: { "content-type": "application/json" },
            body: JSON.stringify(Object.fromEntries(new FormData(event.target)))
        });
        if (r.ok)
            a.navigate(new URL(`/contacts/${hs.contact.id}`, location.href));
        else
            alert(await r.text());
    }
}

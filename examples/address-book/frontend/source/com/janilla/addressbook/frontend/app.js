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
import BaseApp from "base/app";

export default class App extends BaseApp {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["app"];
    }

    constructor() {
        super();

        this.attachShadow({ mode: "open" });
    }

    connectedCallback() {
        super.connectedCallback();

        this.addEventListener("click", this.handleClick);
    }

    disconnectedCallback() {
        this.removeEventListener("click", this.handleClick);

        super.disconnectedCallback();
    }

    async updateDisplaySite() {
        const hs = history.state;
        const ss = this.serverState;

        const p = location.pathname;
        const o = {
            $template: "",
            sidebar: (() => {
                const h = p === "/";
                const c = p.match(/\/contacts\/([^/]+)(\/edit)?/);
                return {
                    $template: "sidebar",
                    slot: (h || c) ? (hs.contacts || ss.contacts ? "content" : "new-content") : null,
                    href: p + location.search,
                    loading: (h || c) && !(hs.contacts || ss.contacts),
                    pending: c && c[1] != (hs.contact ?? ss.contact)?.id
                };
            })(),
            about: {
                $template: "about",
                slot: p === "/about" ? "content" : null
            }
        };
        o.loading = {
            $template: "loading",
            slot: ["sidebar", "about"].every(x => o[x].slot !== "content") ? "content" : null
        };

        const df = this.interpolateDom(o);
        this.shadowRoot.append(...df.querySelectorAll("link, slot"));
        this.appendChild(df);
    }
}

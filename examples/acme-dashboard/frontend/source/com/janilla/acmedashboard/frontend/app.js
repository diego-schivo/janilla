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

    get currentUser() {
        return this.customState.user;
    }

    set currentUser(currentUser) {
        this.customState.user = currentUser;
        this.dispatchEvent(new CustomEvent("userchanged", { detail: currentUser }));
    }

    async updateDisplay() {
        const s = this.customState;
        const ss = this.serverState;

        if (!Object.hasOwn(s, "user"))
            s.user = ss && Object.hasOwn(ss, "user")
                ? ss.user
                : await (await fetch(`${this.dataset.apiUrl}/authentication`,
                    { credentials: "include" })).json();

        const p = location.pathname;
        const f = this.interpolateDom({
            $template: "",
            welcome: {
                $template: "welcome",
                slot: p === "/" ? "content" : null
            },
            login: {
                $template: "login",
                slot: p === "/login" ? "content" : null
            },
            dashboard: (() => {
                const a = p.split("/")[1] === "dashboard";
                return {
                    $template: "dashboard",
                    slot: a ? "content" : null,
                    uri: a ? p + location.search : null
                };
            })()
        });
        this.shadowRoot.append(...f.querySelectorAll("slot"));
        this.appendChild(f);
    }
}

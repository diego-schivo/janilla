/*
 * MIT License
 *
 * Copyright (c) 2018-2025 Payload CMS, Inc. <info@payloadcms.com>
 * Copyright (c) 2024-2026 Diego Schivo <diego.schivo@janilla.com>
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
import BlankApp from "blank/app";

const postsRegex = /^\/posts(\/.*)?$/;

export default class App extends BlankApp {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["/base/app", "/blank/app", "app"];
    }

    get colorScheme() {
        return this.customState.colorScheme;
    }

    set colorScheme(colorScheme) {
        if (colorScheme)
            localStorage.setItem(`${this.dataset.key}.color-scheme`, colorScheme);
        else
            localStorage.removeItem(`${this.dataset.key}.color-scheme`);
        this.requestDisplay();
    }

    connectedCallback() {
        super.connectedCallback();

        this.customState.title = document.title;
    }

    async updateDisplaySite() {
        const s = this.customState;
        const ss = this.serverState;
        if (!Object.hasOwn(s, "header"))
            s.header = ss && Object.hasOwn(ss, "header")
                ? ss.header
                : await (await fetch(`${this.dataset.apiUrl}/header`)).json();
        if (!Object.hasOwn(s, "footer"))
            s.footer = ss && Object.hasOwn(ss, "footer")
                ? ss.footer
                : await (await fetch(`${this.dataset.apiUrl}/footer`)).json();
        s.colorScheme = localStorage.getItem(`${this.dataset.key}.color-scheme`);

        this.appendChild(this.interpolateDom({
            $template: "",
            site: this.siteData()
        }));
    }

    siteData() {
        return {
            $template: "site",
            colorScheme: this.colorScheme ?? "light dark",
            before: this.currentUser?.roles?.some(x => x.name === "ADMIN") ? { $template: "admin-bar" } : null,
            header: { $template: "header" },
            content: this.contentData(),
            footer: { $template: "footer" }
        };
    }

    contentData() {
        const s = this.customState;
        if (s.notFound)
            return { $template: "not-found" };
        const p = location.pathname;
        const m = p.match(postsRegex);
        if (m)
            return m[1] ? {
                $template: "post",
                slug: m[1].substring(1)
            } : { $template: "posts" };
        return p === "/search" ? {
            $template: "search",
            query: new URLSearchParams(location.search).get("q")
        } : {
            $template: "page",
            slug: p.split("/").map(x => x === "" ? "home" : x)[1]
        };
    }
}

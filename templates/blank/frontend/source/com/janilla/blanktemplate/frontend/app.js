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
import BaseApp from "base/app";

const adminRegex = /^\/admin(\/.*)?$/;

export default class App extends BaseApp {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["/base/app", "app"];
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
                : await (await fetch(`${this.dataset.apiUrl}/users/me`)).json();

        const p = location.pathname;
        const m = p.match(adminRegex);
        if (m)
            this.appendChild(this.interpolateDom({
                $template: "",
                admin: {
                    $template: "admin",
                    user: this.currentUser ? JSON.stringify(this.currentUser) : null,
                    uri: (m[1] ?? "/") + location.search
                }
            }));
        else
            await super.updateDisplay();
    }

    async updateDisplaySite() {
        this.appendChild(this.interpolateDom({
            $template: "",
            site: this.customState.notFound ? { $template: "not-found" } : {
                $template: "page",
                slug: location.pathname.split("/").map(x => x === "" ? "home" : x)[1]
            }
        }));
    }

    navigate(url) {
        this.querySelectorAll("dialog[open]").forEach(x => x.close());
        super.navigate(url);
    }

    updateSeo(meta) {
        const sn = this.dataset.title;
        const t = [meta?.title && meta.title !== sn ? meta.title : null, sn].filter(x => x).join(" | ");
        const d = meta?.description ?? "";
        for (const [k, v] of Object.entries({
            title: t,
            description: d,
            "og:title": t,
            "og:description": d,
            "og:url": location.href,
            "og:site_name": sn,
            "og:image": meta?.image?.uri ? `${location.protocol}://${location.host}${meta.image.uri}` : null,
            "og:type": "website"
        }))
            if (k === "title")
                document.title = v ?? "";
            else {
                const el = document.querySelector(`meta[name="${k}"]`) ?? (() => {
                    const x = document.createElement("meta");
                    x.setAttribute("name", k);
                    document.head.append(x);
                    return x;
                })();
                el.setAttribute("content", v ?? "");
            }
    }
}

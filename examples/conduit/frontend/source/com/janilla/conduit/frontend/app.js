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

export default class App extends BlankApp {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["/base/app", "/blank/app", "app"];
    }

    connectedCallback() {
        super.connectedCallback();

        addEventListener("popstate", this.handlePopState);
        this.addEventListener("set-current-user", this.handleSetCurrentUser);

        if (!location.hash)
            location.hash = "#/";
    }

    disconnectedCallback() {
        removeEventListener("popstate", this.handlePopState);
        this.removeEventListener("set-current-user", this.handleSetCurrentUser);

        super.disconnectedCallback();
    }

    siteData() {
        const p = location.hash.substring(1);
        const nn = p.split("/");
        const hs = history.state ?? {};
        return {
            $template: "site",
            header: ({
                $template: "header",
                navItems: (() => {
                    const ii = [{
                        href: "#/",
                        text: "Home"
                    }];
                    const u = this.currentUser;
                    if (u)
                        ii.push({
                            href: "#/editor",
                            icon: "ion-compose",
                            text: "New Article"
                        }, {
                            href: "#/settings",
                            icon: "ion-gear-a",
                            text: "Settings"
                        }, {
                            href: `#/@${u.username}`,
                            image: u.image,
                            text: u.username
                        });
                    else
                        ii.push({
                            href: "#/login",
                            text: "Sign in"
                        }, {
                            href: "#/register",
                            text: "Sign up"
                        });
                    return ii.map(x => ({
                        $template: "nav-item",
                        ...x,
                        active: x.href === location.hash ? "active" : null,
                    }));
                })()
            }),
            path: p,
            loading: (() => {
                switch (nn[1]) {
                    case "article":
                    case "editor":
                        return !hs.article;
                    default:
                        if (nn[1]?.startsWith("@"))
                            return !hs.profile;
                        return false;
                }
            })(),
            footer: { $template: "footer" }
        };
    }

    handleClick(event) {
        const a = event.composedPath().find(x => x instanceof Element && x.matches("a"));
        if (a?.href) {
            event.preventDefault();
            location.hash = new URL(a.href).hash;
        }
    }

    handlePopState = _ => {
        this.requestDisplay(0);
    }

    handleSetCurrentUser = event => {
        const { user } = event.detail;
        const s = this.customState;
        s.user = user;

        if (user?.token) {
            localStorage.setItem("jwtToken", user.token);
            s.apiHeaders = { Authorization: `Token ${user.token}` };
        } else {
            localStorage.removeItem("jwtToken");
            s.apiHeaders = {};
        }

        location.hash = "#/";
    }
}

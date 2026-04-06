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
import WebComponent from "base/web-component";

export default class Page extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["page"];
    }

    static get observedAttributes() {
        return ["data-slug"];
    }

    async updateDisplay() {
        let hs = history.state;
        const a = this.closest("app-element");

        if (!Object.hasOwn(hs, "page"))
            history.replaceState(hs = {
                ...hs,
                page: a.serverState?.page
            }, "");

        if (this.dataset.slug != hs.page?.slug) {
            const u = new URL(`${a.dataset.apiUrl}/pages`, location.href);
            u.searchParams.append("slug", this.dataset.slug);
            const p = (await (await fetch(u)).json())[0] ?? (this.dataset.slug === "home" ? { slug: "home" } : null);
            history.replaceState(hs = {
                ...hs,
                page: p
            }, "");
        }

        if (hs.page) {
            if (hs.page.slug === "home" && !hs.page.layout)
                history.replaceState(hs = {
                    ...hs,
                    page: {
                        ...hs.page,
                        layout: [
                            {
                                $type: "Content",
                                columns: [
                                    {
                                        $type: "Column",
                                        size: { name: "FULL" },
                                        richText: `<h1>Janilla Ecommerce Template</h1>
																	<p>
																	  <a href="/admin">Visit the admin dashboard</a>
																	  to make your account and seed content for your website.
																	</p>`
                                    }
                                ]
                            }
                        ]
                    }
                }, "");
            a.updateSeo(hs.page.meta);
            this.appendChild(this.interpolateDom({
                $template: "",
                placeholder: !hs.page.id ? "placeholder" : null,
                hero: (hs.page.hero?.type?.name ?? "NONE") !== "NONE" ? {
                    $template: "hero",
                    path: "hero"
                } : null,
                layout: hs.page.layout?.map((x, i) => ({
                    $template: x.$type.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-"),
                    path: `layout.${i}`
                }))
            }));
        } else
            a.notFound();
    }

    data(path) {
        return path.split(".").reduce((x, n) => Array.isArray(x)
            ? x[parseInt(n)]
            : typeof x === "object" && x !== null
                ? x[n]
                : null, history.state.page);
    }
}

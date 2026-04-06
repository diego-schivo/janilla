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

export default class Post extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get observedAttributes() {
        return ["data-slug"];
    }

    static get templateNames() {
        return ["post"];
    }

    async updateDisplay() {
        let hs = history.state;
        const a = this.closest("app-element");

        if (!Object.hasOwn(hs, "post"))
            history.replaceState(hs = {
                ...hs,
                post: a.serverState?.post
            }, "");

        if (this.dataset.slug != hs.post?.slug) {
            const u = new URL(`${a.dataset.apiUrl}/posts`, location.href);
            u.searchParams.append("slug", this.dataset.slug);
            u.searchParams.append("depth", 1);
            const p = (await (await fetch(u)).json()).elements[0];
            history.replaceState(hs = {
                ...hs,
                post: p
            }, "");
        }

        const p = hs.post ?? {};
        a.updateSeo(p.meta);
        this.appendChild(this.interpolateDom({
            $template: "",
            ...p,
            metadata: {
                $template: "metadata",
                items: [p.authors?.length ? {
                    term: "Author",
                    description: p.authors.map(x => x.name).join(", ")
                } : null, p.publishedAt ? {
                    term: "Date Published",
                    description: {
                        $template: "date",
                        value: p.publishedAt
                    }
                } : null].filter(x => x).map(x => ({
                    $template: "metadatum",
                    ...x
                }))
            },
            content: hs.post?.content?.map((x, i) => ({
                $template: x.$type.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-"),
                path: `content.${i}`
            })),
            cards: hs.post?.relatedPosts?.map(x => ({
                $template: "card",
                ...x
            }))
        }));
    }

    data(path) {
        return path.split(".").reduce((x, n) => Array.isArray(x)
            ? x[parseInt(n)]
            : typeof x === "object" && x !== null
                ? x[n]
                : null, history.state.post);
    }
}

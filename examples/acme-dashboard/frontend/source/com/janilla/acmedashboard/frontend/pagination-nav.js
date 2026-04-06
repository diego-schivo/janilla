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
import WebComponent from "base/web-component";

export default class PaginationNav extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["pagination-nav"];
    }

    static get observedAttributes() {
        return ["data-href", "data-page", "data-page-count"];
    }

    async updateDisplay() {
        const pc = this.dataset.pageCount ? parseInt(this.dataset.pageCount) : 0;
        const u = new URL(this.dataset.href, location.href);
        const p = this.dataset.page ? parseInt(this.dataset.page) : 1;
        this.appendChild(this.interpolateDom({
            $template: "",
            links: pc > 1 ? [(() => {
                u.searchParams.set("page", p - 1);
                return {
                    $template: "link",
                    href: p > 1 ? u.pathname + u.search : null,
                    content: {
                        $template: "icon",
                        icon: "arrow-left"
                    }
                };
            })(), ...Array.from({ length: pc }, (_, i) => i + 1)
                .map(x => {
                    u.searchParams.set("page", x);
                    return {
                        $template: "link",
                        href: u.pathname + u.search,
                        class: x === p ? "active" : "",
                        content: {
                            $template: "text",
                            text: x
                        }
                    };
                }), (() => {
                    u.searchParams.set("page", p + 1);
                    return {
                        $template: "link",
                        href: p < pc ? u.pathname + u.search : null,
                        content: {
                            $template: "icon",
                            icon: "arrow-right"
                        }
                    };
                })()] : null
        }));
    }
}

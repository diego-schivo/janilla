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

export default class Search extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get observedAttributes() {
        return ["data-query"];
    }

    static get templateNames() {
        return ["search"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("input", this.handleInput);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("input", this.handleInput);
    }

    handleInput = async event => {
        const el = event.target.closest("input");
        if (el?.name === "q") {
            event.stopPropagation();
            const { results, ...hs } = history.state;
            const u = new URL(location.pathname, location.href);
            if (el.value)
                u.searchParams.append("q", el.value);
            history.pushState(hs, "", u.pathname + u.search);
            dispatchEvent(new CustomEvent("popstate"));
        }
    }

    async updateDisplay() {
        let hs = history.state;

        if (!hs.results) {
            const a = this.closest("app-element");
            const u = new URL(`${a.dataset.apiUrl}/search-results`, location.href);
            if (this.dataset.query)
                u.searchParams.append("query", this.dataset.query);
            history.replaceState(hs = {
                ...hs,
                results: await (await fetch(u)).json()
            }, "");
        }

        this.appendChild(this.interpolateDom({
            $template: "",
            ...this.dataset,
            cards: hs.results.map(x => ({
                $template: "card",
                ...x
            }))
        }));
    }
}

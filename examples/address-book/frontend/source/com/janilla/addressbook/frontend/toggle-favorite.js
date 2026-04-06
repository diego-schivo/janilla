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

export default class ToggleFavorite extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["toggle-favorite"];
    }

    static get observedAttributes() {
        return ["data-checked"];
    }

    connectedCallback() {
        super.connectedCallback();

        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        this.removeEventListener("submit", this.handleSubmit);

        super.disconnectedCallback();
    }

    async updateDisplay() {
        const f = this.dataset.checked !== undefined;
        this.appendChild(this.interpolateDom({
            $template: "",
            label: f ? "Remove from favorites" : "Add to favorites",
            value: (!f).toString(),
            text: f ? "★" : "☆"
        }));
    }

    handleSubmit = event => {
        event.preventDefault();
        event.stopPropagation();

        this.closest("contact-element").toggleFavorite(this.dataset.checked === undefined);
    }
}

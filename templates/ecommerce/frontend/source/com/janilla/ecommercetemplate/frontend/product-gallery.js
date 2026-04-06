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

export default class ProductGallery extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["product-gallery"];
    }

    static get observedAttributes() {
        return ["data-variant-options"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("click", this.handleClick);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("click", this.handleClick);
    }

    async updateDisplay() {
        const s = this.customState;
        const p = history.state.product;
        s.index ??= p.enableVariants ? (() => {
            const oo = this.dataset.variantOptions.split(",");
            const i = p.gallery.findIndex(x => oo.includes(x.variantOption.id.toString()));
            return i !== -1 ? i : 0;
        })() : 0;
        this.appendChild(this.interpolateDom({
            $template: "",
            image: p.gallery[s.index].image,
            thumbnails: p.gallery.map((x, i) => ({
                $template: "thumbnail",
                active: i === s.index ? "active" : null,
                image: x.image
            }))
        }));
    }

    handleClick = async event => {
        const b = event.target.closest("button");
        const ul = b?.closest("ul");
        if (ul) {
            this.customState.index = Array.from(ul.children).findIndex(x => x.contains(b));
            this.requestDisplay();
        }
    }
}

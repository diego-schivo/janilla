/*
 * MIT License
 *
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

export default class TodoBottombar extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["todo-bottombar"];
    }

    connectedCallback() {
        super.connectedCallback();

        this.addEventListener("click", this.handleClick);

        const s = this.customState;
        s.app = this.closest("todo-app");
        s.app.addEventListener("datachanged", this.handleDataChanged);
        s.app.addEventListener("filterchanged", this.handleFilterChanged);
    }

    disconnectedCallback() {
        this.removeEventListener("click", this.handleClick);

        const s = this.customState;
        s.app.removeEventListener("datachanged", this.handleDataChanged);
        s.app.removeEventListener("filterchanged", this.handleFilterChanged);

        super.disconnectedCallback();
    }

    async updateDisplay() {
        const as = this.customState.app.customState;
        this.appendChild(this.interpolateDom({
            $template: "",
            content: as.totalItems !== 0 ? {
                $template: "content",
                todoStatusText: `${as.activeItems} ${as.activeItems === 1 ? "item" : "items"} left!`,
                filterItems: ["all", "active", "completed"].map(x => ({
                    $template: "filter-item",
                    id: `filter-link-${x}`,
                    selected: x === as.filter ? "selected" : null,
                    href: `#/${x !== "all" ? x : ""}`,
                    text: `${x.charAt(0).toUpperCase()}${x.substring(1)}`
                }))
            } : null
        }));
    }

    handleClick = event => {
        if (event.target.matches(".clear-completed-button"))
            this.closest("todo-app").clearCompleted();
    }

    handleDataChanged = () => {
        this.requestDisplay();
    }

    handleFilterChanged = () => {
        this.requestDisplay();
    }
}

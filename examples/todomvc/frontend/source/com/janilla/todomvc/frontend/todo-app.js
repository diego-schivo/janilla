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

export default class TodoApp extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["todo-app"];
    }

    connectedCallback() {
        super.connectedCallback();

        const s = this.customState;
        s.data = [];
        s.totalItems = 0;
        s.activeItems = 0;
        s.completedItems = 0;
        s.filter = "all";

        this.addEventListener("datachanged", this.handleDataChanged);
        addEventListener("hashchange", this.handleHashChange);
    }

    disconnectedCallback() {
        this.removeEventListener("datachanged", this.handleDataChanged);
        removeEventListener("hashchange", this.handleHashChange);

        super.disconnectedCallback();
    }

    async updateDisplay() {
        const s = this.customState;
        this.appendChild(this.interpolateDom({
            $template: "",
            list: s.totalItems !== 0 ? { $template: "list" } : null
        }));
    }

    addItem(item) {
        this.customState.data.push(item);
        this.dispatchEvent(new Event("datachanged"));
    }

    clearCompleted() {
        const d = this.customState.data;
        for (let i = d.length - 1;i >= 0;i--)
            if (d[i].completed)
                d.splice(i, 1);

        this.dispatchEvent(new Event("datachanged"));
    }

    handleDataChanged = () => {
        const s = this.customState;
        s.totalItems = s.data.length;
        s.activeItems = s.data.reduce((x, y) => y.completed ? x : x + 1, 0);
        s.completedItems = s.totalItems - s.activeItems;

        this.requestDisplay();
    }

    handleHashChange = _ => {
        const h1 = location.hash.split("/")[1];
        this.customState.filter = h1?.length ? h1 : "all";

        this.dispatchEvent(new Event("filterchanged"));
    }

    removeItem(item) {
        const d = this.customState.data;
        for (let i = d.length - 1;i >= 0;i--)
            if (d[i].id === item.id)
                d.splice(i, 1);

        this.dispatchEvent(new Event("datachanged"));
    }

    toggleAll(item) {
        this.customState.data.forEach(x => x.completed = item.completed);

        this.dispatchEvent(new Event("datachanged"));
    }

    toggleItem(item) {
        this.customState.data.find(x => x.id === item.id).completed = item.completed;

        this.dispatchEvent(new Event("datachanged"));
    }

    updateItem(item) {
        this.customState.data.find(x => x.id === item.id).title = item.title;

        this.dispatchEvent(new Event("datachanged"));
    }
}

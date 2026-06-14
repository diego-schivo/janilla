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
        s.data = this.closest("app-element").serverState.todoItems.elements;
        s.totalItems = s.data.length;
        s.activeItems = s.data.reduce((x, y) => y.completed ? x : x + 1, 0);
        s.completedItems = s.totalItems - s.activeItems;
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

    async addItem(item) {
        const a = this.closest("app-element");
        const r = await fetch(`${a.customEnv.apiUrl}/todo-items`, {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify(item)
        });
        const j = await r.json();
        if (r.ok) {
            this.customState.data.push(j);
            this.dispatchEvent(new Event("datachanged"));
        }
    }

    async clearCompleted() {
        const a = this.closest("app-element");
        const u = new URL(`${a.customEnv.apiUrl}/todo-items`);
        this.customState.data.filter(x => x.completed)
            .forEach(x => u.searchParams.append("id", x.id));
        const r = await fetch(u, { method: "DELETE" });
        if (r.ok) {
            const d = this.customState.data;
            for (let i = d.length - 1;i >= 0;i--)
                if (d[i].completed)
                    d.splice(i, 1);
            this.dispatchEvent(new Event("datachanged"));
        }
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

    async removeItem(item) {
        const a = this.closest("app-element");
        const r = await fetch(`${a.customEnv.apiUrl}/todo-items/${item.id}`, { method: "DELETE" });
        if (r.ok) {
            const d = this.customState.data;
            for (let i = d.length - 1;i >= 0;i--)
                if (d[i].id == item.id)
                    d.splice(i, 1);
            this.dispatchEvent(new Event("datachanged"));
        }
    }

    async toggleAll(item) {
        const a = this.closest("app-element");
        const u = new URL(`${a.customEnv.apiUrl}/todo-items`);
        this.customState.data.filter(x => x.completed != item.completed)
            .forEach(x => u.searchParams.append("id", x.id));
        const r = await fetch(u, {
            method: "PATCH",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ completed: item.completed })
        });
        if (r.ok) {
            this.customState.data.forEach(x => x.completed = item.completed);
            this.dispatchEvent(new Event("datachanged"));
        }
    }

    async toggleItem(item) {
        const a = this.closest("app-element");
        const r = await fetch(`${a.customEnv.apiUrl}/todo-items/${item.id}`, {
            method: "PATCH",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ completed: item.completed })
        });
        const j = await r.json();
        if (r.ok) {
            const s = this.customState;
            const i = s.data.findIndex(x => x.id == item.id);
            s.data[i] = j;
            this.dispatchEvent(new Event("datachanged"));
        }
    }

    async updateItem(item) {
        const a = this.closest("app-element");
        const r = await fetch(`${a.customEnv.apiUrl}/todo-items/${item.id}`, {
            method: "PATCH",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ title: item.title })
        });
        const j = await r.json();
        if (r.ok) {
            const s = this.customState;
            const i = s.data.findIndex(x => x.id == item.id);
            s.data[i] = j;
            this.dispatchEvent(new Event("datachanged"));
        }
    }
}

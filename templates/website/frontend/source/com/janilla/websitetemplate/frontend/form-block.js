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

export default class FormBlock extends WebComponent {

    static get moduleUrl() {
        return import.meta.url;
    }

    static get templateNames() {
        return ["form-block"];
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener("submit", this.handleSubmit);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("submit", this.handleSubmit);
    }

    async updateDisplay() {
        const s = this.customState;
        s.data = this.closest("[data-slug]").data(this.dataset.path);
        if (s.submission && s.data.form?.confirmationType?.name === "REDIRECT") {
            this.closest("app-element").navigate(new URL(s.data.form.redirect, location.href));
            return;
        }

        this.appendChild(this.interpolateDom({
            $template: "",
            intro: !s.submission && s.data.enableIntro ? {
                $template: "intro",
                ...s.data
            } : null,
            form: !s.submission && s.data.form ? {
                $template: "form",
                fields: s.data.form.fields.map(x => ({
                    $template: "field",
                    ...x,
                    control: (() => {
                        const t = x.$type.replace(/Field$/, "").toLowerCase();
                        return {
                            $template: t === "textarea" ? "textarea" : "input",
                            ...x,
                            type: ["text", "textarea"].includes(t) ? null : t
                        };
                    })()
                }))
            } : null,
            confirmation: s.submission ? {
                $template: "confirmation",
                ...s.data.form
            } : null
        }));
    }

    handleSubmit = async event => {
        const el = event.target;
        event.preventDefault();

        const a = this.closest("app-element");
        const s = this.customState;
        const ee = [...new FormData(el).entries()];
        this.customState.submission = await (await fetch(`${a.dataset.apiUrl}/form-submissions`, {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({
                $type: "FormSubmission",
                form: s.data.form.id,
                submissionData: ee.map(([k, v]) => ({
                    field: k,
                    value: v
                }))
            })
        })).json();
        this.requestDisplay();
    }
}

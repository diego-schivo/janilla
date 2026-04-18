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
import { matchNode } from "/base/test-bench.js";

const delay = () => new Promise(x => setTimeout(x, 500));

const untilElement = (tag, text) => async context => await matchNode(`//text()[contains(.,"${text}")]/ancestor::${tag}`, context, false);

const whileElement = (tag, text) => async context => await matchNode(`//text()[contains(.,"${text}")]/ancestor::${tag}`, context, true);

const foo = async content => {
    const b = content.body;
    (await untilElement("a", "Go to admin panel")(b)).click();
    await delay();
    (await untilElement("li", "Email")(b)).querySelector("input").value = "demo-author@janilla.com";
    await delay();
    (await untilElement("li", "New Password")(b)).querySelector("input").value = "password";
    await delay();
    (await untilElement("li", "Confirm Password")(b)).querySelector("input").value = "password";
    await delay();
    (await untilElement("button", "Create")(b)).click();
    await delay();
    (await untilElement("a", "Users")(b)).click();
    await delay();
    (await untilElement("a", "1")(b)).click();
    await delay();

    {
        const el = (await untilElement("li", "Name")(b)).querySelector("input");
        el.value = "Demo Author";
        el.dispatchEvent(new Event("input", { bubbles: true }));
    }

    await delay();
    (await untilElement("button", "Save")(b)).click();
    await delay();
    await untilElement("li", "Updated successfully.")(b);
}

export default {
    "Foo": foo
};

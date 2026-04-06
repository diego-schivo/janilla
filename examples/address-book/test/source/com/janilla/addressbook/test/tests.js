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
import { matchNode } from "./test-bench.js";

const delay = () => new Promise(x => setTimeout(x, 1000));

const untilFormControl = placeholder => async context => await matchNode(`//*[contains(@placeholder,"${placeholder}")]`, context, false);

const untilElement = (tag, text) => async context => await matchNode(`//text()[contains(.,"${text}")]/ancestor::${tag}`, context, false);

const whileElement = (tag, text) => async context => await matchNode(`//text()[contains(.,"${text}")]/ancestor::${tag}`, context, true);

const createContact = async content => {
	const b = content.body;
	(await untilElement("sidebar-layout", "demo")(b)).shadowRoot.querySelector("button").click();
	await delay();
	(await untilFormControl("First")(b)).value = "Foo";
	await delay();
	(await untilFormControl("Last")(b)).value = "Bar";
	await delay();
	(await untilFormControl("avatar")(b)).value = "https://avatars.githubusercontent.com/u/4830475?v=4";
	await delay();
	(await untilElement("button", "Save")(b)).click();
	await delay();
	await untilElement("h1", "Foo Bar")(b);
}

const deleteContact = async content => {
	const b = content.body;
	(await untilElement("sidebar-layout", "demo")(b)).shadowRoot.querySelector("nav a").click();
	await delay();
	const w = document.querySelector("iframe").contentWindow;
	const c = w.confirm;
	w.confirm = () => {
		w.confirm = c;
		return true;
	}
	(await untilElement("button", "Delete")(b)).click();
	await delay();
	await untilElement("sidebar-layout", "demo")(b);
}

const updateContact = async content => {
	const b = content.body;
	(await untilElement("sidebar-layout", "demo")(b)).shadowRoot.querySelector("nav a").click();
	await delay();
	(await untilElement("button", "Edit")(b)).click();
	await delay();
	(await untilFormControl("Last")(b)).value = "Foo";
	await delay();
	(await untilElement("button", "Save")(b)).click();
	await delay();
	await untilElement("h1", "Alex Foo")(b);
}

export default {
	"Create contact": createContact,
	"Update contact": updateContact,
	"Delete contact": deleteContact
};

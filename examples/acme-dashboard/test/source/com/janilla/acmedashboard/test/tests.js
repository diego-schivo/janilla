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
import { matchNode } from "./test-bench.js";

const delay = () => new Promise(x => setTimeout(x, 100));

const untilFormControl = placeholder => async context => await matchNode(`//*[contains(@placeholder,"${placeholder}")]`, context, false);

const untilElement = (tag, text) => async context => await matchNode(`//text()[contains(.,"${text}")]/ancestor::${tag}`, context, false);

const whileElement = (tag, text) => async context => await matchNode(`//text()[contains(.,"${text}")]/ancestor::${tag}`, context, true);

const login = (email, password) => {
	return async content => {
		const b = content.body;
		(await untilElement("a", "Log in")(b)).click();
		await delay();
		(await untilFormControl("email")(b)).value = email;
		await delay();
		(await untilFormControl("password")(b)).value = password;
		await delay();
		(await untilElement("button", "Log in")(b)).click();
	};
}

const createInvoice = async content => {
	await login("user@nextmail.com", "123456")(content);
	await delay();
	const b = content.body;
	await untilElement("h1", "Dashboard")(b);
	await delay();
	/*
	console.log(b.querySelector("dashboard-layout"));
	console.log(Array.from(b.querySelector("dashboard-layout").shadowRoot.querySelectorAll("dashboard-nav a"))
		.find(x => x.textContent.includes("Invoices")));
	console.log("a");
	b.querySelector("dashboard-layout").foo();
	b.querySelector("dashboard-layout").shadowRoot.querySelector("dashboard-nav").bar();
	console.log("b");
	*/
	Array.from(b.querySelector("dashboard-layout").shadowRoot.querySelectorAll("dashboard-nav a"))
		.find(x => x.textContent.includes("Invoices"))
		.click();
	//console.log("c");
	await delay();
	(await untilElement("a", "Create Invoice")(b)).click();
	await delay();
	(await untilElement("option", "customer")(b)).closest("select").selectedIndex = 1;
	await delay();
	(await untilFormControl("amount")(b)).value = "123.45";
	await delay();
	(await untilElement("label", "Pending")(b)).click();
	await delay();
	(await untilElement("button", "Create Invoice")(b)).click();
	await delay();
	await untilElement("intl-format", "$123.45")(b);
}

const deleteInvoice = async content => {
	await login("user@nextmail.com", "123456")(content);
	await delay();
	const b = content.body;
	await untilElement("h1", "Dashboard")(b);
	await delay();
	Array.from(b.querySelector("dashboard-layout").shadowRoot.querySelectorAll("dashboard-nav a"))
		.find(x => x.textContent.includes("Invoices"))
		.click();
	await delay();
	(await untilElement("tr", "Amy Burns")(b)).querySelector("button").click();
	await delay();
	await whileElement("tr", "Amy Burns")(b);
	await delay();
}

const loginUser = async content => {
	await login("user@nextmail.com", "123456")(content);
	await delay();
	await untilElement("h1", "Dashboard")(content.body);
}

const logoutUser = async content => {
	await login("user@nextmail.com", "123456")(content);
	await delay();
	const b = content.body;
	(await untilElement("h1", "Dashboard")(b)).closest("dashboard-layout").shadowRoot.querySelector("button").click();
	await delay();
	(await untilElement("h1", "log in")(b));
}

const updateInvoice = async content => {
	await login("user@nextmail.com", "123456")(content);
	await delay();
	const b = content.body;
	await untilElement("h1", "Dashboard")(b);
	await delay();
	Array.from(b.querySelector("dashboard-layout").shadowRoot.querySelectorAll("dashboard-nav a"))
		.find(x => x.textContent.includes("Invoices"))
		.click();
	await delay();
	(await untilElement("tr", "Amy Burns")(b)).querySelector("a").click();
	await delay();
	(await untilElement("option", "customer")(b)).closest("select").selectedIndex = 2;
	await delay();
	(await untilFormControl("amount")(b)).value = "123.45";
	await delay();
	(await untilElement("label", "Pending")(b)).click();
	await delay();
	(await untilElement("button", "Edit Invoice")(b)).click();
	await delay();
	await untilElement("intl-format", "$123.45")(b);
}

export default {
	"Login user": loginUser,
	"Logout user": logoutUser,
	"Create invoice": createInvoice,
	"Update invoice": updateInvoice,
	"Delete invoice": deleteInvoice
};

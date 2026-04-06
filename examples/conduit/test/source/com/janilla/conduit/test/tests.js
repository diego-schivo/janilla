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
import { matchNode } from "./test-bench.js";

const delay = () => new Promise(x => setTimeout(x, 10));

const untilFormControl = placeholder => async context => await matchNode(`//*[contains(@placeholder,"${placeholder}")]`, context, false);

const untilElement = (tag, text) => async context => await matchNode(`//text()[contains(.,"${text}")]/ancestor::${tag}`, context, false);

const whileElement = (tag, text) => async context => await matchNode(`//text()[contains(.,"${text}")]/ancestor::${tag}`, context, true);

const login = (email, password) => {
	return async content => {
		const b = content.body;
		(await untilElement("nav-link", "Sign in")(b)).shadowRoot.querySelector("a").click();
		await delay();
		(await untilFormControl("Email")(b)).value = email;
		await delay();
		(await untilFormControl("Password")(b)).value = password;
		await delay();
		(await untilElement("button", "Sign in")(b)).click();
	};
}

const addComment = async content => {
	await login("aute.cillum@lorem.ipsum", "aute")(content);
	await delay();
	const b = content.body;
	(await untilElement("nav-link", "Global Feed")(b)).shadowRoot.querySelector("a").click();
	await delay();
	(await untilElement("a", "Tempor ex")(b)).click();
	await delay();
	(await untilFormControl("comment")(b)).value = `This is the first line.  
And this is the second line.`;
	await delay();
	(await untilElement("button", "Post Comment")(b)).click();
	await delay();
	await untilElement("p", "second line")(b);
}

const createArticle = async content => {
	await login("aute.cillum@lorem.ipsum", "aute")(content);
	await delay();
	const b = content.body;
	(await untilElement("nav-link", "New Article")(b)).shadowRoot.querySelector("a").click();
	await delay();
	(await untilFormControl("Title")(b)).value = "Foo bar";
	await delay();
	(await untilFormControl("about")(b)).value = "Foo bar baz qux.";
	await delay();
	(await untilFormControl("markdown")(b)).value = `# Heading level 1

This is the first line.  
And this is the second line.
`;
	await delay();
	const c = await untilFormControl("tags")(b);
	for (const v of ["foo", "bar"]) {
		c.value = v;
		c.dispatchEvent(new KeyboardEvent("keydown", { bubbles: true, cancelable: true, key: "Enter" }));
		await delay();
	}
	(await untilElement("button", "Publish Article")(b)).click();
	await delay();
	await untilElement("h1", "Foo bar")(b);
}

const deleteArticle = async content => {
	await login("aute.cillum@lorem.ipsum", "aute")(content);
	await delay();
	const b = content.body;
	(await untilElement("nav-link", "Aute Cillum")(b)).shadowRoot.querySelector("a").click();
	await delay();
	(await untilElement("a", "Cillum reprehenderit")(b)).click();
	await delay();
	(await untilElement("button", "Delete Article")(b)).click();
	await delay();
	await untilElement("nav-link", "Your Feed")(b);
}

const deleteComment = async content => {
	await login("voluptate.enim@lorem.ipsum", "voluptate")(content);
	await delay();
	const b = content.body;
	(await untilElement("nav-link", "Global Feed")(b)).shadowRoot.querySelector("a").click();
	await delay();
	(await untilElement("a", "Tempor ex")(b)).click();
	await delay();
	(await untilElement("p", "Ullamco ea sit excepteur reprehenderit dolore commodo aliquip dolore ipsum.")(b)).closest(".card").querySelector(".ion-trash-a").click();
	await delay();
	await whileElement("p", "Ullamco ea sit excepteur reprehenderit dolore commodo aliquip dolore ipsum.")(b);
}

const favoriteArticle = async content => {
	await login("aute.cillum@lorem.ipsum", "aute")(content);
	await delay();
	const b = content.body;
	(await untilElement("nav-link", "Global Feed")(b)).shadowRoot.querySelector("a").click();
	await delay();
	const p = (await untilElement("h1", "Tempor ex")(b)).closest(".article-preview");
	p.querySelector(".ion-heart").click();
	await delay();
	await untilElement("button", "3")(p);
}

const followUser = async content => {
	await login("aute.cillum@lorem.ipsum", "aute")(content);
	await delay();
	const b = content.body;
	(await untilElement("nav-link", "Global Feed")(b)).shadowRoot.querySelector("a").click();
	await delay();
	(await untilElement("a", "Tempor ex")(b)).click();
	await delay();
	(await untilElement("follow-button", "Follow")(b)).querySelector("button").click();
	await delay();
	(await untilElement("a", "conduit")(b)).click();
	await delay();
	await untilElement("a", "Tempor ex")(b);
}

const loginUser = async content => {
	await login("aute.cillum@lorem.ipsum", "aute")(content);
	await delay();
	const b = content.body;
	(await untilElement("nav-link", "Aute Cillum")(b)).shadowRoot.querySelector("a").click();
}

const logoutUser = async content => {
	await login("aute.cillum@lorem.ipsum", "aute")(content);
	await delay();
	const b = content.body;
	(await untilElement("nav-link", "Aute Cillum")(b)).shadowRoot.querySelector("a").click();
	await delay();
	(await untilElement("a", "Edit Profile Settings")(b)).click();
	await delay();
	(await untilElement("button", "logout")(b)).click();
	await delay();
	(await untilElement("nav-link", "Sign in")(b));
}

const registerUser = async content => {
	const b = content.body;
	(await untilElement("nav-link", "Sign up")(b)).shadowRoot.querySelector("a").click();
	await delay();
	(await untilFormControl("Username")(b)).value = "Foo Bar";
	await delay();
	(await untilFormControl("Email")(b)).value = "foo.bar@lorem.ipsum";
	await delay();
	(await untilFormControl("Password")(b)).value = "foo";
	await delay();
	(await untilElement("button", "Sign up")(b)).click();
	await delay();
	(await untilElement("nav-link", "Foo Bar")(b)).shadowRoot.querySelector("a").click();
}

const unfavoriteArticle = async content => {
	await login("aute.cillum@lorem.ipsum", "aute")(content);
	await delay();
	const b = content.body;
	(await untilElement("nav-link", "Aute Cillum")(b)).shadowRoot.querySelector("a").click();
	await delay();
	(await untilElement("nav-link", "Favorited Articles")(b)).shadowRoot.querySelector("a").click();
	await delay();
	(await untilElement("h1", "Sit voluptate")(b)).click();
	await delay();
	const e = (await untilElement("button", "Unfavorite Article")(b));
	e.click();
	await delay();
	await untilElement("span", "1")(e);
}

const unfollowUser = async content => {
	await login("aute.cillum@lorem.ipsum", "aute")(content);
	await delay();
	const b = content.body;
	(await untilElement("a", "Nulla Officia")(b)).click();
	await delay();
	(await untilElement("follow-button", "Unfollow")(b)).querySelector("button").click();
	await delay();
	await untilElement("follow-button", "Follow")(b);
}

const updateArticle = async content => {
	await login("aute.cillum@lorem.ipsum", "aute")(content);
	await delay();
	const b = content.body;
	(await untilElement("nav-link", "Aute Cillum")(b)).shadowRoot.querySelector("a").click();
	await delay();
	(await untilElement("a", "Cillum reprehenderit")(b)).click();
	await delay();
	(await untilElement("a", "Edit Article")(b)).click();
	await delay();
	(await untilFormControl("Title")(b)).value = "Foo bar";
	await delay();
	(await untilFormControl("about")(b)).value = "Foo bar baz qux.";
	await delay();
	(await untilFormControl("markdown")(b)).value = `# Heading level 1

This is the first line.  
And this is the second line.
`;
	await delay();
	const c = await untilFormControl("tags")(b);
	for (const v of ["foo", "bar"]) {
		c.value = v;
		c.dispatchEvent(new KeyboardEvent("keydown", { bubbles: true, cancelable: true, key: "Enter" }));
		await delay();
	}
	(await untilElement("button", "Publish Article")(b)).click();
	await delay();
	await untilElement("h1", "Foo bar")(b);
}

const updateUser = async content => {
	await login("aute.cillum@lorem.ipsum", "aute")(content);
	await delay();
	const b = content.body;
	(await untilElement("nav-link", "Settings")(b)).shadowRoot.querySelector("a").click();
	await delay();
	(await untilFormControl("picture")(b)).value = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><text x="2" y="12.5" font-size="12">😁</text></svg>';
	await delay();
	(await untilFormControl("Name")(b)).value = "Foo Bar";
	await delay();
	(await untilFormControl("bio")(b)).value = `This is the first line.
And this is the second line.`;
	await delay();
	(await untilFormControl("Email")(b)).value = "foo.bar@lorem.ipsum";
	await delay();
	(await untilFormControl("Password")(b)).value = "foo";
	await delay();
	(await untilElement("button", "Update Settings")(b)).click();
	await delay();
	(await untilElement("nav-link", "Foo Bar")(b)).shadowRoot.querySelector("a").click();
	await delay();
}

export default {
	"Login user": loginUser,
	"Register user": registerUser,
	"Update user": updateUser,
	"Logout user": logoutUser,
	"Follow user": followUser,
	"Unfollow user": unfollowUser,
	"Create article": createArticle,
	"Update article": updateArticle,
	"Delete article": deleteArticle,
	"Add comment": addComment,
	"Delete comment": deleteComment,
	"Favorite article": favoriteArticle,
	"Unfavorite article": unfavoriteArticle
};

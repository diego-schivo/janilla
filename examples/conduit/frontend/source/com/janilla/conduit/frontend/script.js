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
import App from "./app.js";
import Article from "./article.js";
import Articles from "./articles.js";
import ArticlePreview from "./article-preview.js";
import Comments from "./comments.js";
import Editor from "./editor.js";
import Errors from "./errors.js";
import FavoriteButton from "./favorite-button.js";
import FollowButton from "./follow-button.js";
import Home from "./home.js";
import IntlFormat from "./intl-format.js";
import Login from "./login.js";
import NavLink from "./nav-link.js";
import PageDisplay from "./page-display.js";
import PaginationNav from "./pagination-nav.js";
import PopularTags from "./popular-tags.js";
import Profile from "./profile.js";
import Register from "./register.js";
import Settings from "./settings.js";
import TagsInput from "./tags-input.js";

customElements.define("app-element", App);
customElements.define("article-element", Article);
customElements.define("articles-element", Articles);
customElements.define("article-preview", ArticlePreview);
customElements.define("comments-element", Comments);
customElements.define("editor-element", Editor);
customElements.define("errors-element", Errors);
customElements.define("favorite-button", FavoriteButton);
customElements.define("follow-button", FollowButton);
customElements.define("home-element", Home);
customElements.define("intl-format", IntlFormat);
customElements.define("login-element", Login);
customElements.define("nav-link", NavLink);
customElements.define("page-display", PageDisplay);
customElements.define("pagination-nav", PaginationNav);
customElements.define("popular-tags", PopularTags);
customElements.define("profile-element", Profile);
customElements.define("register-element", Register);
customElements.define("settings-element", Settings);
customElements.define("tags-input", TagsInput);

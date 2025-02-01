/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
function parseMarkdown(text) {
	const s = [];
	{
		const e = { type: "Document", children: [], links: [] };
		s.push(e);
	}
	text.split("\n").forEach((l, i, ll) => {
		const a = [s[0]], b = ["Document"];
		for (let j = 1; ; j++) {
			let t = undefined;
			if (!t) {
				const m = l.match(/^>([ >]|$)/);
				if (m) {
					t = "Blockquote";
					l = l.substring(m[1] === " " ? 2 : 1);
				}
			}
			if (!t) {
				const m = l.match(/^    |^\t/);
				if (m) {
					if (["OrderedList", "UnorderedList"].includes(b.at(-1))) {
						t = "ListItem";
						l = l.substring(m[0].length);
					} else if (s[j].type.endsWith("List"))
						t = s[j].type;
					else {
						t = "CodeBlock";
						l = l.substring(m[0].length);
					}
				}
			}
			if (!t) {
				const m = l.match(/^\d+\. /);
				if (m) {
					if (b.at(-1) === "OrderedList") {
						t = "!ListItem";
						l = l.substring(2);
					} else
						t = "OrderedList";
				}
			}
			if (!t) {
				const m = l.match(/^[-*+] /);
				if (m) {
					if (b.at(-1) === "UnorderedList") {
						t = "!ListItem";
						l = l.substring(2);
					} else
						t = "UnorderedList";
				}
			}
			if (!t && l.length) {
				let m = l.match(/^ *\[(.*?)\]: /);
				if (m) {
					const o = {};
					s[0].links[m[1]] = o;
					l = l.substring(m[0].length);
					m = l.match(/ "(.*?)"$| "(.*?)"$| \((.*?)\)$/);
					if (m) {
						o.title = m[1] ?? m[2] ?? m[3];
						l = l.substring(0, l.length - m[0].length);
					}
					if (l.startsWith("<") && l.endsWith(">")) l = l.substring(1, l.length - 1);
					o.href = l;
					return;
				}
				t = "Paragraph";
			}

			if (t)
				b.push(t);

			const e = s[j];
			if (e) {
				const u = e.type;
				if (b.length > 1 ? (t === u && a.length === j) : (u !== "Paragraph"))
					a.push(e);
			}

			if (b.length > 1 || j >= s.length - 1)
				if (!t || ["CodeBlock", "Paragraph"].includes(t))
					break;
		}

		while (s.length > a.length)
			s.pop();

		let e = s.at(-1);
		switch (e.type) {
			case "CodeBlock":
			case "Paragraph":
				e.lines.push(l);
				break;
			default:
				for (let j = a.length; j < b.length; j++) {
					let f;
					switch (b[j]) {
						case "Blockquote":
							f = { type: "Blockquote", children: [] };
							break;
						case "CodeBlock":
							f = { type: "CodeBlock", lines: [l] };
							break;
						case "!ListItem":
							f = { type: "ListItem", children: [] };
							break;
						case "OrderedList":
							f = { type: "OrderedList", children: [] };
							break;
						case "Paragraph":
							f = { type: "Paragraph", lines: [l] };
							break;
						case "UnorderedList":
							f = { type: "UnorderedList", children: [] };
							break;
					}
					e.children.push(f);
					s.push(f);
					e = f;
				}
				break;
		}
	});
	// console.log("s[0]", s[0]);
	return s[0];
}

function formatMarkdownAsHtml(document) {
	if (!document)
		return "";
	const s = [[document]];
	const h = [];
	do {
		let q = s.at(-1);
		let e = q[0];
		switch (e.type) {
			case "Blockquote":
				h.push("<blockquote>");
				break;
			case "CodeBlock":
				const l = e.lines.flatMap((l, i) => {
					l = l.replace(/[<>]/g, m => {
						switch (m) {
							case "<":
								return "&lt;";
							case ">":
								return "&gt;";
						}
					});
					return i === 0 ? l : ["\n", l];
				});
				h.push("<pre>", "<code>", ...l, "</code>", "</pre>");
				q.shift();
				break;
			case "ListItem":
				h.push("<li>");
				break;
			case "OrderedList":
				h.push("<ol>");
				break;
			case "Paragraph":
				function r(x) {
					return x
						.replace(/\*{2}(.*?)\*{2}/g, "<strong>$1</strong>")
						.replace(/_{2}(.*?)_{2}/g, "<strong>$1</strong>")
						.replace(/\*(.*?)\*/g, "<em>$1</em>")
						.replace(/_(.*?)_/g, "<em>$1</em>")
						.replace(/`(.*?)`/g, "<code>$1</code>")
						.replace(/!\[(.*?)\]\((.*?)\)/g, (x, y, z) => {
							const m = z.match(/ "(.*?)"$/);
							return m ? `<img src="${z.substring(0, z.length - m[0].length)}" alt="${y}" title="${m[1]}" />` : `<img src="${z}" alt="${y}" />`;
						}).replace(/\[(.*?)\]\((.*?)\)/g, (x, y, z) => {
							const m = z.match(/ "(.*?)"$/);
							return m ? `<a href="${z.substring(0, z.length - m[0].length)}" title="${m[1]}">${y}</a>` : `<a href="${z}">${y}</a>`;
						}).replace(/\[(.*?)\] ?\[(.*?)\]/g, (x, y, z) => {
							const o = document.links[z];
							return o ? `<a ${Object.entries(o).map(([k, v]) => `${k}="${v}"`).join(" ")}>${y}</a>` : y;
						}).replace(/\\(\*)/g, "$1");
				}
				if (e.lines.length === 1 && /^#+ /.test(e.lines[0])) {
					const l = e.lines[0].indexOf(" ");
					h.push(`<h${l}>`, r(e.lines[0].substring(l + 1)), `</h${l}>`);
				} else if (e.lines.length === 2 && /^=+$|^-+$/.test(e.lines[1])) {
					const l = e.lines[1].startsWith("=") ? 1 : 2;
					h.push(`<h${l}>`, r(e.lines[0]), `</h${l}>`);
				} else if (e.lines.length === 1 && /^\*{3,}$|^-{3,}$|^_{3,}$/.test(e.lines[0]))
					h.push("<hr />");
				else {
					const d = s.at(-2)[0];
					const t = r(e.lines.map(l => l.replace(/ {2,}$|<br>$/, "")
						.replace(/<(.*?)>/g, (x, y) => {
							return `<a href="${y.includes("@") ? "mailto:" : ""}${y}">${y}</a>`;
						}))
						.join("<br />"));
					if (d.type === "ListItem" && e === d.children[0] && !["Blockquote", "CodeBlock", "Paragraph"].includes(d.children[1]?.type))
						h.push(t);
					else
						h.push("<p>", t, "</p>");
				}
				q.shift();
				break;
			case "UnorderedList":
				h.push("<ul>");
				break;
		}

		if (e.children) {
			q = [...e.children];
			s.push(q);
		}

		while (!q.length) {
			s.pop();
			if (!s.length)
				break;
			q = s.at(-1);
			e = q.shift();
			switch (e.type) {
				case "Blockquote":
					h.push("</blockquote>");
					break;
				case "ListItem":
					h.push("</li>");
					break;
				case "OrderedList":
					h.push("</ol>");
					break;
				case "UnorderedList":
					h.push("</ul>");
					break;
			}
		}
	} while (s.length);
	// console.log("h", h);
	return h.join("");
}

export { parseMarkdown, formatMarkdownAsHtml };

/*
 * Copyright (c) 2024, Diego Schivo. All rights reserved.
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
export default function convertMarkdownIntoHTML(text) {
	const l = {}, h = [], s = [];
	let k;
	text.split('\n').filter(e => {
		const m = e.match(/^\[(.*)?\]: /);
		if (!m)
			return true;
		e = e.substring(m[0].length);
		const n = e.match(/^(([^<].*?)|<(.*?)>) ("(.*?)"|'(.*?)'|\((.*?)\))$/);
		const o = { href: n ? (n[2] ?? n[3]) : e };
		if (n) o.title = n[5] ?? n[6] ?? n[7];
		l[m[1]] = o;
		return false;
	}).forEach((e, i, a) => {
		if (k > 0) {
			k--;
			return;
		}

		if (s.at(-1) === 'code') {
			e = e.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;') + '\n';
			h.push(e);
			return;
		}

		/*
		if (!/\S/.test(e))
			return;
		*/

		let m = e.match(/^(    |\t)+/);
		let j = m ? m[1].replaceAll('    ', '\t').length : 0;
		if (m)
			e = e.substring(m[1].length);

		if (/^(\*\*\*+|---+|___+)$/.test(e)) {
			h.push('<hr />');
			return;
		}

		m = e.match(/^(\d+\.|[-*+]) ?/);
		if (j === s.length + 1) {
			s.push('pre', 'code');
			h.push(`<${s.at(-2)}>`, `<${s.at(-1)}>`);
		} else if (s.length === j) {
			if (m)
				s[j] = `${m[1].at(-1) === '.' ? 'ol' : 'ul'}`;
			else {
				let n = e.match(/^(#+) /);
				if (n) {
					s[j] = `h${n[1].length}`;
					e = e.substring(n[0].length);
				} else {
					n = e.match(/^(> +)/);
					if (n) {
						s[j] = 'blockquote';
						e = e.substring(n[0].length);
					} else
						s[j] = 'p';
				}
			}
			h.push(`<${s.at(-1)}>`);
		} else if (s.length > j) {
			if (['ol', 'ul'].includes(s.at(-1)))
				h.push('</li>');
			// while (s.length > j + 1)
			while (s.length > j)
				h.push(`</${s.pop()}>`);
		}
		if (m) {
			e = e.substring(m[1].length);
			h.push('<li>', e);
			return;
		}
		if (s.length > j && ['ol', 'ul'].includes(s.at(-1)))
			h.push('</li>');

		/*
		m = e.match(/^(#+) /);
		if (m) {
			e = e.substring(m[0].length);
			h.push(`<h${m[1].length}>`, e, `</h${m[1].length}>`);
			return;
		}

		m = i + 1 < a.length && a[i + 1].match(/^(=+|-+)$/);
		if (m) {
			h.push(`<h${m[1].startsWith('=') ? 1 : 2}>`, e, `</h${m[1].startsWith('=') ? 1 : 2}>`);
			k = 1;
			return;
		}
		*/

		e = e.replace(/<(.*?)>/g, (_, p1) => `<a href="${p1.includes('@') ? 'mailto:' : ''}${p1}">${p1}</a>`);
		e = e.replace(/\!\[(.*?)\]\((.*?)\)/g, (_, p1, p2) => {
			const n = p2.match(/^(.*?) "(.*?)"$/);
			return `<img src="${n ? n[1] : p2}"${n ? ` title="${n[2]}"` : ''} alt="${p1}" />`;
		});
		e = e.replace(/\[(.*?)\] ?\[(.*?)\]/g, (_, p1, p2) => {
			const o = l[p2];
			return `<a href="${o.href}"${o.title ? ` title="${o.title}"` : ''}>${p1}</a>`;
		});
		e = e.replace(/\[(.*?)\]\((.*?)\)/g, (_, p1, p2) => {
			const n = p2.match(/^(.*?) "(.*?)"$/);
			return `<a href="${n ? n[1] : p2}"${n ? ` title="${n[2]}"` : ''}>${p1}</a>`;
		});
		m = e.match(/( +)$/);
		if (m && m[1].length >= 2)
			e = e.substring(0, e.length - m[1].length) + '<br />';
		h.push(e);
	});
	if (s.length) {
		if (['ol', 'ul'].includes(s.at(-1)))
			h.push('</li>');
		while (s.length)
			h.push(`</${s.pop()}>`);
	}
	// console.log(h);
	return h.join('');
}

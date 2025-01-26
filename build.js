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
const fs = require("fs").promises;
const path = require("path");

const files = [
	"./node_modules/janillas/dom-utils.js",
	"./node_modules/janillas/markdown.js",
	"./node_modules/janillas/test-bench.css",
	"./node_modules/janillas/test-bench.html",
	"./node_modules/janillas/test-bench.js",
	"./node_modules/janillas/updatable-html-element.js"
];

const copy = async (src, dest) => {
	await fs.copyFile(src, dest);
};

const build = async () => {
	for (const f of files)
		await copy(f, path.join("./source/com/janilla/frontend", path.basename(f)));
};

build();

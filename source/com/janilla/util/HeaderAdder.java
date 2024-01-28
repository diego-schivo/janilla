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
package com.janilla.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

class HeaderAdder {

	public static void main(String[] args) {
		var h = Map.of("janilla", """
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
				 */""".split("\n"), "janilla-conduit", """
				/*
				 * MIT License
				 *
				 * Copyright (c) 2024 Diego Schivo
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
				 */""".split("\n"), "janilla-website", """
				/*
				 * MIT License
				 *
				 * Copyright (c) 2024 Diego Schivo
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
				 */""".split("\n"), "janilla-petclinic", """
				/*
				 * Copyright 2012-2024 the original author or authors.
				 *
				 * Licensed under the Apache License, Version 2.0 (the "License");
				 * you may not use this file except in compliance with the License.
				 * You may obtain a copy of the License at
				 *
				 *      https://www.apache.org/licenses/LICENSE-2.0
				 *
				 * Unless required by applicable law or agreed to in writing, software
				 * distributed under the License is distributed on an "AS IS" BASIS,
				 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
				 * See the License for the specific language governing permissions and
				 * limitations under the License.
				 */""".split("\n"));
		var s = Path.of(System.getProperty("user.home")).resolve("git");
		try {
			Files.walkFileTree(s, new SimpleFileVisitor<>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					var n = dir.getFileName().toString();
					if (dir.getParent().equals(s)) {
						if (!h.containsKey(n) || n.equals("bin") || n.equals("target"))
							return FileVisitResult.SKIP_SUBTREE;
					}
					if (n.startsWith(".") || n.equals("src") || n.equals("bin") || n.equals("target"))
						return FileVisitResult.SKIP_SUBTREE;
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					var n = file.getFileName().toString();
					if (!n.endsWith(".java") && !n.endsWith(".js"))
						return FileVisitResult.CONTINUE;
					var l = Files.readAllLines(file);
					var i = l.iterator();
					var c = new Consumer<String>() {

						int state;

						@Override
						public void accept(String t) {
							state = switch (state) {
							case 0 -> t.equals("/*") ? 1 : -1;
							case 1 -> t.startsWith(" *") ? t.equals(" */") ? 2 : 1 : -1;
							case 2 -> 3;
							default -> throw new RuntimeException();
							};
						}
					};
					var a = Stream.iterate(i.hasNext() ? i.next() : null, t -> {
						if (t == null)
							return false;
						c.accept(t);
						return c.state == 1 || c.state == 2;
					}, t -> i.hasNext() ? i.next() : null).toArray(String[]::new);
					var b = h.get(s.relativize(file).getName(0).toString());
					if (!Arrays.equals(a, b)) {
						System.out.println(file);
						Files.write(file, Stream.concat(Arrays.stream(b), l.stream().skip(a.length)).toList());
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

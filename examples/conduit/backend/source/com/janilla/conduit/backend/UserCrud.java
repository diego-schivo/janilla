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
package com.janilla.conduit.backend;

import com.janilla.backend.persistence.DefaultCrud;
import com.janilla.backend.persistence.Persistence;

public class UserCrud extends DefaultCrud<Long, User> {

	public UserCrud(Persistence persistence) {
		super(User.class, persistence.idConverter(User.class), persistence);
	}

	public boolean follow(Long profile, Long user) {
		return persistence.database().perform(
				() -> persistence.database().index("User.followList").insert(new Object[] { user, profile }, null),
				true);
	}

	public boolean unfollow(Long profile, Long user) {
		return persistence.database().perform(
				() -> persistence.database().index("User.followList").delete(new Object[] { user, profile }, null),
				true) != null;
	}
}

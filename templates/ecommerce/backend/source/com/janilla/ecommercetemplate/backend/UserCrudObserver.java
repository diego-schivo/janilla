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
package com.janilla.ecommercetemplate.backend;

import com.janilla.backend.persistence.CrudObserver;
import com.janilla.backend.persistence.Persistence;
import com.janilla.cms.User;
import com.janilla.ecommercetemplate.Address;
import com.janilla.ecommercetemplate.Cart;
import com.janilla.ecommercetemplate.EcommerceDomain;
import com.janilla.ecommercetemplate.EcommerceUser;

public class UserCrudObserver implements CrudObserver<User<?>> {

	protected final Persistence persistence;

	protected final EcommerceDomain domain;

	public UserCrudObserver(Persistence persistence, EcommerceDomain domain) {
		this.persistence = persistence;
		this.domain = domain;
	}

	@Override
	public User<?> afterRead(User<?> entity) {
		var e = (EcommerceUser<?>) entity;
		var cc = persistence.crud(Cart.class).filter("customer", new Object[] { e.id() });
		e = e.withCarts(cc.stream().map(x -> domain.emptyCart().withId(x)).toList());
		var aa = persistence.crud(Address.class).filter("customer", new Object[] { e.id() });
		e = e.withAddresses(aa.stream().map(x -> domain.emptyAddress().withId(x)).toList());
		return e;
	}

	@Override
	public User<?> beforeCreate(User<?> entity) {
		var e = (EcommerceUser<?>) entity;
		if (e.carts() != null)
			e = e.withCarts(null);
		if (e.addresses() != null)
			e = e.withAddresses(null);
		return e;
	}

	@Override
	public User<?> beforeUpdate(User<?> entity) {
		return beforeCreate(entity);
	}

//	@Override
//	public void afterUpdate(User entity1, User entity2) {
//		var cc = persistence.crud(Cart.class).filter("customer", entity1.id());
//		if (entity2.carts() != null)
//			cc = cc.stream().filter(x -> !entity2.carts().contains(x)).toList();
//		persistence.crud(Cart.class).delete(cc);
//		
//		var aa = persistence.crud(Address.class).filter("customer", entity1.id());
//		if (entity2.addresses() != null)
//			aa = aa.stream().filter(x -> !entity2.addresses().contains(x)).toList();
//		persistence.crud(Address.class).delete(aa);
//	}

	@Override
	public void afterDelete(User<?> entity) {
		var cc = persistence.crud(Cart.class).filter("customer", new Object[] { entity.id() });
		persistence.crud(Cart.class).delete(cc);
		var aa = persistence.crud(Address.class).filter("customer", new Object[] { entity.id() });
		persistence.crud(Address.class).delete(aa);
	}
}

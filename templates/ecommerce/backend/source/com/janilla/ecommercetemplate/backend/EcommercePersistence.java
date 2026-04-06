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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.janilla.backend.persistence.Crud;
import com.janilla.backend.persistence.CrudObserver;
import com.janilla.backend.sqlite.SqliteDatabase;
import com.janilla.cms.User;
import com.janilla.ecommercetemplate.Cart;
import com.janilla.ecommercetemplate.Product;
import com.janilla.ecommercetemplate.VariantType;
import com.janilla.ioc.DiFactory;
import com.janilla.java.Converter;
import com.janilla.java.Property;
import com.janilla.persistence.Entity;
import com.janilla.websitetemplate.backend.WebsitePersistence;

public class EcommercePersistence extends WebsitePersistence {

	public EcommercePersistence(SqliteDatabase database, List<Class<? extends Entity<?>>> storables,
//			TypeResolver typeResolver, 
			Converter converter, DiFactory diFactory, Properties configuration, String configurationKey) {
		super(database, storables, converter, diFactory, configuration, configurationKey);
	}

	@Override
	protected <E extends Entity<?>> Crud<?, E> newCrud(Class<E> type) {
		var c = super.newCrud(type);
		if (c != null) {
			Class<? extends CrudObserver<?>> t;
			if (type.equals(Cart.class))
				t = CartCrudObserver.class;
			else if (type.equals(Product.class))
				t = ProductCrudObserver.class;
			else if (type.equals(User.class))
				t = UserCrudObserver.class;
			else if (type.equals(VariantType.class))
				t = VariantTypeCrudObserver.class;
			else
				t = null;
			if (t != null) {
				@SuppressWarnings("unchecked")
				var o = (CrudObserver<E>) diFactory.newInstance(t, Map.of("persistence", this));
				c.observers().add(o);
			}
		}
		return c;
	}

	@Override
	protected Class<?> seedDataClass() {
		return SeedData.class;
	}

	@Override
	protected List<Property> properties() {
		var pp = super.properties();
		var ii = Stream.of("products", "variants", "carts").mapToInt(
				x -> IntStream.range(0, pp.size()).filter(y -> pp.get(y).name().equals(x)).findFirst().orElseThrow())
				.toArray();
		var i = Arrays.stream(ii).min().getAsInt();
		var pp2 = Arrays.stream(ii).mapToObj(pp::get).toList();
		pp.removeAll(pp2);
		pp.addAll(i, pp2);
		return pp;
	}
}

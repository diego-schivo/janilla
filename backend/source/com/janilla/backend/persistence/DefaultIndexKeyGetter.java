package com.janilla.backend.persistence;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.janilla.java.Property;

public class DefaultIndexKeyGetter implements IndexKeyGetter {

	protected final Property[] properties;

	public DefaultIndexKeyGetter(Property... properties) {
		this.properties = properties;
	}

	@Override
	public Set<List<Object>> keys(Object object) {
		var l = Arrays.stream(properties).map(p -> {
			var o = p.get(object);
			return o instanceof Collection c ? List.of(c.toArray()) : List.of(o);
		}).toList();
		var ss = l.stream().mapToInt(List::size).toArray();
		var s = Arrays.stream(ss).reduce((x, y) -> x * y).getAsInt();
		var ii = new int[ss.length];
		return IntStream.range(0, s).mapToObj(_ -> {
			var k = IntStream.range(0, ii.length).mapToObj(i -> l.get(i).get(ii[i])).toList();
			for (var i = ii.length - 1; i >= 0; i--) {
				if (++ii[i] != ss[i])
					break;
				ii[i] = 0;
			}
			return k;
		}).collect(Collectors.toCollection(LinkedHashSet::new));
	}
}

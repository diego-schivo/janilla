package com.janilla.addressbook.backend;

import java.util.concurrent.ThreadLocalRandom;

import com.janilla.backend.persistence.StringIdHelper;
import com.janilla.persistence.Entity;

public class ContactIdHelper extends StringIdHelper {

	@Override
	public String random(Entity<String> entity) {
		var l = 1L + Integer.MAX_VALUE + ThreadLocalRandom.current().nextLong(Long.MAX_VALUE - Integer.MAX_VALUE);
		var s = Long.toString(l, 36);
		s = s.length() > 7 ? s.substring(s.length() - 7, s.length()) : s;
		return s;
	}

}

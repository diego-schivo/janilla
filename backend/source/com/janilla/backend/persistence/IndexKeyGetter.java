package com.janilla.backend.persistence;

import java.util.List;
import java.util.Set;

public interface IndexKeyGetter {

	Set<List<Object>> keys(Object object);
}

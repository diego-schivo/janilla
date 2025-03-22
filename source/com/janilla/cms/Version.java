package com.janilla.cms;

import java.time.Instant;

public record Version<E extends Entity>(Long id, E entity) {

	public Instant updatedAt() {
		return entity.updatedAt();
	}

	public Entity.Status status() {
		return entity.status();
	}
}

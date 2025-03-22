package com.janilla.cms;

import java.time.Instant;

public interface Entity {

	Long id();

	Instant createdAt();

	Instant updatedAt();

	Status status();

	public enum Status {

		DRAFT, PUBLISHED
	}
}

package com.janilla.todomvc;

import java.time.Instant;

import com.janilla.cms.Document;
import com.janilla.cms.DocumentStatus;
import com.janilla.persistence.Store;

@Store
public record TodoItem(Long id, String title, Boolean completed, Instant createdAt, Instant updatedAt,
		DocumentStatus documentStatus, Instant publishedAt) implements Document<Long> {
}

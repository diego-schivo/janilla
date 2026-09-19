package com.janilla.acmedashboard;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.janilla.cms.DocumentStatus;
import com.janilla.cms.User;
import com.janilla.cms.UserRole;

record UserImpl(UUID id, String name, String email, String salt, String hash, String resetPasswordToken,
		Instant resetPasswordExpiration, Set<UserRole> roles, Instant createdAt, Instant updatedAt,
		DocumentStatus documentStatus, Instant publishedAt) implements User<UUID> {
}

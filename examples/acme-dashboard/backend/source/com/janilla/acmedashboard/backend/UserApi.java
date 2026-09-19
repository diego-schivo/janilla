package com.janilla.acmedashboard.backend;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import com.janilla.backend.cms.AbstractUserApi;
import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.web.BackendConfig;
import com.janilla.blanktemplate.BlankDomain;
import com.janilla.cms.User;
import com.janilla.http.HttpExchange;
import com.janilla.java.Copier;
import com.janilla.java.Direction;
import com.janilla.web.Handle;

@Handle(path = "/api/users")
public class UserApi extends AbstractUserApi<UUID, User<UUID>> {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public UserApi(Predicate<HttpExchange> drafts, Persistence persistence, Copier copier, Direction defaultDirection,
			Integer defaultDepth, BackendConfig config, BlankDomain domain) {
		super((Class) User.class, drafts, persistence, "title", copier, defaultDirection, defaultDepth, config, domain);
	}

	@Override
	public User<UUID> firstRegister(UserData<User<UUID>> data) {
		var u = domain.withRoles(data.user(), Set.of(domain.userRole("ADMIN")));
		var d = data.withUser(u);

		return super.firstRegister(d);
	}
}

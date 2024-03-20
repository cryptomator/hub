package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public class AuthorityRepository implements PanacheRepositoryBase<Authority, String> {

	public Stream<Authority> byName(String name) {
		return find("#Authority.byName", Parameters.with("name", '%' + name.toLowerCase() + '%')).stream();
	}

	public Stream<Authority> findAllInList(List<String> ids) {
		return find("#Authority.allInList", Parameters.with("ids", ids)).stream();
	}

}

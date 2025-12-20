package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Immutable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "group_details")
@DiscriminatorValue("GROUP")
public class Group extends Authority {

	@ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
	@JoinTable(name = "group_membership",
			joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "member_id", referencedColumnName = "id")
	)
	private Set<Authority> members = new HashSet<>();

	@Immutable
	@OneToMany(mappedBy = "authority", fetch = FetchType.LAZY)
	public Set<VaultAccess> accessibleVaults = new HashSet<>();

	public Set<Authority> getMembers() {
		return members;
	}

	@Transient
	public int getMemberSize() {
		return members.size();
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<Group, String> {

		public Group findByIdWithEagerDetails(String id) {
			// 1. fetch group with members:
			var group = find("""
					FROM Group g
					LEFT JOIN FETCH g.members
					WHERE g.id = :id
					""", Parameters.with("id", id)).singleResultOptional().orElse(null);
			if (group == null) {
				return null;
			}
			// 2. fetch accessible vaults separately to avoid cartesian product explosion:
			Hibernate.initialize(group.accessibleVaults);
			return group;
		}

		public long deleteByIds(Collection<String> ids) {
			return Batch.of(200).run(ids, 0L, (batch, result) -> result + delete("id IN :ids", Parameters.with("ids", batch)));
		}

	}
}

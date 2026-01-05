package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

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

	public Set<Authority> getMembers() {
		return members;
	}

	@Transient
	public int getMemberSize() {
		return members.size();
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<Group, String> {

		public long deleteByIds(Collection<String> ids) {
			return Batch.of(200).run(ids, 0L, (batch, result) -> result + delete("id IN :ids", Parameters.with("ids", batch)));
		}

	}
}

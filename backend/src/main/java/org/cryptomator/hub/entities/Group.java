package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "group_details")
@DiscriminatorValue("GROUP")
public class Group extends Authority {

	@ManyToMany(cascade = {CascadeType.REMOVE})
	@JoinTable(name = "group_membership",
			joinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "member_id", referencedColumnName = "id")
	)
	private Set<Authority> members = new HashSet<>();

	public Set<Authority> getMembers() {
		return members;
	}

	public void setMembers(Set<Authority> members) {
		this.members = members;
	}

	public int getMemberCount() {
		return members != null ? members.size() : 0;
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<Group, String> {

		public long countMembers(String groupId) {
			return getEntityManager()
					.createQuery("SELECT COUNT(m) FROM Group g JOIN g.members m WHERE g.id = :groupId", Long.class)
					.setParameter("groupId", groupId)
					.getSingleResult();
		}
	}
}

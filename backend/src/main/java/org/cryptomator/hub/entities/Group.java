package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;

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

	@Inject
	transient Repository groupRepo;

	public Set<Authority> getMembers() {
		return members;
	}

	public void setMembers(Set<Authority> members) {
		this.members = members;
	}

	public int getMemberCount() {
		return groupRepo.countMembers(this.getId());
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<Group, String> {
		public int countMembers(String groupId) {
			return getEntityManager()
					.createQuery("SELECT SIZE(g.members) FROM Group g WHERE g.id = :groupId", Integer.class)
					.setParameter("groupId", groupId)
					.getSingleResult();
		}
	}
}

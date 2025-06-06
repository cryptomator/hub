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
import jakarta.persistence.Transient;

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

	public void setMembers(Set<Authority> members) {
		this.members = members;
	}

	@Transient
	public int getMemberSize() {
		return members.size();
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<Group, String> {
	}
}

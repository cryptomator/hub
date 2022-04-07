package org.cryptomator.hub.entities;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;

@Inheritance
@Entity
@Table(name = "group_details")
@DiscriminatorValue("GROUP")
public class Group extends Authority {

	@ManyToMany
	@JoinTable(name = "group_membership",
			joinColumns = {@JoinColumn(name = "group_id", referencedColumnName = "id"), @JoinColumn(name = "group_type", referencedColumnName = "type")},
			inverseJoinColumns = {@JoinColumn(name = "member_id", referencedColumnName = "id"), @JoinColumn(name = "member_type", referencedColumnName = "type")}
	)
	public Set<Authority> members = new HashSet<>();

	@Transactional(Transactional.TxType.REQUIRED)
	public static void createOrUpdate(String id, String name) {
		var compositeId = new AuthorityId(id, AuthorityType.GROUP);
		Group group = Authority.findById(compositeId);
		if (group == null) {
			group = new Group();
			group.id.id = id;
			group.id.type = AuthorityType.GROUP;
		}
		group.name = name;
		group.persist();
	}
}

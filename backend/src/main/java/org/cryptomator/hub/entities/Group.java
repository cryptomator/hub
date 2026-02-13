package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Immutable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@NamedNativeQuery(name = "Group.addMember", query = """
		INSERT INTO "group_membership" ("group_id", "member_id")
		VALUES (:groupId, :memberId)
		ON CONFLICT DO NOTHING
		""")
@NamedNativeQuery(name = "Group.removeMember", query = """
		DELETE FROM "group_membership"
		WHERE "group_id" = :groupId AND "member_id" = :memberId
		""")
@Entity
@Table(name = "group_details")
@DiscriminatorValue("GROUP")
public class Group extends Authority {

	@ManyToMany
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

		/**
		 * Adds the group membership mapping without loading the entities.
		 * <p>
		 * This is a lightweight alternative to calling {@link Group#getMembers() group.getMembers().add(...)} but should be avoided if the group is already present in the persistence context.
		 *
		 * @param groupId  group ID
		 * @param memberId memnber ID
		 */
		public void addMember(String groupId, String memberId) {
			getEntityManager()
					.createNamedQuery("Group.addMember")
					.setParameter("groupId", groupId)
					.setParameter("memberId", memberId)
					.executeUpdate();
		}

		/**
		 * Removes the group membership mapping without loading the entities.
		 * <p>
		 * This is a lightweight alternative to calling {@link Group#getMembers() group.getMembers().remove(...)} but should be avoided if the group is already present in the persistence context.
		 *
		 * @param groupId  group ID
		 * @param memberId memnber ID
		 */
		public void removeMember(String groupId, String memberId) {
			getEntityManager()
					.createNamedQuery("Group.removeMember")
					.setParameter("groupId", groupId)
					.setParameter("memberId", memberId)
					.executeUpdate();
		}

	}
}

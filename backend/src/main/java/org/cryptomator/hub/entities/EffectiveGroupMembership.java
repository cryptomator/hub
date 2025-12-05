package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@NamedNativeQuery(name = "EffectiveGroupMembership.fullUpdate", query = """
		INSERT INTO "effective_group_membership" ("group_id", "member_id", "path")
		WITH RECURSIVE "members" ("root", "member_id", "depth", "path") AS (
		    SELECT "group_id", "member_id", 0, '/' || "group_id" || '/' || "member_id"
		        FROM "group_membership"
		    UNION
		    SELECT "parent"."root", "child"."member_id", "parent"."depth" + 1, "parent"."path" || '/' || "child"."member_id"
		        FROM "group_membership" "child"
		        INNER JOIN "members" "parent" ON "child"."group_id" = "parent"."member_id"
		        WHERE "parent"."depth" < 10
		) SELECT "root", "member_id", "path" FROM "members"
		ON CONFLICT DO NOTHING
		""")
@NamedQuery(name = "EffectiveGroupMembership.deleteGroups", query = """
		DELETE
		FROM EffectiveGroupMembership egm
		WHERE egm.id.groupId IN :groupIds
		""")
@NamedNativeQuery(name = "EffectiveGroupMembership.updateGroups", query = """
		INSERT INTO "effective_group_membership" ("group_id", "member_id", "path")
		WITH RECURSIVE "members" ("root", "member_id", "depth", "path") AS (
		    SELECT "group_id", "member_id", 0, '/' || "group_id" || '/' || "member_id"
		        FROM "group_membership"
		        WHERE "group_id" IN :groupIds
		    UNION
		    SELECT "parent"."root", "child"."member_id", "parent"."depth" + 1, "parent"."path" || '/' || "child"."member_id"
		        FROM "group_membership" "child"
		        INNER JOIN "members" "parent" ON "child"."group_id" = "parent"."member_id"
		        WHERE "parent"."depth" < 10
		) SELECT "root", "member_id", "path" FROM "members"
		ON CONFLICT DO NOTHING
		""")
@NamedQuery(name = "EffectiveGroupMembership.deleteUsers", query = """
		DELETE
		FROM EffectiveGroupMembership egm
		WHERE egm.id.memberId IN :userIds
		""")
@NamedNativeQuery(name = "EffectiveGroupMembership.updateUsers", query = """
		INSERT INTO "effective_group_membership" ("group_id", "member_id", "path")
		WITH RECURSIVE "members" ("group_id", "member_id", "depth", "path") AS (
		    SELECT "group_id", "member_id", 0, '/' || "group_id" || '/' || "member_id"
		        FROM "group_membership"
		        WHERE "member_id" IN :userIds
		    UNION
		    SELECT "parent"."group_id", "child"."member_id", "child"."depth" + 1, '/' || "parent"."group_id" || "child"."path"
		        FROM "group_membership" "parent"
		        INNER JOIN "members" "child" ON "child"."group_id" = "parent"."member_id"
		        WHERE "child"."depth" < 10
		) SELECT "group_id", "member_id", "path" FROM "members"
		ON CONFLICT DO NOTHING
		""")
@Entity
@Immutable
@Table(name = "effective_group_membership")
public class EffectiveGroupMembership {

	@EmbeddedId
	private Id id;

	private String path;

	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "group_id")
		private String groupId;

		@Column(name = "member_id")
		private String memberId;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o instanceof Id egmId) {
				return Objects.equals(groupId, egmId.groupId) //
						&& Objects.equals(memberId, egmId.memberId);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(groupId, memberId);
		}

		@Override
		public String toString() {
			return "EffectiveGroupMembershipId{" +
					"groupId='" + groupId + '\'' +
					", memberId='" + memberId + '\'' +
					'}';
		}
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<EffectiveGroupMembership, EffectiveGroupMembership.Id> {

		public void fullUpdate() {
			deleteAll();
			getEntityManager()
				.createNamedQuery("EffectiveGroupMembership.fullUpdate")
				.executeUpdate();
		}

		public void updateGroups(Collection<String> groupIds) {
			Batch.of(200).run(groupIds, (batch) -> {
				delete("#EffectiveGroupMembership.deleteGroups", Parameters.with("groupIds", batch));
				getEntityManager()
						.createNamedQuery("EffectiveGroupMembership.updateGroups")
						.setParameter("groupIds", batch)
						.executeUpdate();
			});
		}

		public void updateUsers(Collection<String> userIds) {
			Batch.of(200).run(userIds, (batch) -> {
				delete("#EffectiveGroupMembership.deleteUsers", Parameters.with("userIds", batch));
				getEntityManager()
						.createNamedQuery("EffectiveGroupMembership.updateUsers")
						.setParameter("userIds", batch)
						.executeUpdate();
			});
		}

	}
}

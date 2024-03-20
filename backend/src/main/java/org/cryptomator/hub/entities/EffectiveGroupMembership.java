package org.cryptomator.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Immutable
@Table(name = "effective_group_membership")
@NamedQuery(name = "EffectiveGroupMembership.countEGUs", query = """
				SELECT count( DISTINCT u)
				FROM User u
				INNER JOIN EffectiveGroupMembership egm	ON u.id = egm.id.memberId
				WHERE egm.id.groupId = :groupId
		""")
@NamedQuery(name = "EffectiveGroupMembership.getEGUs", query = """
				SELECT DISTINCT u
				FROM User u
				INNER JOIN EffectiveGroupMembership egm ON u.id = egm.id.memberId
				WHERE egm.id.groupId = :groupId
		""")
public class EffectiveGroupMembership {

	@EmbeddedId
	Id id;

	String path;

	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "group_id")
		public String groupId;

		@Column(name = "member_id")
		public String memberId;

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
}

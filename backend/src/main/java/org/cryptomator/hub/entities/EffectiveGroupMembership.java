package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import org.hibernate.annotations.Immutable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Immutable
@Table(name = "effective_group_membership")
public class EffectiveGroupMembership extends PanacheEntityBase {

	@EmbeddedId
	public EffectiveGroupMembershipId id;

	public String path;

	@Embeddable
	public static class EffectiveGroupMembershipId implements Serializable {

		@Column(name = "group_id")
		public String groupId;

		@Column(name = "member_id")
		public String memberId;
	}
}

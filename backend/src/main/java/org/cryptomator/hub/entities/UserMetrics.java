package org.cryptomator.hub.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

@Entity
@Immutable
@Subselect("""
		SELECT
			u.id AS user_id,
			COUNT(g.group_id) AS direct_group_count,
			COUNT(DISTINCT eva.vault_id) AS effective_vault_count,
			COUNT(DISTINCT d.id) AS device_count
		FROM
			user_details u
		LEFT JOIN group_membership g ON g.member_id = u.id
		LEFT JOIN effective_vault_access eva ON eva.authority_id = u.id
		LEFT JOIN device d ON d.owner_id = u.id
		GROUP BY
			u.id
		""")
public class UserMetrics {

	@Id
	@Column(name = "user_id")
	private String userId;

	@OneToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
	private User user;

	@Column(name = "direct_group_count")
	private long directGroupMembershipCount;

	@Column(name = "effective_vault_count")
	private long effectiveVaultAccessCount;

	@Column(name = "device_count")
	private long deviceCount;

	public long getDirectGroupMembershipCount() {
		return directGroupMembershipCount;
	}

	public long getEffectiveVaultAccessCount() {
		return effectiveVaultAccessCount;
	}

	public long getDeviceCount() {
		return deviceCount;
	}
}

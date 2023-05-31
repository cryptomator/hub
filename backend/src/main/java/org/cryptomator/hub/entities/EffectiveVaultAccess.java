package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "effective_vault_access")
@NamedQuery(name = "EffectiveVaultAccess.countVaultAccessesOfUser", query = """
				SELECT count(eva)
				FROM EffectiveVaultAccess eva
				WHERE eva.id.authorityId = :userId
		""")
@NamedQuery(name = "EffectiveVaultAccess.countEVUs", query = """
				SELECT count(DISTINCT u)
				FROM User u
				INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		""")
@NamedQuery(name = "EffectiveVaultAccess.countEVUsInGroup", query = """
				SELECT count(DISTINCT u)
				FROM User u
				INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
				INNER JOIN EffectiveGroupMembership egm ON u.id = egm.id.memberId
				WHERE egm.id.groupId = :groupId
		""")
public class EffectiveVaultAccess extends PanacheEntityBase {

	@EmbeddedId
	public VaultAccess.Id id;

	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	public VaultAccess.Role role;

	public static boolean isUserOccupyingSeat(String userId) {
		return EffectiveVaultAccess.count("#EffectiveVaultAccess.countVaultAccessesOfUser", Parameters.with("userId", userId)) > 0;
	}

	public static long countEffectiveVaultUsers() {
		return EffectiveVaultAccess.count("#EffectiveVaultAccess.countEVUs");
	}

	public static long countEffectiveVaultUsersOfGroup(String groupId) {
		return EffectiveVaultAccess.count("#EffectiveVaultAccess.countEVUsInGroup", Parameters.with("groupId", groupId));
	}

}

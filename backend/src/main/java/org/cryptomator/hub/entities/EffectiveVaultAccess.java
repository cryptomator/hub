package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "effective_vault_access")
@NamedQuery(name = "EffectiveVaultAccess.countSeatsOccupiedByUser", query = """
		SELECT count(eva)
		FROM EffectiveVaultAccess eva
		INNER JOIN Vault v ON eva.id.vaultId = v.id AND NOT v.archived
		WHERE eva.id.authorityId = :userId
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatOccupyingUsers", query = """
		SELECT count(DISTINCT u)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id AND NOT v.archived
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatOccupyingUsersOfGroup", query = """
		SELECT count(DISTINCT u)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN EffectiveGroupMembership egm ON u.id = egm.id.memberId
		INNER JOIN Vault v ON eva.id.vaultId = v.id AND NOT v.archived
		WHERE egm.id.groupId = :groupId
		""")
@RegisterForReflection(targets = {UUID[].class})
public class EffectiveVaultAccess extends PanacheEntityBase {

	@EmbeddedId
	public VaultAccess.Id id;

	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	public VaultAccess.Role role;

	public static boolean isUserOccupyingSeat(String userId) {
		return EffectiveVaultAccess.count("#EffectiveVaultAccess.countSeatsOccupiedByUser", Parameters.with("userId", userId)) > 0;
	}

	public static long countSeatOccupyingUsers() {
		return EffectiveVaultAccess.count("#EffectiveVaultAccess.countSeatOccupyingUsers");
	}

	public static long countSeatOccupyingUsersOfGroup(String groupId) {
		return EffectiveVaultAccess.count("#EffectiveVaultAccess.countSeatOccupyingUsersOfGroup", Parameters.with("groupId", groupId));
	}

}

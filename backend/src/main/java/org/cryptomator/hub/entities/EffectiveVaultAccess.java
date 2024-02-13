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
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Immutable
@Table(name = "effective_vault_access")
@NamedQuery(name = "EffectiveVaultAccess.countSeatsOccupiedByUser", query = """
		SELECT count(eva)
		FROM EffectiveVaultAccess eva
		INNER JOIN Vault v ON eva.id.vaultId = v.id AND NOT v.archived
		WHERE eva.id.authorityId = :userId
		""")
@NamedQuery(name = "EffectiveVaultAccess.getSeatOccupyingUserIds", query = """
		SELECT DISTINCT u.id
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id AND NOT v.archived
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatOccupyingUsers", query = """
		SELECT count(DISTINCT u)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id AND NOT v.archived
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatOccupyingUsersWithAccessToken", query = """
		SELECT count(DISTINCT u)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id AND NOT v.archived
		INNER JOIN AccessToken at ON eva.id.vaultId = at.id.vaultId AND eva.id.authorityId = at.id.userId
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatOccupyingUsersOfGroup", query = """
		SELECT count(DISTINCT u)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN EffectiveGroupMembership egm ON u.id = egm.id.memberId
		INNER JOIN Vault v ON eva.id.vaultId = v.id AND NOT v.archived
		WHERE egm.id.groupId = :groupId
		""")
@NamedQuery(name = "EffectiveVaultAccess.findByUserAndVault", query = """
		SELECT eva
		FROM EffectiveVaultAccess eva
		WHERE eva.id.vaultId = :vaultId AND eva.id.authorityId = :authorityId
		""")
public class EffectiveVaultAccess extends PanacheEntityBase {

	@EmbeddedId
	public EffectiveVaultAccess.Id id;

	public static boolean isUserOccupyingSeat(String userId) {
		return EffectiveVaultAccess.count("#EffectiveVaultAccess.countSeatsOccupiedByUser", Parameters.with("userId", userId)) > 0;
	}

	public static Stream<String> getSeatOccupyingUserIds() {
		return getEntityManager().createNamedQuery("EffectiveVaultAccess.getSeatOccupyingUserIds", String.class).getResultStream();
	}

	public static long countSeatOccupyingUsers() {
		return EffectiveVaultAccess.count("#EffectiveVaultAccess.countSeatOccupyingUsers");
	}

	public static long countSeatOccupyingUsersWithAccessToken() {
		return EffectiveVaultAccess.count("#EffectiveVaultAccess.countSeatOccupyingUsersWithAccessToken");
	}

	public static long countSeatOccupyingUsersOfGroup(String groupId) {
		return EffectiveVaultAccess.count("#EffectiveVaultAccess.countSeatOccupyingUsersOfGroup", Parameters.with("groupId", groupId));
	}

	public static Collection<VaultAccess.Role> listRoles(UUID vaultId, String authorityId) {
		return EffectiveVaultAccess.<EffectiveVaultAccess>find("#EffectiveVaultAccess.findByUserAndVault", Parameters.with("vaultId", vaultId).and("authorityId", authorityId)).stream()
				.map(eva -> eva.id.role)
				.collect(Collectors.toUnmodifiableSet());
	}

	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "vault_id")
		public UUID vaultId;

		@Column(name = "authority_id")
		public String authorityId;

		@Column(name = "role", nullable = false)
		@Enumerated(EnumType.STRING)
		public VaultAccess.Role role;

		public Id(UUID vaultId, String authorityId, VaultAccess.Role role) {
			this.vaultId = vaultId;
			this.authorityId = authorityId;
			this.role = role;
		}

		public Id() {
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o instanceof EffectiveVaultAccess.Id other) {
				return Objects.equals(this.vaultId, other.vaultId) && Objects.equals(this.authorityId, other.authorityId) && Objects.equals(this.role, other.role);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(vaultId, authorityId, role);
		}

		@Override
		public String toString() {
			return "EffectiveVaultAccess.Id{" +
					"vaultId='" + vaultId + '\'' +
					", authorityId='" + authorityId + '\'' +
					", role='" + role + '\'' +
					'}';
		}
	}

}

package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
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
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Immutable
@Table(name = "effective_vault_access")
@NamedQuery(name = "EffectiveVaultAccess.countSeatsOccupiedBySingleUser", query = """
		SELECT count(u)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		WHERE eva.id.authorityId = :userId
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatsOccupiedByUsers", query = """
		SELECT COUNT(DISTINCT u.id)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id AND NOT v.archived
		WHERE u.id IN :userIds
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
@NamedQuery(name = "EffectiveVaultAccess.findByAuthorityAndVault", query = """
		SELECT eva
		FROM EffectiveVaultAccess eva
		WHERE eva.id.vaultId = :vaultId AND eva.id.authorityId = :authorityId
		""")
public class EffectiveVaultAccess {

	@EmbeddedId
	private EffectiveVaultAccess.Id id;

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "vault_id")
		private UUID vaultId;

		@Column(name = "authority_id")
		private String authorityId;

		@Column(name = "role", nullable = false)
		@Enumerated(EnumType.STRING)
		private VaultAccess.Role role;

		public UUID getVaultId() {
			return vaultId;
		}

		public void setVaultId(UUID vaultId) {
			this.vaultId = vaultId;
		}

		public String getAuthorityId() {
			return authorityId;
		}

		public void setAuthorityId(String authorityId) {
			this.authorityId = authorityId;
		}

		public VaultAccess.Role getRole() {
			return role;
		}

		public void setRole(VaultAccess.Role role) {
			this.role = role;
		}

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

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<EffectiveVaultAccess, Id> {

		public boolean isUserOccupyingSeat(String userId) {
			return count("#EffectiveVaultAccess.countSeatsOccupiedBySingleUser", Parameters.with("userId", userId)) > 0;
		}

		public long countSeatsOccupiedByUsers(List<String> userIds) {
			return count("#EffectiveVaultAccess.countSeatsOccupiedByUsers", Parameters.with("userIds", userIds));
		}

		public long countSeatOccupyingUsers() {
			return count("#EffectiveVaultAccess.countSeatOccupyingUsers");
		}

		public long countSeatOccupyingUsersWithAccessToken() {
			return count("#EffectiveVaultAccess.countSeatOccupyingUsersWithAccessToken");
		}

		public long countSeatOccupyingUsersOfGroup(String groupId) {
			return count("#EffectiveVaultAccess.countSeatOccupyingUsersOfGroup", Parameters.with("groupId", groupId));
		}

		public Collection<VaultAccess.Role> listRoles(UUID vaultId, String authorityId) {
			return find("#EffectiveVaultAccess.findByAuthorityAndVault", Parameters.with("vaultId", vaultId).and("authorityId", authorityId)).stream()
					.map(eva -> eva.getId().getRole())
					.collect(Collectors.toUnmodifiableSet());
		}
	}
}

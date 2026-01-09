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
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Immutable
@Table(name = "effective_vault_access")
@NamedQuery(name = "EffectiveVaultAccess.isUserOccupyingSeat", query = """
		SELECT 1
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id
		WHERE u.id = :userId AND NOT v.archived
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatsOccupiedByUsers", query = """
		SELECT COUNT(DISTINCT u.id)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id
		WHERE u.id IN :userIds AND NOT v.archived
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatOccupyingUsers", query = """
		SELECT COUNT(DISTINCT u.id)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id
		WHERE NOT v.archived
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatOccupyingUsersWithAccessToken", query = """
		SELECT COUNT(DISTINCT u.id)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id
		INNER JOIN AccessToken at ON eva.id.vaultId = at.id.vaultId AND eva.id.authorityId = at.id.userId
		WHERE NOT v.archived
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatOccupyingUsersOfGroup", query = """
		SELECT COUNT(DISTINCT u.id)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN EffectiveGroupMembership egm ON u.id = egm.id.memberId
		INNER JOIN Vault v ON eva.id.vaultId = v.id
		WHERE egm.id.groupId = :groupId AND NOT v.archived
		""")
@NamedQuery(name = "EffectiveVaultAccess.findByAuthorityAndVault", query = """
		SELECT eva
		FROM EffectiveVaultAccess eva
		WHERE eva.id.vaultId = :vaultId AND eva.id.authorityId = :authorityId
		""")
@NamedQuery(name = "EffectiveVaultAccess.findMembersWithoutAccessTokens", query = """
		SELECT eva
		FROM EffectiveVaultAccess eva
			INNER JOIN FETCH eva.authority u
			LEFT JOIN AccessToken token ON token.id.vaultId = eva.id.vaultId AND token.id.userId = u.id
			WHERE eva.id.vaultId = :vaultId AND token.vault IS NULL AND u.ecdhPublicKey IS NOT NULL
		"""
)
public class EffectiveVaultAccess {

	@EmbeddedId
	private EffectiveVaultAccess.Id id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("vaultId")
	@JoinColumn(name = "vault_id")
	private Vault vault;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("authorityId")
	@JoinColumn(name = "authority_id")
	private Authority authority;

	public Id getId() {
		return id;
	}

	public Vault getVault() {
		return vault;
	}

	public Authority getAuthority() {
		return authority;
	}

	public VaultAccess.Role getRole() {
		return id.role;
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
			return find("#EffectiveVaultAccess.isUserOccupyingSeat", Parameters.with("userId", userId)).page(0, 1).firstResult() != null;
		}

		public long countSeatsOccupiedByUsers(Collection<String> userIds) {
			return Batch.of(200).run(Set.copyOf(userIds), 0L, (batch, result) -> {
				long partialCount = count("#EffectiveVaultAccess.countSeatsOccupiedByUsers", Parameters.with("userIds", batch));
				return result + partialCount;
			});
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
					.map(EffectiveVaultAccess::getRole)
					.collect(Collectors.toUnmodifiableSet());
		}

		public Stream<EffectiveVaultAccess> findMembersWithoutAccessTokens(UUID vaultId) {
			return find("#EffectiveVaultAccess.findMembersWithoutAccessTokens", Parameters.with("vaultId", vaultId)).stream();
		}
	}
}

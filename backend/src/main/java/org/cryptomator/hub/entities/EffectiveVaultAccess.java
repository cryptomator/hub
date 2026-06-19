package org.cryptomator.hub.entities;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
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

import java.util.Collection;
import java.util.Map;
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
		WHERE u.id = :userId AND NOT v.archived AND u.enabled
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatsOccupiedByUsers", query = """
		SELECT COUNT(DISTINCT u.id)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id
		WHERE u.id IN :userIds AND NOT v.archived AND u.enabled
		""")
@NamedQuery(name = "EffectiveVaultAccess.usersSeatedOnOtherVaults", query = """
		SELECT DISTINCT u.id
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id
		WHERE NOT v.archived AND v.id <> :vaultId AND u.enabled
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatOccupyingUsers", query = """
		SELECT COUNT(DISTINCT u.id)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id
		WHERE NOT v.archived AND u.enabled
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatOccupyingUsersWithAccessToken", query = """
		SELECT COUNT(DISTINCT u.id)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN Vault v ON eva.id.vaultId = v.id
		INNER JOIN AccessToken at ON eva.id.vaultId = at.id.vaultId AND eva.id.authorityId = at.id.userId
		WHERE NOT v.archived AND u.enabled
		""")
@NamedQuery(name = "EffectiveVaultAccess.countSeatOccupyingUsersOfGroup", query = """
		SELECT COUNT(DISTINCT u.id)
		FROM User u
		INNER JOIN EffectiveVaultAccess eva ON u.id = eva.id.authorityId
		INNER JOIN EffectiveGroupMembership egm ON u.id = egm.id.memberId
		INNER JOIN Vault v ON eva.id.vaultId = v.id
		WHERE egm.id.groupId = :groupId AND NOT v.archived AND u.enabled
		""")
@NamedQuery(name = "EffectiveVaultAccess.findByAuthorityAndVault", query = """
		SELECT eva
		FROM EffectiveVaultAccess eva
		WHERE eva.id.vaultId = :vaultId AND eva.id.authorityId = :authorityId
		""")
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
		return id.role();
	}

	@Embeddable
	public record Id(
			@Column(name = "vault_id") UUID vaultId,
			@Column(name = "authority_id") String authorityId,
			@Column(name = "role", nullable = false) @Enumerated(EnumType.STRING) VaultAccess.Role role) {
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<EffectiveVaultAccess, Id> {

		public boolean isUserOccupyingSeat(String userId) {
			return find("#EffectiveVaultAccess.isUserOccupyingSeat", Map.of("userId", userId)).page(0, 1).firstResult() != null;
		}

		public long countSeatsOccupiedByUsers(Collection<String> userIds) {
			return Batch.of(200).run(Set.copyOf(userIds), 0L, (batch, result) -> {
				long partialCount = count("#EffectiveVaultAccess.countSeatsOccupiedByUsers", Map.of("userIds", batch));
				return result + partialCount;
			});
		}

		public long countSeatOccupyingUsers() {
			return count("#EffectiveVaultAccess.countSeatOccupyingUsers");
		}

		@WithSpan("EffectiveVaultAccess.Repository.countSeatOccupyingUsersWithAccessToken")
		public long countSeatOccupyingUsersWithAccessToken() {
			return count("#EffectiveVaultAccess.countSeatOccupyingUsersWithAccessToken");
		}

		public long countSeatOccupyingUsersOfGroup(String groupId) {
			return count("#EffectiveVaultAccess.countSeatOccupyingUsersOfGroup", Map.of("groupId", groupId));
		}

		public Collection<VaultAccess.Role> listRoles(UUID vaultId, String authorityId) {
			return find("#EffectiveVaultAccess.findByAuthorityAndVault", Map.of("vaultId", vaultId, "authorityId", authorityId)).stream()
					.map(eva -> eva.getId().role())
					.collect(Collectors.toUnmodifiableSet());
		}

		public Stream<String> usersSeatedOnOtherVaults(UUID vaultId) {
			return find("#EffectiveVaultAccess.usersSeatedOnOtherVaults", Map.of("vaultId", vaultId)).project(String.class).stream();
		}
	}
}

package org.cryptomator.hub.entities;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.runtime.annotations.RegisterForReflection;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BinaryOperator;
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
@NamedQuery(name = "EffectiveVaultAccess.findUsersWithoutAccessTokens", query = """
		SELECT u, eva.id.role
		FROM User u
			INNER JOIN EffectiveVaultAccess eva ON eva.id.authorityId = u.id
			LEFT JOIN AccessToken token ON token.id.vaultId = eva.id.vaultId AND token.id.userId = u.id
			WHERE eva.id.vaultId = :vaultId AND token.vault IS NULL AND u.ecdhPublicKey IS NOT NULL AND u.enabled
		""",
		resultClass = EffectiveVaultAccess.UserWithRole.class
)
@NamedQuery(name = "EffectiveVaultAccess.findMembersWithoutAccessTokensForAccessibleVaults", query = """
		SELECT DISTINCT eva
		FROM EffectiveVaultAccess eva
			INNER JOIN eva.authority u
			INNER JOIN Vault v ON v.id = eva.id.vaultId
			INNER JOIN AccessToken callerToken ON callerToken.id.vaultId = eva.id.vaultId AND callerToken.id.userId = :currentUser
			LEFT JOIN AccessToken token ON token.id.vaultId = eva.id.vaultId AND token.id.userId = u.id
			WHERE v.archived = false AND token.vault IS NULL AND u.ecdhPublicKey IS NOT NULL AND u.enabled
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
		return id.role();
	}

	/**
	 * Result row of {@code EffectiveVaultAccess.findUsersWithoutAccessTokens}: a user and one of their effective roles on the vault.
	 */
	@RegisterForReflection
	public record UserWithRole(User user, VaultAccess.Role role) {
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

		/**
		 * Finds enabled users with a key pair who have effective access to the given vault but no access token for it yet.
		 *
		 * @param vaultId ID of a vault
		 * @return pending users mapped to their highest effective role on the vault (e.g. OWNER for a user that is MEMBER directly but OWNER via a group)
		 * @see #findMembersWithoutAccessTokens(String)
		 */
		public Map<User, VaultAccess.Role> findUsersWithoutAccessTokensForVault(UUID vaultId) {
			return getEntityManager().createNamedQuery("EffectiveVaultAccess.findUsersWithoutAccessTokens", UserWithRole.class)
					.setParameter("vaultId", vaultId)
					.getResultStream()
					.collect(Collectors.toMap(UserWithRole::user, UserWithRole::role, BinaryOperator.maxBy(Comparator.naturalOrder()), LinkedHashMap::new)); // maxBy naturalOrder -> report "highest" role
		}

		/**
		 * Finds the pending access grants the given user could perform, grouped by vault id. Limited to vaults the user
		 * holds an access token for (i.e. can decrypt and therefore re-share). The Web-of-Trust decision and the vault's
		 * own (encrypted, tamper-proof) trust threshold / enabled flag are evaluated client-side — deliberately not here:
		 * the {@code effective_wot} view is a recursive transitive-closure computation that would be too costly to join
		 * on this long-poll hot path, and the client already avoids re-evaluating ruled-out candidates via its blocklists.
		 *
		 * @param currentUserId ID of the currently logged-in user
		 * @return ids of vault members who have no access token yet, on vaults the user can decrypt, grouped by vault id
		 * @see #findMembersWithoutAccessTokensForVault(UUID)
		 */
		public Map<UUID, Set<String>> findMembersWithoutAccessTokens(String currentUserId) {
			return find("#EffectiveVaultAccess.findMembersWithoutAccessTokensForAccessibleVaults", Map.of("currentUser", currentUserId))
					.stream()
					.collect(Collectors.groupingBy(
							eva -> eva.getId().vaultId(),
							Collectors.mapping(eva -> eva.getId().authorityId(), Collectors.toSet())));
		}

		public Stream<String> usersSeatedOnOtherVaults(UUID vaultId) {
			return find("#EffectiveVaultAccess.usersSeatedOnOtherVaults", Map.of("vaultId", vaultId)).project(String.class).stream();
		}
	}
}

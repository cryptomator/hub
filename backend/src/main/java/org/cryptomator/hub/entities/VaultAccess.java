package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Entity
@Table(name = "vault_access")
@NamedQuery(name = "VaultAccess.forVault",
		query = """
				SELECT va
				FROM VaultAccess va
				INNER JOIN FETCH va.vault
				INNER JOIN FETCH va.authority
				WHERE va.id.vaultId = :vaultId
				""")
@NamedQuery(name = "VaultAccess.countByAuthority",
		query = """
				SELECT COUNT(va)
				FROM VaultAccess va
				WHERE va.id.authorityId = :authorityId
				""")
@NamedQuery(name = "VaultAccess.findByAuthority",
		query = """
				SELECT va
				FROM VaultAccess va
				INNER JOIN FETCH va.vault
				WHERE va.id.authorityId = :authorityId
				""")
@NamedQuery(name = "VaultAccess.deleteSpecific",
		query = """
				DELETE FROM VaultAccess va
				WHERE va.id.vaultId = :vaultId
				AND va.id.authorityId IN :authorityIds
				""")
public class VaultAccess {

	@EmbeddedId
	private VaultAccess.Id id;

	@ManyToOne
	@MapsId("vaultId")
	@JoinColumn(name = "vault_id")
	private Vault vault;

	@ManyToOne
	@MapsId("authorityId")
	@JoinColumn(name = "authority_id")
	private Authority authority;

	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	private Role role;

	public enum Role {
		/**
		 * User with access to vault contents.
		 */
		MEMBER,

		/**
		 * User with administrative privileges on a vault.
		 */
		OWNER
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public Vault getVault() {
		return vault;
	}

	public void setVault(Vault vault) {
		this.vault = vault;
	}

	public Authority getAuthority() {
		return authority;
	}

	public void setAuthority(Authority authority) {
		this.authority = authority;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public static VaultAccess create(Vault vault, Authority authority, Role role) {
		VaultAccess entity = new VaultAccess();
		entity.setId(new Id(vault.getId(), authority.getId()));
		entity.setVault(vault);
		entity.setAuthority(authority);
		entity.setRole(role);
		return entity;
	}

	@Embeddable
	public record Id(@Column(name = "vault_id") UUID vaultId, @Column(name = "authority_id") String authorityId) {
	}

	@ApplicationScoped
	public static class Repository implements PanacheRepositoryBase<VaultAccess, Id> {

		public Stream<VaultAccess> forVault(UUID vaultId) {
			return find("#VaultAccess.forVault", Map.of("vaultId", vaultId)).stream();
		}

		public long countByAuthority(String authorityId) {
			return count("#VaultAccess.countByAuthority", Map.of("authorityId", authorityId));
		}

		public Stream<VaultAccess> findByAuthority(String authorityId) {
			return find("#VaultAccess.findByAuthority", Map.of("authorityId", authorityId)).stream();
		}

		public long delete(UUID vaultId, Iterable<String> authorityIds) {
			return delete("#VaultAccess.deleteSpecific", Map.of("vaultId", vaultId, "authorityIds", authorityIds));
		}
	}
}

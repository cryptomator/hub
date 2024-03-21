package org.cryptomator.hub.entities;

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

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

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
public class VaultAccess {

	@EmbeddedId
	VaultAccess.Id id = new VaultAccess.Id();

	@ManyToOne
	@MapsId("vaultId")
	@JoinColumn(name = "vault_id")
	Vault vault;

	@ManyToOne
	@MapsId("authorityId")
	@JoinColumn(name = "authority_id")
	Authority authority;

	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	Role role;

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

	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "vault_id")
		UUID vaultId;

		@Column(name = "authority_id")
		String authorityId;

		public Id(UUID vaultId, String authorityId) {
			this.vaultId = vaultId;
			this.authorityId = authorityId;
		}

		public Id() {
		}

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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o instanceof Id other) {
				return Objects.equals(this.vaultId, other.vaultId) && Objects.equals(this.authorityId, other.authorityId);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(vaultId, authorityId);
		}

		@Override
		public String toString() {
			return "VaultAccess.Id{" +
					"vaultId='" + vaultId + '\'' +
					", authorityId='" + authorityId + '\'' +
					'}';
		}
	}
}

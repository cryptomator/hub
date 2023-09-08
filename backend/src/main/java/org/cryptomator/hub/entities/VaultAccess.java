package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;
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
public class VaultAccess extends PanacheEntityBase {

	@EmbeddedId
	public VaultAccess.Id id = new VaultAccess.Id();

	@ManyToOne
	@MapsId("vaultId")
	@JoinColumn(name = "vault_id")
	public Vault vault;

	@ManyToOne
	@MapsId("authorityId")
	@JoinColumn(name = "authority_id")
	public Authority authority;

	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	public Role role;

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

	public static Stream<VaultAccess> forVault(UUID vaultId) {
		return find("#VaultAccess.forVault", Parameters.with("vaultId", vaultId)).stream();
	}

	@Embeddable
	public static class Id implements Serializable {

		@Column(name = "vault_id")
		public UUID vaultId;

		@Column(name = "authority_id")
		public String authorityId;

		public Id(UUID vaultId, String authorityId) {
			this.vaultId = vaultId;
			this.authorityId = authorityId;
		}

		public Id() {
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

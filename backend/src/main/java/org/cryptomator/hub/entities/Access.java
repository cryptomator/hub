package org.cryptomator.hub.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.panache.common.Parameters;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "access")
// FIXME simplify request, some LEFT JOIN can be replaced with JOIN
@NamedQuery(name = "Access.get", query = """
			SELECT a
			FROM Access a
			LEFT JOIN a.vault.members vu
			LEFT JOIN vu.groups gu
			LEFT JOIN a.vault.groups vg
			WHERE a.device.id = :deviceId
				AND a.id.userId = :userId
				AND a.id.vaultId = :vaultId
				AND NOT (vu.id IS NULL AND vg.id is NULL)
		""")
@NamedQuery(name = "Access.revokeDeviceAccessIfNoAccessViaGroupsGranted", query = """
			DELETE
			FROM Access a
			WHERE a IN (SELECT qa FROM Access qa
				LEFT JOIN qa.vault.members vu
				LEFT JOIN vu.groups gu
				LEFT JOIN qa.vault.groups vg
				WHERE a.id.userId = :userId
					AND qa.id.vaultId = :vaultId
					AND vu.id IS NULL AND vg.id is NULL)
		""")
@NamedQuery(name = "Access.revokeDeviceAccessIfNoAccessViaUserGranted", query = """
			DELETE
			FROM Access a
			WHERE a IN (SELECT qa FROM Access qa
				LEFT JOIN qa.vault.members vu
				LEFT JOIN vu.groups gu
				LEFT JOIN qa.vault.groups vg
					WHERE gu.id = :groupId
					AND qa.id.vaultId = :vaultId
					AND vu.id IS NULL AND vg.id is NULL)
		""")
@NamedQuery(name = "Access.revokeDeviceAccessForGroupsIfNoAccessViaUserGranted", query = """
			DELETE
			FROM Access a
			WHERE a IN (SELECT qa FROM Access qa
				LEFT JOIN qa.vault.members vu
				LEFT JOIN vu.groups gu
				LEFT JOIN qa.vault.groups vg
					WHERE vu.id is NULL AND vg.id IN :groupIds)
		""")

@NamedQuery(name = "Access.revokeDeviceAccessForUsersIfNoAccessViaGroupsGranted", query = """
			DELETE
			FROM Access a
			WHERE a IN (SELECT qa FROM Access qa
				LEFT JOIN qa.vault.members vu
				LEFT JOIN vu.groups gu
				LEFT JOIN qa.vault.groups vg
					WHERE vu.id is NULL AND vg.id iS NULL AND qa.id.userId IN :userIds)
		""")
public class Access extends PanacheEntityBase {

	@EmbeddedId
	public AccessId id = new AccessId();

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("deviceId")
	@JoinColumn(name = "device_id")
	public Device device;

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	public User user;

	@ManyToOne(optional = false, cascade = {CascadeType.REMOVE})
	@MapsId("vaultId")
	@JoinColumn(name = "vault_id")
	public Vault vault;

	@Column(name = "jwe", nullable = false)
	public String jwe;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Access access = (Access) o;
		return Objects.equals(id, access.id)
				&& Objects.equals(device, access.device)
				&& Objects.equals(user, access.user)
				&& Objects.equals(vault, access.vault)
				&& Objects.equals(jwe, access.jwe);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, device, user, vault, jwe);
	}

	@Override
	public String toString() {
		return "Access{" +
				"id=" + id +
				", device=" + device.id +
				", user=" + user.id +
				", vault=" + vault.id +
				", jwe='" + jwe + '\'' +
				'}';
	}

	@Embeddable
	public static class AccessId implements Serializable {

		@Column(name = "device_id", nullable = false)
		private String deviceId;

		@Column(name = "user_id", nullable = false)
		private String userId;

		@Column(name = "vault_id", nullable = false)
		private String vaultId;

		public AccessId(String deviceId, String userId, String vaultId) {
			this.deviceId = deviceId;
			this.userId = userId;
			this.vaultId = vaultId;
		}

		public AccessId() {
		}

		public String getDeviceId() {
			return deviceId;
		}

		public String getVaultId() {
			return vaultId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AccessId other = (AccessId) o;
			return Objects.equals(deviceId, other.deviceId) //
					&& Objects.equals(userId, other.userId) //
					&& Objects.equals(vaultId, other.vaultId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(deviceId, userId, vaultId);
		}
	}

	// --- data layer queries ---

	public static Access unlock(String vaultId, String deviceId, String userId) {
		try {
			return find("#Access.get", Parameters.with("deviceId", deviceId).and("vaultId", vaultId).and("userId", userId)).firstResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public static void revokeDeviceAccessIfNoAccessViaGroupsGranted(String vaultId, String userId) {
		//TODO Replace with PanacheEntityBase.delete(...) once https://github.com/quarkusio/quarkus/issues/20758 is fixed
		int affected = getEntityManager().createNamedQuery("Access.revokeDeviceAccessIfNoAccessViaGroupsGranted").setParameter("vaultId", vaultId).setParameter("userId", userId).executeUpdate();
		// TODO think about what to do with that
		/*if (affected == 0) {
			throw new EntityNotFoundException("Access(vault: " + vaultId + ", device: " + userId + ") not found");
		}*/
	}

	public static void revokeDeviceAccessIfNoAccessViaUserGranted(String vaultId, String groupId) {
		//TODO Replace with PanacheEntityBase.delete(...) once https://github.com/quarkusio/quarkus/issues/20758 is fixed
		int affected = getEntityManager().createNamedQuery("Access.revokeDeviceAccessIfNoAccessViaUserGranted").setParameter("vaultId", vaultId).setParameter("groupId", groupId).executeUpdate();
		// TODO think about what to do with that
		/*if (affected == 0) {
			throw new EntityNotFoundException("Access(vault: " + vaultId + ", device: " + groupId + ") not found");
		}*/
	}

	public static void revokeDeviceAccessForGroupsIfNoAccessViaUserGranted(Set<String> groupIds) {
		//TODO Replace with PanacheEntityBase.delete(...) once https://github.com/quarkusio/quarkus/issues/20758 is fixed
		int affected = getEntityManager().createNamedQuery("Access.revokeDeviceAccessForGroupsIfNoAccessViaUserGranted").setParameter("groupIds", groupIds).executeUpdate();
		// TODO think about what to do with that
		/*if (affected == 0) {
			throw new EntityNotFoundException("Access(vault: " + vaultId + ", device: " + groupId + ") not found");
		}*/
	}

	public static void revokeDeviceAccessForUsersIfNoAccessViaGroupsGranted(Set<String> userIds) {
		//TODO Replace with PanacheEntityBase.delete(...) once https://github.com/quarkusio/quarkus/issues/20758 is fixed
		int affected = getEntityManager().createNamedQuery("Access.revokeDeviceAccessForUsersIfNoAccessViaGroupsGranted").setParameter("userIds", userIds).executeUpdate();
		// TODO think about what to do with that
		/*if (affected == 0) {
			throw new EntityNotFoundException("Access(vault: " + vaultId + ", device: " + groupId + ") not found");
		}*/
	}
}

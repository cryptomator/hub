package org.cryptomator.hub.persistence.entities;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;

@ApplicationScoped
@Transactional
public class AccessDao {

	@PersistenceContext()
	EntityManager em;

	@Inject
	BasicDao basicDao;

	public Access unlock(String vaultId, String deviceId, String userId) {
		try {
			return em.createNamedQuery("Access.get", Access.class)
					.setParameter("deviceId", deviceId)
					.setParameter("vaultId", vaultId)
					.setParameter("userId", userId)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public Access.AccessId persist(Access access) {
		try {
			return basicDao.persist(access).getId();
		} catch (PersistenceException e) {
			//ConstraintViolation.throwIfConstraintViolated(e);
			throw e;
		}
	}

	public void deleteDeviceAccess(String vaultId, String deviceId) {
		int affected = em.createNamedQuery("Access.revokeDevice")
				.setParameter("vaultId", vaultId)
				.setParameter("deviceId", deviceId)
				.executeUpdate();
		if (affected == 0) {
			throw new EntityNotFoundException("Access(vault: " + vaultId + ", device: " + deviceId + ") not found");
		}
	}

	public void deleteUserAccess(String vaultId, String userId) {
		int affected = em.createNamedQuery("Access.revokeUser")
				.setParameter("vaultId", vaultId)
				.setParameter("userId", userId)
				.executeUpdate();
		if (affected == 0) {
			throw new EntityNotFoundException("Access(vault: " + vaultId + ", user: " + userId + ") not found");
		}
	}

}

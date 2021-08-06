package org.cryptomator.hub.persistence.entities;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
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

	public Access get(String vaultId, String deviceId) {
		return basicDao.get(Access.class, new Access.AccessId(deviceId, vaultId));
	}

	public Access.AccessId persist(Access access) {
		try {
			return basicDao.persist(access).getId();
		} catch (PersistenceException e) {
			//ConstraintViolation.throwIfConstraintViolated(e);
			throw e;
		}
	}

}

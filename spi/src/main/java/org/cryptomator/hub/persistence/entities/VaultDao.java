package org.cryptomator.hub.persistence.entities;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
@Transactional
public class VaultDao {

    @PersistenceContext()
    EntityManager em;

    @Inject
    BasicDao basicDao;

    public List<Vault> getAll() {
        return basicDao.getAll(Vault.class);
    }

    public Vault get(String id) {
        return basicDao.get(Vault.class, id);
    }

    public String persist(Vault vault) {
        try {
            return basicDao.persist(vault).getId();
        } catch (PersistenceException e) {
            //ConstraintViolation.throwIfConstraintViolated(e);
            throw e;
        }
    }
}

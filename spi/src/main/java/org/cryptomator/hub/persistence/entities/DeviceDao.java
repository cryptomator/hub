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
public class DeviceDao {

    @PersistenceContext()
    EntityManager em;

    @Inject
    BasicDao basicDao;

    public List<Device> getAll() {
        return basicDao.getAll(Device.class);
    }

    public Device get(String id) {
        return basicDao.get(Device.class, id);
    }

    public String persist(Device device) {
        try {
            return basicDao.persist(device).getId();
        } catch (PersistenceException e) {
            //ConstraintViolation.throwIfConstraintViolated(e);
            throw e;
        }
    }

    public void delete(String id) {
        basicDao.delete(Device.class, id);
    }

}

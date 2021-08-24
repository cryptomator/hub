package org.cryptomator.hub.persistence.entities;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
class BasicDao {

	@PersistenceContext()
	EntityManager em;

	public <T> List<T> getAll(Class<T> clazz) {
		CriteriaQuery<T> query = em.getCriteriaBuilder().createQuery(clazz);
		Root<T> rootEntry = query.from(clazz);
		CriteriaQuery<T> result = query.select(rootEntry);
		return em.createQuery(result).getResultList();
	}

	public <T> T get(Class<T> clazz, int id) {
		return em.find(clazz, id);
	}

	public <T> T get(Class<T> clazz, String id) {
		return em.find(clazz, id);
	}

	public <T> T get(Class<T> clazz, Object primaryKey) {
		return em.find(clazz, primaryKey);
	}

	public <T> T persist(T entity) {
		em.persist(entity);
		em.flush();
		return entity;
	}

	public <T> void delete(Class<T> clazz, int id) {
		T entity = get(clazz, id);
		em.remove(entity);
	}

	public <T> void delete(Class<T> clazz, String id) {
		T entity = get(clazz, id);
		em.remove(entity);
	}

	public <T> List<T> getByIds(Class<T> clazz, Collection<Integer> ids) {
		if (ids.isEmpty()) {
			return Collections.emptyList();
		} else {
			CriteriaQuery<T> query = em.getCriteriaBuilder().createQuery(clazz);
			Root<T> rootEntry = query.from(clazz);
			CriteriaQuery<T> result = query.select(rootEntry).where(rootEntry.get("id").in(ids));
			return em.createQuery(result).getResultList();
		}
	}

}
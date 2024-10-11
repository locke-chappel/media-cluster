package io.github.lc.oss.mc.scheduler;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.mc.scheduler.app.entity.AbstractBaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

@Transactional(propagation = Propagation.REQUIRES_NEW)
@Rollback(false)
public abstract class AbstractItHelper {
    @Autowired
    private Factory factory;

    @PersistenceContext
    private EntityManager entityManager;

    public void delete(AbstractBaseEntity entity) {
        this.em().remove(this.find(entity.getClass(), entity.getId()));
    }

    public String getDefaultAppName() {
        return this.fac().getDefaultAppName();
    }

    public String getDefaultUsername() {
        return this.fac().getDefaultUsername();
    }

    protected <T extends AbstractBaseEntity> T assrt(Class<T> clazz, String key, Map<String, String> ids) {
        T e = this.find(clazz, key, ids);
        Assertions.assertNotNull(e);
        return e;
    }

    protected <T extends AbstractBaseEntity> T assrt(Class<T> clazz, String id) {
        T e = this.find(clazz, id);
        Assertions.assertNotNull(e);
        return e;
    }

    protected <T extends AbstractBaseEntity> T find(Class<T> clazz, String key, Map<String, String> ids) {
        String id = ids.get(key);
        Assertions.assertNotNull(id);
        return this.find(clazz, id);
    }

    protected <T extends AbstractBaseEntity> T find(Class<T> clazz, String id) {
        return this.em().find(clazz, id);
    }

    protected <T extends AbstractBaseEntity> T find(Class<T> clazz, String attribute, String value) {
        CriteriaBuilder cb = this.em().getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(clazz);
        Root<T> root = query.from(clazz);
        query.where(cb.equal(root.get(attribute), value));
        TypedQuery<T> typedQuery = this.em().createQuery(query);
        return typedQuery.getSingleResult();
    }

    protected EntityManager em() {
        return this.entityManager;
    }

    protected Factory fac() {
        return this.factory;
    }
}

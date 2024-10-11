package io.github.lc.oss.mc.scheduler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

public class SqlHelper {
    private static final List<String> SQL = Collections.unmodifiableList(Arrays.asList( //
            "set REFERENTIAL_INTEGRITY false", //
            "truncate table JOB", //
            "truncate table JOB_HISTORY", //
            "truncate table NODE", //
            "truncate table PROFILE", //
            "truncate table USERS", //
            "truncate table USER_HASH", //
            "set REFERENTIAL_INTEGRITY true" //
    ));

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearDatabase() {
        for (String sql : SqlHelper.SQL) {
            Query q = this.entityManager.createNativeQuery(sql);
            q.executeUpdate();
        }
    }
}

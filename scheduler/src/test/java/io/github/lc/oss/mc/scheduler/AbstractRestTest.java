package io.github.lc.oss.mc.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.testing.web.JsonObject;
import io.github.lc.oss.mc.scheduler.app.entity.AbstractBaseEntity;
import io.github.lc.oss.mc.scheduler.app.entity.AbstractEntity;
import io.github.lc.oss.mc.scheduler.app.model.Credentials;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

@SpringBootTest(classes = { IntegrationConfig.class })
@Rollback
@Tag("restTest")
@ActiveProfiles("integrationtest")
public abstract class AbstractRestTest extends io.github.lc.oss.commons.testing.web.AbstractRestTest {
    private static final Pattern COOKIE_MAX_AGE_ZERO = Pattern.compile("Max-Age=[^0]");

    @Autowired
    private Factory factory;
    @Autowired
    private SqlHelper sqlHelper;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${server.servlet.session.cookie.name:_Host-Identity}")
    private String cookieName;

    @BeforeEach
    public void dbCleanup() {
        this.sqlHelper.clearDatabase();
        this.getFactory().initDatabase();
    }

    protected void assertJsonMessage(JsonObject object, Message message, String text) {
        this.assertJsonMessage(object, message.getCategory().name(), message.getSeverity().name(), message.getNumber(),
                text);
    }

    protected void assertJsonMessage(JsonObject object, Message... messages) {
        for (Message m : messages) {
            this.assertJsonMessage(object, m.getCategory().name(), m.getSeverity().name(), m.getNumber());
        }
    }

    protected <T extends AbstractBaseEntity> T assrt(Class<T> clazz, String property, Object value) {
        List<T> l = this.find(clazz, property, value);
        Assertions.assertEquals(1, l.size());
        T e = l.iterator().next();
        Assertions.assertNotNull(e);
        return e;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected <T extends AbstractBaseEntity> void assertNotFound(Class<T> clazz, String id) {
        T e = this.find(clazz, id);
        Assertions.assertNull(e);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void assertNotModified(AbstractEntity entity) {
        AbstractEntity database = this.assrt(entity.getClass(), entity.getId());
        Assertions.assertEquals(entity.getModified(), database.getModified());
    }

    protected <T extends AbstractBaseEntity> T find(Class<T> clazz, String key, Map<String, String> ids) {
        String id = ids.get(key);
        Assertions.assertNotNull(id);
        return this.find(clazz, id);
    }

    protected <T extends AbstractBaseEntity> T find(Class<T> clazz, String id) {
        return this.em().find(clazz, id);
    }

    protected <T extends AbstractBaseEntity> List<T> find(Class<T> clazz, String property, Object value) {
        CriteriaBuilder cb = this.em().getCriteriaBuilder();

        CriteriaQuery<T> query = cb.createQuery(clazz);
        Root<T> root = query.from(clazz);
        query.where(cb.equal(root.get(property), value));
        TypedQuery<T> typedQuery = this.em().createQuery(query);
        return typedQuery.getResultList();
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractBaseEntity> T reload(T entity) {
        return this.find((Class<T>) entity.getClass(), entity.getId());
    }

    protected Map<String, String> userToken() {
        return this.userToken(this.getDefaultUsername(), this.getDefaultUserPassword());
    }

    protected Map<String, String> userToken(String username, String password) {
        Credentials creds = new Credentials(username, password);
        ResponseEntity<JsonObject> response = this.postJson("/api/v1/login", creds);
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        String jwt = cookies.stream(). //
                filter(c -> c.startsWith(this.cookieName)). //
                filter(c -> {
                    Matcher m = AbstractRestTest.COOKIE_MAX_AGE_ZERO.matcher(c);
                    return m.find();
                }). //
                map(c -> c.split(";")[0]). //
                findAny(). //
                orElse(null);
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.COOKIE, jwt);
        return headers;
    }

    public String getDefaultAppName() {
        return this.getFactory().getDefaultAppName();
    }

    public String getDefaultUsername() {
        return this.getFactory().getDefaultUsername();
    }

    public String getDefaultUserPassword() {
        return this.getFactory().getDefaultUserPassword();
    }

    protected EntityManager em() {
        return this.entityManager;
    }

    protected Factory getFactory() {
        return this.factory;
    }
}

package io.github.lc.oss.mc.scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.signing.Algorithms;
import io.github.lc.oss.commons.testing.AbstractFactory;
import io.github.lc.oss.mc.api.ApiObject;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.entity.AbstractEntity;
import io.github.lc.oss.mc.scheduler.app.entity.Node;
import io.github.lc.oss.mc.scheduler.app.entity.User;
import io.github.lc.oss.mc.scheduler.app.entity.UserHash;
import io.github.lc.oss.mc.scheduler.app.model.NodeTypes;
import io.github.lc.oss.mc.scheduler.app.service.PasswordHasher;
import io.github.lc.oss.mc.scheduler.security.WebUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class Factory extends AbstractFactory {
    @Autowired
    private JsonService jsonService;
    @Autowired
    private L10N l10n;
    @Autowired
    private PasswordHasher passwordHasher;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Rollback(false)
    public void initDatabase() {
        this.defaultData();
    }

    public String getDefaultAppName() {
        return this.getText("application.name");
    }

    public String getDefaultAppUrl() {
        return "http://localhost:8080";
    }

    public String getDefaultUsername() {
        return "user";
    }

    public String getDefaultUserPassword() {
        return "password";
    }

    private Map<String, String> defaultData() {
        try {
            Map<String, String> data = new HashMap<>();

            WebUser user = new WebUser("System", "System", "DEADBEEF-DEAD-BEEF-DEAD-BEEFDEADBEEF", new ArrayList<>());

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null,
                    user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            this.scheduler();

            UserHash hash = new UserHash();
            this.setBasic(hash);
            hash.setHash(this.passwordHasher.hash(this.getDefaultUserPassword()));

            User defaultUser = new User();
            this.setBasic(defaultUser);
            defaultUser.setExternalId(UUID.randomUUID().toString());
            defaultUser.setUsername(this.getDefaultUsername());
            defaultUser.addHash(hash);
            this.persist(defaultUser);

            return data;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public Node scheduler() {
        Node n = new Node();
        this.setBasic(n);
        n.setClusterName(null); // the scheduler is explicitly not a part of any cluster
        n.setName(this.getText("application.default.scheduler.name"));
        n.setStatus(Status.Available);
        n.setType(NodeTypes.Scheduler);
        n.setUrl(this.getDefaultAppUrl());
        n.setPublicKey("MCowBQYDK2VwAyEA/nhJdAGmoLecvJlhjv6KpWGeS5EBtD3jE873wopqEAA=");
        this.persist(n);
        return n;
    }

    public Node worker(String name) {
        Node n = new Node();
        this.setBasic(n);
        n.setClusterName("JUnit");
        n.setName(name);
        n.setStatus(Status.Available);
        n.setType(NodeTypes.Worker);
        n.setUrl("http://localhost:8081/" + name);
        n.setPublicKey("MCowBQYDK2VwAyEA/nhJdAGmoLecvJlhjv6KpWGeS5EBtD3jE873wopqEAA=");
        this.persist(n);
        return n;
    }

    public SignedRequest sign(String nodeId, ApiObject request) {
        return this.sign(nodeId, System.currentTimeMillis(), request);
    }

    public SignedRequest sign(String nodeId, long created, ApiObject request) {
        SignedRequest sr = new SignedRequest();
        sr.setCreated(created);
        sr.setNodeId(nodeId);
        sr.setBody(Encodings.Base64.encode(this.getJsonService().to(request)));
        sr.setSignature(Algorithms.ED25519.getSignature(
                "MC4CAQAwBQYDK2VwBCIEIJJw6OfOvokC4xO+t247uiR/3ffxrz0i8EBFRH0NWFHs", sr.getSignatureData()));
        return sr;
    }

    protected JsonService getJsonService() {
        if (this.jsonService == null) {
            this.jsonService = new io.github.lc.oss.commons.web.services.JsonService();
        }
        return this.jsonService;
    }

    protected String getText(String id) {
        if (this.l10n == null) {
            return id;
        }
        return this.l10n.getText(this.l10n.getDefaultLocale(), id);
    }

    protected void setBasic(AbstractEntity entity) {
        this.setField("createdBy", "DEADDEAD-BEEF-BEEF-DEAD-DEADBEEFBEEF", entity);
        if (this.em() == null) {
            this.setField("id", UUID.randomUUID().toString(), entity);
            this.setField("modified", new Date(), entity);
            this.setField("modifiedBy", "DEADDEAD-BEEF-BEEF-DEAD-DEADBEEFBEEF", entity);
        }
    }

    protected void persist(AbstractEntity entity) {
        if (this.em() != null) {
            this.em().persist(entity);
        }
    }

    protected EntityManager em() {
        return this.entityManager;
    }
}

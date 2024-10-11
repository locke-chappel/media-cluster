package io.github.lc.oss.mc.scheduler.app.service;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.services.AbstractRuntimeService;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.entity.Node;
import io.github.lc.oss.mc.scheduler.app.entity.User;
import io.github.lc.oss.mc.scheduler.app.entity.UserHash;
import io.github.lc.oss.mc.scheduler.app.model.NodeTypes;
import io.github.lc.oss.mc.scheduler.app.repository.NodeRepository;
import io.github.lc.oss.mc.scheduler.app.repository.UserRepository;
import io.github.lc.oss.mc.scheduler.security.SecureConfig;
import io.github.lc.oss.mc.scheduler.security.WebUser;
import jakarta.annotation.PostConstruct;

@Service
public class FirstRunService extends AbstractRuntimeService {
    private static final Logger logger = LoggerFactory.getLogger(FirstRunService.class);

    @Autowired
    private L10N l10n;
    @Autowired
    private NodeRepository nodeRepo;
    @Autowired
    private PasswordHasher passwordHasher;
    @Autowired
    private SecureConfig secureConfig;
    @Autowired
    private SignatureService signatureService;
    @Autowired
    private UserRepository userRepo;

    @Value("${application.keyStore.path:${user.home}/app-data/app.jks}")
    private String keyStorePath;
    @Value("${application.keyStore.keytoolPath:keytool}")
    private String keytoolPath;
    @Value("${application.firstrun.username:}")
    private String username;
    @Value("${application.firstrun.password:}")
    private String password;
    @Value("${server.ssl.enabled:true}")
    private boolean tlsEnabled;

    @Autowired
    protected PlatformTransactionManager txManager;

    private boolean hasNodes() {
        return this.nodeRepo.count() > 0;
    }

    private boolean hasUser() {
        try {
            return this.userRepo.count() > 0;
        } catch (Throwable ex) {
            if (StringUtils.containsIgnoreCase(ex.getMessage(), "this database is empty")) {
                /*
                 * This happens on a first run where there is no database at all, so make sure
                 * we create one.
                 */
                System.setProperty("spring.jpa.hibernate.ddl-auto", "update");
                return false;
            }

            throw ex;
        }
    }

    private boolean hasKeyStore() {
        return new File(this.keyStorePath).exists();
    }

    @PostConstruct
    public void setup() {
        if (!this.hasKeyStore()) {
            try {
                List<String> secrets = this.secureConfig.getUserJwtSecrets();
                String currentUserSessionId = secrets.get(secrets.size() - 1);

                Process p = this.exec(this.keytoolPath, //
                        "-genkeypair", //
                        "-alias", "scheduler", //
                        "-dname", "CN=scheduler", //
                        "-keystore", this.keyStorePath, //
                        "-keypass", this.secureConfig.getKeystorePassword(), //
                        "-storepass", this.secureConfig.getKeystorePassword(), //
                        "-keyalg", "EdDSA", //
                        "-keysize", "255", //
                        "-validity", "9999");
                p.waitFor(60, TimeUnit.SECONDS);
                int exitCode = p.exitValue();
                if (exitCode != 0) {
                    FirstRunService.logger.error(String.format("Executing: '%s'",
                            Arrays.stream(new String[] { this.keytoolPath, //
                                    "-genkeypair", //
                                    "-alias", "scheduler", //
                                    "-dname", "CN=scheduler", //
                                    "-keystore", this.keyStorePath, //
                                    "-keypass", this.secureConfig.getKeystorePassword(), //
                                    "-storepass", this.secureConfig.getKeystorePassword(), //
                                    "-keyalg", "EdDSA", //
                                    "-keysize", "255", //
                                    "-validity", "9999" }).collect(Collectors.joining(" "))));
                    throw new RuntimeException(
                            String.format("Error generating scheduler key pair. Keytool exited with %d", exitCode));
                }

                p = this.exec(this.keytoolPath, //
                        "-genkeypair", //
                        "-alias", currentUserSessionId, //
                        "-dname", "CN=" + currentUserSessionId, //
                        "-keystore", this.keyStorePath, //
                        "-keypass", this.secureConfig.getKeystorePassword(), //
                        "-storepass", this.secureConfig.getKeystorePassword(), //
                        "-keyalg", "EdDSA", //
                        "-keysize", "255", //
                        "-validity", "9999");
                p.waitFor(60, TimeUnit.SECONDS);
                exitCode = p.exitValue();
                if (exitCode != 0) {
                    FirstRunService.logger.error(String.format("Executing: '%s'",
                            Arrays.stream(new String[] { this.keytoolPath, //
                                    "-genkeypair", //
                                    "-alias", currentUserSessionId, //
                                    "-dname", "CN=" + currentUserSessionId, //
                                    "-keystore", this.keyStorePath, //
                                    "-keypass", this.secureConfig.getKeystorePassword(), //
                                    "-storepass", this.secureConfig.getKeystorePassword(), //
                                    "-keyalg", "EdDSA", //
                                    "-keysize", "255", //
                                    "-validity", "9999" }).collect(Collectors.joining(" "))));
                    throw new RuntimeException(
                            String.format("Error generating user session key pair. Keytool exited with %d", exitCode));
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException("Error generating required keypairs", ex);
            }
        }

        if (!FirstRunService.this.hasUser()) {
            if (StringUtils.isAnyBlank(this.username, this.password)) {
                throw new RuntimeException(
                        "application.firstrun.username and application.firstrun.password must be set. Unable to initalize database");
            }
        }

        TransactionTemplate tmpl = new TransactionTemplate(this.txManager);
        tmpl.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    WebUser user = new WebUser("System", "System", "DEADBEEF-DEAD-BEEF-DEAD-BEEFDEADBEEF",
                            new ArrayList<>());

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null,
                            user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    if (!FirstRunService.this.hasNodes()) {
                        Node node = new Node();
                        node.setClusterName(null); // the scheduler is explicitly not a part of any cluster
                        node.setName(FirstRunService.this.l10n.getText(FirstRunService.this.l10n.getDefaultLocale(),
                                "application.default.scheduler.name"));
                        node.setType(NodeTypes.Scheduler);
                        node.setStatus(Status.Available);
                        node.setUrl((FirstRunService.this.tlsEnabled ? "https://" : "http://")
                                + FirstRunService.this.getHostIP());
                        node.setPublicKey(FirstRunService.this.signatureService.getPublicKey());
                        FirstRunService.this.nodeRepo.saveAndFlush(node);
                    }

                    if (!FirstRunService.this.hasUser()) {
                        UserHash hash = new UserHash();
                        hash.setHash(FirstRunService.this.passwordHasher.hash(FirstRunService.this.password));

                        User defaultUser = new User();
                        defaultUser.setExternalId(UUID.randomUUID().toString());
                        defaultUser.setUsername(FirstRunService.this.username);
                        defaultUser.addHash(hash);
                        defaultUser = FirstRunService.this.userRepo.saveAndFlush(defaultUser);
                    }
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }
        });
    }

    /**
     * Best guess at a default IP to use for the scheduler node. This can be edited
     * later via the UI.
     */
    private String getHostIP() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 53));
            return socket.getLocalAddress().getHostAddress();
        } catch (IOException ex) {
            FirstRunService.logger.error("Unable to detect host IP. Defaulting to blank.", ex);
            return StringUtils.EMPTY;
        }
    }
}

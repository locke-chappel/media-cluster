package io.github.lc.oss.mc.scheduler.security;

import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import io.github.lc.oss.commons.api.identity.ApplicationInfo;
import io.github.lc.oss.commons.jwt.Jwt;
import io.github.lc.oss.commons.jwt.JwtHeader;
import io.github.lc.oss.commons.jwt.JwtService;
import io.github.lc.oss.commons.jwt.UserCache;
import io.github.lc.oss.commons.signing.Algorithm;
import io.github.lc.oss.commons.signing.Algorithms;
import io.github.lc.oss.commons.util.IoTools;
import io.github.lc.oss.commons.web.util.CookieUtil;
import io.github.lc.oss.mc.scheduler.app.entity.User;
import io.github.lc.oss.mc.scheduler.app.service.IdentityService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtManager extends JwtService {
    private static final Algorithm ALGORITHM = Algorithms.ED25519;
    private static final long TOKEN_REFRESH_TIMEOUT = 30 * 1000;

    private final Logger logger = LoggerFactory.getLogger(JwtManager.class);

    /*
     * exposed for testing purposes only
     */
    class RefreshCleanup extends TimerTask {
        private final String subject;

        public RefreshCleanup(String subject) {
            this.subject = subject;
        }

        @Override
        public void run() {
            synchronized (JwtManager.this.inProgressRefresh) {
                JwtManager.this.inProgressRefresh.remove(this.subject);
            }
        }
    }

    @Autowired
    private Clock clock;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private SecureConfig secureConfig;
    @Autowired
    private UserCache<WebUser> userCache;

    @Value("${server.servlet.session.cookie.secure:true}")
    private boolean secureCookies;
    @Value("${server.servlet.session.cookie.path:/}")
    private String cookiePath;
    @Value("${server.servlet.session.cookie.domain:}")
    private String cookieDomain;
    @Value("${server.servlet.session.cookie.name:__Host-media-scheduler}")
    private String cookieName;
    @Value("${application.keyStore.path:${user.home}/app-data/app.jks}")
    private String keyStorePath;

    private long sessionTimeout = -1;
    private long sessionMaxAge = -1;

    private Set<String> inProgressRefresh = new HashSet<>();
    private String currentUserSecretId;
    private Map<String, KeyPair> secrets;

    @Override
    protected void log(String message, Throwable ex) {
        if (ex != null) {
            this.logger.error(message, ex);
        } else if (!StringUtils.equals(message, "Token parsed to null")) {
            // null tokens are common for the public pages
            this.logger.warn(message);
        }
    }

    private String getCurrentUserSecretId() {
        if (this.currentUserSecretId == null) {
            List<String> secrets = this.secureConfig.getUserJwtSecrets();
            this.currentUserSecretId = secrets.get(secrets.size() - 1);
        }
        return this.currentUserSecretId;
    }

    private Map<String, KeyPair> getSecrets() {
        if (this.secrets == null) {
            if (StringUtils.isBlank(this.keyStorePath)) {
                throw new RuntimeException("Key store path cannot be blank.");
            }

            Map<String, KeyPair> map = new HashMap<>();
            List<String> ids = this.secureConfig.getUserJwtSecrets();
            ids.forEach(id -> map.put(id, this.loadKey(this.keyStorePath, id)));
            this.secrets = Collections.unmodifiableMap(map);
        }
        return this.secrets;
    }

    private KeyPair loadKey(String keyStorePath, String alias) {
        char[] password = null;
        try {
            password = this.secureConfig.getKeystorePassword().toCharArray();
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(IoTools.getAbsoluteFilePath(keyStorePath)), password);
            PrivateKey pk = (PrivateKey) ks.getKey(alias, password);
            Certificate cert = ks.getCertificate(alias);
            return new KeyPair(cert.getPublicKey(), pk);
        } catch (Exception ex) {
            throw new RuntimeException("Error loading KeyPair", ex);
        } finally {
            Arrays.fill(password, ' ');
        }
    }

    @Override
    public boolean isAlgorithmAllowed(Algorithm alg) {
        if (alg == null) {
            return false;
        }
        return JwtManager.ALGORITHM.getId().equals(alg.getId());
    }

    public void cleanCache() {
        this.userCache.clean();
        this.getRevocationList().clean();
    }

    public void invalidate(HttpServletRequest request, HttpServletResponse response) {
        String token = this.getToken(request);
        if (token != null) {
            Jwt jwt = this.validate(token);
            if (jwt != null) {
                this.userCache.remove(jwt.getSubject());
                this.invalidate(jwt);
            } else {
                String[] parts = token.split("\\.");
                if (parts.length == 3) {
                    this.invalidate(parts[2], this.getSessionMaxAge());
                }
            }
        }

        if (response != null) {
            this.setCookie(response, token, 0);
        }
    }

    @Override
    public String getAudience() {
        return this.identityService.getApplicationId();
    }

    @Override
    public Set<String> getIssuers() {
        return this.identityService.getJwtIssuers();
    }

    @Override
    protected long now() {
        return this.clock.instant().toEpochMilli();
    }

    private long getSessionTimeout() {
        if (this.sessionTimeout < 0) {
            this.loadSessionLimits();
        }
        return this.sessionTimeout;
    }

    private long getSessionMaxAge() {
        if (this.sessionMaxAge < 0) {
            this.loadSessionLimits();
        }
        return this.sessionMaxAge;
    }

    private void loadSessionLimits() {
        ApplicationInfo applicationInfo = this.identityService.getApplicationInfo();
        this.sessionTimeout = applicationInfo.getSessionTimeout();
        this.sessionMaxAge = applicationInfo.getSessionMax();
    }

    public Jwt issueCookie(HttpServletRequest request, HttpServletResponse response, User user) {
        this.invalidate(request, response);

        WebUser iu = new WebUser(user.getUsername(), user.getUsername(), user.getId(),
                Arrays.asList(new SimpleGrantedAuthority(Permissions.User.getPermission())));

        Jwt token = super.issue(JwtManager.ALGORITHM, this.now() + this.getSessionTimeout(), null, user.getExternalId(),
                this.identityService.getApplicationId(), this.getAudience());
        token.getHeader().setKeyId(this.getCurrentUserSecretId());
        token.getPayload().setPermissions(this.getAudience(), Arrays.asList(Permissions.User.getPermission()));
        token.getPayload().setDisplayName(user.getUsername());

        iu.setJwtId(token.getTokenId());
        this.userCache.add(iu, this.computeMaxExpiration(token));

        String encoded = this.signAndEncode(token);
        this.setCookie(response, encoded, (int) this.getSessionTimeout() / 1000);
        return token;
    }

    protected void setCookie(HttpServletResponse response, String token, int maxAge) {
        Cookie c = new Cookie(this.cookieName, token);
        c.setDomain(this.cookieDomain);
        c.setPath(this.cookiePath);
        c.setSecure(this.secureCookies);
        c.setMaxAge(maxAge);
        c.setHttpOnly(true);
        response.addCookie(c);
    }

    private String getToken(HttpServletRequest request) {
        return this.getCookie(request);
    }

    private String getCookie(HttpServletRequest request) {
        Cookie cookie = CookieUtil.getCookie(request, this.cookieName);
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }

    @Override
    protected byte[] getSignSecret(JwtHeader header, byte[] defaultSecret) {
        if (StringUtils.isBlank(header.getKeyId())) {
            return null;
        }

        KeyPair kp = this.getSecrets().get(header.getKeyId());
        if (kp == null) {
            return null;
        }

        return kp.getPrivate().getEncoded();
    }

    @Override
    protected byte[] getValidateSecret(JwtHeader header, byte[] defaultSecret) {
        if (StringUtils.isBlank(header.getKeyId())) {
            return null;
        }

        KeyPair kp = this.getSecrets().get(header.getKeyId());
        if (kp == null) {
            return null;
        }

        return kp.getPublic().getEncoded();
    }

    public WebUser validate(HttpServletRequest request, HttpServletResponse response) {
        String jwt = this.getToken(request);
        Jwt token = this.validate(jwt);
        if (token == null) {
            CookieUtil.deleteCookie(request, response, this.cookieName);
            return null;
        }

        if (token.getExpiration() * 1000 - this.now() < this.getSessionTimeout() / 2) {
            boolean inProgress = true;
            final String subject = token.getSubject();
            synchronized (this.inProgressRefresh) {
                inProgress = this.inProgressRefresh.contains(subject);
                this.inProgressRefresh.add(subject);

                Timer t = new Timer();
                t.schedule(this.newRefreshCleanup(subject), JwtManager.TOKEN_REFRESH_TIMEOUT);
            }

            if (!inProgress) {
                String newJwt = this.identityService.refreshSession(jwt);
                token = this.validate(newJwt);
                if (token == null) {
                    CookieUtil.deleteCookie(request, response, this.cookieName);
                    return null;
                }
                this.setCookie(response, newJwt, token.getExpirationMillis());
            }
        }

        return this.loadUser(token);
    }

    public void setCookie(HttpServletResponse response, String token, long expiration) {
        int expires = 0;
        if (expiration > this.now()) {
            /*
             * REVIEW: How to test? Used by external IdM solution... see login process for
             * raw JWT path (i.e. not credentials)
             */
            expires = (int) (expiration - this.now()) / 1000;
        }

        Cookie cookie = new Cookie(this.cookieName, token);
        cookie.setDomain(this.cookieDomain);
        cookie.setPath(this.cookiePath);
        cookie.setSecure(this.secureCookies);
        cookie.setMaxAge(expires);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    public WebUser loadUser(Jwt token) {
        String externalId = token.getSubject();
        WebUser user = this.userCache.get(externalId);
        if (user == null) {
            user = new WebUser( //
                    token.getPayload().getSubject(), //
                    token.getPayload().getDisplayName(), //
                    token.getSubject(), //
                    token.getPayload().getPermissions(this.getAudience()).stream(). //
                            map(p -> new SimpleGrantedAuthority(p)). //
                            collect(Collectors.toSet()));
            user.setJwtId(token.getTokenId());
            this.userCache.add(user, externalId, this.computeMaxExpiration(token));
        }
        return user;
    }

    RefreshCleanup newRefreshCleanup(String subject) {
        return new RefreshCleanup(subject);
    }

    private long computeMaxExpiration(Jwt token) {
        return token.getIssuedAt() * 1000l + this.getSessionMaxAge();
    }
}

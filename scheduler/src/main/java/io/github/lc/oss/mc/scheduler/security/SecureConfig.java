package io.github.lc.oss.mc.scheduler.security;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lc.oss.commons.encryption.config.ConfigKey;
import io.github.lc.oss.commons.encryption.config.EncryptedConfig;

public class SecureConfig extends EncryptedConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecureConfig.class);

    public enum Keys implements ConfigKey {
        DatabaseUser(User.class),
        DatabaseUrl(String.class),
        KeystorePassword(String.class),
        UserJwtSecrets(List.class),
        JwtIssuers(List.class),
        PrivateKey(String.class);

        private final Class<?> type;

        private Keys(Class<?> type) {
            this.type = type;
        }

        @Override
        public Class<?> type() {
            return this.type;
        }
    }

    @Override
    protected void logError(String message) {
        SecureConfig.logger.error(message);
    }

    public SecureConfig() {
        super(Keys.class);
    }

    public String getDatabaseUrl() {
        return (String) this.get(Keys.DatabaseUrl);
    }

    public User getDatabaseUser() {
        return (User) this.get(Keys.DatabaseUser);
    }

    @SuppressWarnings("unchecked")
    public List<String> getUserJwtSecrets() {
        return (List<String>) this.get(Keys.UserJwtSecrets);
    }

    @SuppressWarnings("unchecked")
    public List<String> getJwtIssuers() {
        return (List<String>) this.get(Keys.JwtIssuers);
    }

    public String getKeystorePassword() {
        return (String) this.get(Keys.KeystorePassword);
    }

    public String getPrivateKey() {
        return (String) this.get(Keys.PrivateKey);
    }
}

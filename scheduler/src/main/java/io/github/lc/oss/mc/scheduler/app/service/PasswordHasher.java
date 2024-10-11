package io.github.lc.oss.mc.scheduler.app.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import io.github.lc.oss.commons.hashing.passwords.PasswordHashes;
import io.github.lc.oss.mc.scheduler.app.entity.User;
import io.github.lc.oss.mc.scheduler.app.entity.UserHash;

@Service
public class PasswordHasher {
    public String hash(String password) {
        return PasswordHashes.PBKDF2.hash(password);
    }

    public boolean matches(String password, User user) {
        if (user == null) {
            return false;
        }

        UserHash hash = user.getHash();
        if (hash == null) {
            return false;
        }

        if (StringUtils.isBlank(hash.getHash()) || StringUtils.isBlank(password)) {
            return false;
        }

        return PasswordHashes.PBKDF2.matches(password, hash.getHash());
    }
}

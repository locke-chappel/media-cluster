package io.github.lc.oss.mc.scheduler.app.service;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.signing.Algorithms;
import io.github.lc.oss.commons.util.IoTools;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.scheduler.security.SecureConfig;

@Service
public class SignatureService extends AbstractService {
    @Autowired
    private SecureConfig secureConfig;

    @Value("${application.keyStore.path:${user.home}/app-data/app.jks}")
    private String keyStorePath;

    public String getPublicKey() {
        return Encodings.Base64.encode(this.loadSchedulerKey().getPublic().getEncoded());
    }

    public void sign(SignedRequest request) {
        String sig = Algorithms.ED25519.getSignature( //
                this.loadSchedulerKey().getPrivate().getEncoded(), //
                request.getSignatureData().getBytes(StandardCharsets.UTF_8));
        request.setSignature(sig);
    }

    private KeyPair loadSchedulerKey() {
        char[] password = null;
        try {
            password = this.secureConfig.getKeystorePassword().toCharArray();
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(IoTools.getAbsoluteFilePath(this.keyStorePath)), password);
            PrivateKey pk = (PrivateKey) ks.getKey("scheduler", password);
            Certificate cert = ks.getCertificate("scheduler");
            return new KeyPair(cert.getPublicKey(), pk);
        } catch (Exception ex) {
            throw new RuntimeException("Error loading KeyPair", ex);
        } finally {
            Arrays.fill(password, ' ');
        }
    }
}

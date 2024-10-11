package io.github.lc.oss.mc.worker;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.signing.Algorithms;
import io.github.lc.oss.commons.testing.AbstractFactory;
import io.github.lc.oss.mc.api.ApiObject;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.Profile;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.worker.security.Configuration;

public class Factory extends AbstractFactory {
    @Autowired
    private JsonService jsonService;
    @Autowired
    private Configuration config;

    public Job job() {
        Job job = new Job();
        job.setId(UUID.randomUUID().toString());
        job.setType(JobTypes.Video);
        job.setSource("sleep.avi");
        job.setIndex(1);
        job.setBatchIndex(1);
        job.setProfile(this.getJsonService().to(this.profile()));
        return job;
    }

    public Job jobError() {
        Job job = new Job();
        job.setId(UUID.randomUUID().toString());
        job.setType(JobTypes.Video);
        job.setSource("error.avi");
        job.setIndex(1);
        return job;
    }

    public Profile profile() {
        Profile p = new Profile();
        p.setExt("mkv");
        p.setSliceLength(10000);
        p.setName("Junit-Profile");
        p.setAudioArgs(Arrays.asList("-c:a", "copy"));
        p.setVideoArgs(Arrays.asList("-c:v", "copy"));
        return p;
    }

    public SignedRequest sign(ApiObject request) {
        return this.sign(this.getConfig().getId(), request);
    }

    public SignedRequest sign(String request) {
        return this.sign(this.getConfig().getId(), request);
    }

    public SignedRequest sign(String nodeId, ApiObject request) {
        return this.sign(nodeId, System.currentTimeMillis(), request);
    }

    public SignedRequest sign(String nodeId, String request) {
        return this.sign(nodeId, System.currentTimeMillis(), request);
    }

    public SignedRequest sign(long created, ApiObject request) {
        return this.sign(this.getConfig().getId(), created, request);
    }

    public SignedRequest sign(long created, String request) {
        return this.sign(this.getConfig().getId(), created, request);
    }

    public SignedRequest sign(String nodeId, long created, ApiObject request) {
        return this.sign(nodeId, created, Encodings.Base64.encode(this.getJsonService().to(request)));
    }

    public SignedRequest sign(String nodeId, long created, String request) {
        SignedRequest sr = new SignedRequest();
        sr.setCreated(created);
        sr.setNodeId(nodeId);
        sr.setBody(request);
        sr.setSignature(Algorithms.ED25519.getSignature(this.getConfig().getPrivateKey(), sr.getSignatureData()));
        return sr;
    }

    public Configuration getConfig() {
        return this.config;
    }

    public void setConfig(Configuration config) {
        Assertions.assertNull(this.config);
        this.config = config;
    }

    public JsonService getJsonService() {
        if (this.jsonService == null) {
            this.jsonService = new io.github.lc.oss.commons.web.services.JsonService();
        }
        return this.jsonService;
    }
}

package io.github.lc.oss.mc.scheduler.app.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SignedIdentityRequest implements io.github.lc.oss.commons.api.identity.SignedRequest {
    private long created;
    private final String applicationId;
    private final String body;
    private final String signature;

    public SignedIdentityRequest( //
            @JsonProperty("created") long created, //
            @JsonProperty("applicationId") String applicationId, //
            @JsonProperty("body") String body, //
            @JsonProperty("signature") String signature //
    ) {
        this.created = created;
        this.applicationId = applicationId;
        this.body = body;
        this.signature = signature;
    }

    @Override
    public long getCreated() {
        return this.created;
    }

    @Override
    public String getApplicationId() {
        return this.applicationId;
    }

    @Override
    public String getBody() {
        return this.body;
    }

    @Override
    public String getSignature() {
        return this.signature;
    }
}

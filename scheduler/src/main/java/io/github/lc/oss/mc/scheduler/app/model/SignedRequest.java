package io.github.lc.oss.mc.scheduler.app.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_EMPTY)
public class SignedRequest {
    private long created;
    private final String nodeId;
    private final String body;
    private final String signature;

    public SignedRequest( //
            @JsonProperty("created") long created, //
            @JsonProperty("nodeId") String nodeId, //
            @JsonProperty("body") String body, //
            @JsonProperty("signature") String signature //
    ) {
        this.created = created;
        this.nodeId = nodeId;
        this.body = body;
        this.signature = signature;
    }

    public long getCreated() {
        return this.created;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String getBody() {
        return this.body;
    }

    public String getSignature() {
        return this.signature;
    }

    public String getSignatureData() {
        return Long.toString(this.getCreated()) + this.getNodeId() + this.getBody();
    }
}

package io.github.lc.oss.mc.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class SignedRequest implements ApiObject {
    private long created = System.currentTimeMillis();

    private String body;
    private String signature;
    private String nodeId;

    /**
     * The timestamp in milliseconds since the Unix epoch UTC that this request was
     * created. Requests over 30 seconds old are rejected even if otherwise valid.
     */
    public long getCreated() {
        return this.created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    /**
     * Base64 encoded JSON body of the request.
     */
    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Node ID associated with this request.
     */
    public String getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Signature of the value returned by {@link #getSignatureData()}
     */
    public String getSignature() {
        return this.signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * Returns the data to verified via the signature.
     */
    public String getSignatureData() {
        return Long.toString(this.getCreated()) + this.getNodeId() + this.getBody();
    }
}

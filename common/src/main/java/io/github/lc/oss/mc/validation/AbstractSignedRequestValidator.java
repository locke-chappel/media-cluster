package io.github.lc.oss.mc.validation;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.signing.Algorithms;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.SignedRequest;

public abstract class AbstractSignedRequestValidator<Type extends SignedRequest> extends AbstractValidator<Type> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSignedRequestValidator.class);
    private static final long MAX_SIGNED_REQUEST_AGE = 30 * 1000;
    private static final long MAX_SIGNED_REQUEST_FUTURE = 5 * 1000;

    @Value("${application.requests.maxAge:}")
    private Long maxRequestAge;
    @Value("${application.requests.maxFuture:}")
    private Long maxRequestFuture;

    protected abstract String getPublicKey(String nodeId);

    protected long getMaxRequestAge() {
        if (this.maxRequestAge != null) {
            return this.maxRequestAge;
        }
        return AbstractSignedRequestValidator.MAX_SIGNED_REQUEST_AGE;
    }

    protected long getMaxRequestFutureAge() {
        if (this.maxRequestFuture != null) {
            return this.maxRequestFuture;
        }
        return AbstractSignedRequestValidator.MAX_SIGNED_REQUEST_FUTURE;
    }

    protected Set<Message> validateBasic(Type request) {
        Set<Message> messages = this.valid();

        messages.addAll(this.validateHasRequest(request));
        if (!messages.isEmpty()) {
            return messages;
        }

        messages.addAll(this.validateSignature(request));
        messages.addAll(this.validateCreation(request));

        return messages;
    }

    protected Set<Message> validateHasRequest(Type request) {
        Set<Message> messages = this.valid();

        if (this.missingValue(request)) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing, this.getFieldVar("SignedRequest")));
            return messages;
        }

        return messages;
    }

    protected Set<Message> validateCreation(Type request) {
        Set<Message> messages = this.valid();

        long now = System.currentTimeMillis();
        long maxAge = now - this.getMaxRequestAge();
        long maxFuture = now + this.getMaxRequestFutureAge();
        if (request.getCreated() > maxFuture || request.getCreated() < maxAge) {
            AbstractSignedRequestValidator.logger.error( //
                    String.format("Expired Request: %s requested %d but is now %d", //
                            request.getSignature(), //
                            request.getCreated(), //
                            now));
            messages.add(this.toMessage(Messages.Authentication.ExpiredRequest));
            return messages;
        }

        return messages;
    }

    protected Set<Message> validateSignature(Type request) {
        Set<Message> messages = this.valid();

        String sig = request.getSignature();
        if (StringUtils.isBlank(sig)) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing,
                    this.getFieldVar("SignedRequest.Signature")));
            return messages;
        }

        String data = request.getSignatureData();
        if (!Algorithms.ED25519.isSignatureValid(this.getPublicKey(request.getNodeId()), data, sig)) {
            AbstractSignedRequestValidator.logger.error( //
                    String.format("Invalid signature: %s", //
                            request.getSignature()));
            messages.add(this.toMessage(Messages.Authentication.InvalidSignature));
            return messages;
        }

        return messages;
    }
}

package io.github.lc.oss.mc.worker.app.validation;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.validation.AbstractSignedRequestValidator;
import io.github.lc.oss.mc.worker.security.Configuration;

@Component
public class SignedRequestValidator extends AbstractSignedRequestValidator<SignedRequest> {
    @Autowired
    private Configuration config;

    @Override
    protected String getPublicKey(String nodeId) {
        /*
         * Note: Since this is the worker and this validator is for inbound requests
         * only, the only valid node that can send these requests is the scheduler
         * itself. So while we technically could look up the key by node ID, this
         * asserts that the request is from the configured scheduler node only.
         */
        return this.config.getSchedulerPublicKey();
    }

    @Override
    public Set<Message> validate(SignedRequest request) {
        Set<Message> messages = this.valid();

        messages.addAll(this.validateBasic(request));
        if (!messages.isEmpty()) {
            return messages;
        }

        if (!StringUtils.equals(this.config.getId(), request.getNodeId())) {
            messages.add(this.toMessage(Messages.Application.InvalidNode));
            return messages;
        }

        return messages;
    }
}

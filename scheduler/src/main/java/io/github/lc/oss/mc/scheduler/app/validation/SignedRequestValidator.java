package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.validation.AbstractSignedRequestValidator;
import io.github.lc.oss.mc.scheduler.app.entity.Node;
import io.github.lc.oss.mc.scheduler.app.repository.NodeRepository;

@Component
public class SignedRequestValidator extends AbstractSignedRequestValidator<SignedRequest> {
    @Autowired
    private NodeRepository nodeRepo;

    @Override
    protected String getPublicKey(String nodeId) {
        if (StringUtils.isBlank(nodeId)) {
            return null;
        }

        Node worker = this.nodeRepo.findById(nodeId).orElse(null);
        if (worker == null) {
            return null;
        }
        return worker.getPublicKey();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Message> validate(SignedRequest request) {
        Set<Message> messages = this.valid();
        if (this.missingValue(request)) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing, this.getFieldVar("SignedRequest")));
            return messages;
        }

        Node worker = this.nodeRepo.findById(request.getNodeId()).orElse(null);
        if (worker == null) {
            messages.add(this.toMessage(Messages.Application.InvalidNode));
            return messages;
        }

        messages.addAll(this.validateBasic(request));
        if (!messages.isEmpty()) {
            return messages;
        }

        return messages;
    }
}

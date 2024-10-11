package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lc.oss.commons.l10n.Variable;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.validation.Patterns;
import io.github.lc.oss.mc.scheduler.app.model.Node;
import io.github.lc.oss.mc.scheduler.app.model.NodeTypes;
import io.github.lc.oss.mc.scheduler.app.repository.NodeRepository;

@Component
public class NodeValidator extends AbstractValidator<Node> {
    @Autowired
    private NodeRepository nodeRepo;

    @Override
    public Set<Message> validate(Node request) {
        Set<Message> messages = this.valid();
        if (this.missingValue(request)) {
            messages.add(
                    this.toMessage(Messages.Application.RequiredFieldMissing, this.getFieldVar("nodes.node.header")));
            return messages;
        }

        this.validateString(messages, "nodes.node.name", Patterns.Name, true, request.getName());
        this.validateString(messages, "nodes.node.url", Patterns.Url, true, request.getUrl());

        if (!messages.isEmpty()) {
            return messages;
        }

        /* Database Validations */
        io.github.lc.oss.mc.scheduler.app.entity.Node existing = this.nodeRepo.findByNameIgnoreCase(request.getName());
        if (existing != null && !StringUtils.equals(existing.getId(), request.getId())) {
            messages.add(
                    this.toMessage(Messages.Application.DuplicateNodeName, new Variable("Name", request.getName())));
        }

        if (StringUtils.isNotBlank(request.getId())) {
            existing = this.nodeRepo.findById(request.getId()).orElse(null);
            if (existing.getType() != NodeTypes.Scheduler) {
                this.validateString(messages, "nodes.node.clusterName", Patterns.Name, true, request.getClusterName());
            }
        } else {
            this.validateString(messages, "nodes.node.clusterName", Patterns.Name, true, request.getClusterName());
        }

        return messages;
    }
}

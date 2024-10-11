package io.github.lc.oss.mc.worker.app.validation;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.Profile;
import io.github.lc.oss.mc.validation.AbstractValidator;
import io.github.lc.oss.mc.validation.Patterns;

@Component
public class JobRequestValidator extends AbstractValidator<Job> {
    @Autowired
    private ProfileValidator profileValidator;
    @Autowired
    private JsonService jsonService;

    @Override
    public Set<Message> validate(Job instance) {
        Set<Message> messages = this.valid();
        if (this.missingValue(instance)) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing, this.getFieldVar("Job")));
            return messages;
        }

        this.validateString(messages, "Job.Id", Patterns.ExternalId, true, instance.getId());
        this.validateString(messages, "Job.Source", Patterns.FilePath, true, instance.getSource());

        if (instance.getType() == null) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing, this.getFieldVar("Job.Type")));
        }

        if (instance.getBatchIndex() == null) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing, this.getFieldVar("Job.BatchIndex")));
        } else if (instance.getBatchIndex() < 0) {
            messages.add(this.toMessage(Messages.Application.InvalidField, this.getFieldVar("Job.BatchIndex")));
        }

        if (instance.getProfile() == null) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing, this.getFieldVar("Job.Profile")));
        } else {
            Profile profile = this.jsonService.from(instance.getProfile(), Profile.class);
            this.merge(messages, this.profileValidator, profile);
        }

        return messages;
    }
}

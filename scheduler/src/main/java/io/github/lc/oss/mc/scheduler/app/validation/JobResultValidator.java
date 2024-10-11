package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.JobResult;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.scheduler.app.repository.JobRepository;

@Component
public class JobResultValidator extends AbstractValidator<JobResult> {
    @Autowired
    private JobRepository jobRepo;

    @Override
    public Set<Message> validate(JobResult instance) {
        Set<Message> messages = this.valid();

        if (this.missingValue(instance)) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing, this.getFieldVar("JobComplete")));
            return messages;
        }

        io.github.lc.oss.mc.scheduler.app.entity.Job existing = this.jobRepo.findById(instance.getId()).orElse(null);
        if (existing == null) {
            messages.add(this.toMessage(Messages.Application.NotFound));
            return messages;
        }

        return messages;
    }
}

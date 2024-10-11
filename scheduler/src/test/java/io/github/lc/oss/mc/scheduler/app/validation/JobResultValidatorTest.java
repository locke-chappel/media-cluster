package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.JobResult;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.scheduler.app.entity.Job;
import io.github.lc.oss.mc.scheduler.app.repository.JobRepository;

public class JobResultValidatorTest extends AbstractValidatorTest {
    @Mock
    private JobRepository jobRepo;

    @InjectMocks
    private JobResultValidator validator;

    @Test
    public void test_null() {
        this.expectLocale();
        this.expectMessage(Messages.Application.RequiredFieldMissing);
        this.expectFieldVar("JobComplete");

        Set<Message> result = this.validator.validate(null);
        this.assertMessage(Messages.Application.RequiredFieldMissing, result);
    }

    @Test
    public void test_notFound() {
        this.expectLocale();
        this.expectMessage(Messages.Application.NotFound);

        JobResult jr = new JobResult();
        jr.setId("junk");

        Mockito.when(this.jobRepo.findById(jr.getId())).thenReturn(Optional.empty());

        Set<Message> result = this.validator.validate(jr);
        this.assertMessage(Messages.Application.NotFound, result);
    }

    @Test
    public void test_valid() {
        JobResult jr = new JobResult();
        jr.setId("id");

        Job job = new Job();

        Mockito.when(this.jobRepo.findById(jr.getId())).thenReturn(Optional.of(job));

        Set<Message> result = this.validator.validate(jr);
        this.assertValid(result);
    }
}

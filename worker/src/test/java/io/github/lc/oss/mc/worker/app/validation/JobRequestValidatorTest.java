package io.github.lc.oss.mc.worker.app.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.Profile;

public class JobRequestValidatorTest extends AbstractValidatorTest {
    @Mock
    private ProfileValidator profileValidator;
    @Mock
    private JsonService jsonService;

    @InjectMocks
    private JobRequestValidator validator;

    @Test
    public void test_validate_missing_request() {
        this.expectLocale();
        this.expectFieldVar("Job");
        this.expectMessage(Messages.Application.RequiredFieldMissing);

        Set<Message> result = this.validator.validate(null);
        this.assertMessage(Messages.Application.RequiredFieldMissing, result);
    }

    @Test
    public void test_validate_missing_fields() {
        this.expectLocale();
        this.expectFieldVar("Job.Id");
        this.expectFieldVar("Job.Source");
        this.expectFieldVar("Job.Type");
        this.expectFieldVar("Job.BatchIndex");
        this.expectFieldVar("Job.Profile");
        this.expectMessage(Messages.Application.RequiredFieldMissing);

        Job request = new Job();

        Set<Message> result = this.validator.validate(request);
        this.assertMessages(Arrays.asList( //
                Messages.Application.RequiredFieldMissing, //
                Messages.Application.RequiredFieldMissing, //
                Messages.Application.RequiredFieldMissing, //
                Messages.Application.RequiredFieldMissing, //
                Messages.Application.RequiredFieldMissing), //
                result);
    }

    @Test
    public void test_validate_invalid_fields() {
        this.expectLocale();
        this.expectFieldVar("Job.Id");
        this.expectFieldVar("Job.Source");
        this.expectFieldVar("Job.BatchIndex");
        this.expectMessage(Messages.Application.InvalidField);

        Profile profile = new Profile();

        Mockito.when(this.jsonService.from("{}", Profile.class)).thenReturn(profile);
        Mockito.when(this.profileValidator.validate(profile)).thenReturn(new HashSet<>());

        Job request = new Job();
        request.setId("!!!BAD!!!");
        request.setSource("..");
        request.setType(JobTypes.Audio);
        request.setBatchIndex(-1);
        request.setProfile("{}");

        Set<Message> result = this.validator.validate(request);
        this.assertMessages(Arrays.asList( //
                Messages.Application.InvalidField, //
                Messages.Application.InvalidField, //
                Messages.Application.InvalidField), //
                result);
    }

    @Test
    public void test_validate_valid() {
        Profile profile = new Profile();

        Job request = this.fac().job();

        Mockito.when(this.jsonService.from(request.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.profileValidator.validate(profile)).thenReturn(new HashSet<>());

        Set<Message> result = this.validator.validate(request);
        this.assertValid(result);
    }
}

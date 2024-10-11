package io.github.lc.oss.mc.worker.app.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.Profile;
import io.github.lc.oss.mc.validation.CommandValidator;

public class ProfileValidatorTest extends AbstractValidatorTest {
    @Mock
    private CommandValidator commandValidator;

    @InjectMocks
    private ProfileValidator validator;

    @Test
    public void test_validate_null() {
        this.expectLocale();
        this.expectFieldVar("Profile");
        this.expectMessage(Messages.Application.RequiredFieldMissing);

        Set<Message> result = this.validator.validate(null);
        this.assertMessage(Messages.Application.RequiredFieldMissing, result);
    }

    @Test
    public void test_validate_missingValues() {
        this.expectLocale();
        this.expectFieldVar("Profile.Ext");
        this.expectMessage(Messages.Application.RequiredFieldMissing);

        Profile profile = new Profile();

        Set<Message> result = this.validator.validate(profile);
        this.assertMessages(Arrays.asList( //
                Messages.Application.RequiredFieldMissing), //
                result);
    }

    @Test
    public void test_validate_invalidValues() {
        this.expectLocale();
        this.expectFieldVar("Profile.Ext");
        this.expectFieldVar("Profile.SliceLength");
        this.expectMessage(Messages.Application.InvalidField);

        Profile profile = new Profile();
        profile.setSliceLength(-1);
        profile.setExt("!");

        Set<Message> result = this.validator.validate(profile);
        this.assertMessages(Arrays.asList( //
                Messages.Application.InvalidField, //
                Messages.Application.InvalidField), //
                result);
    }

    @Test
    public void test_validate_invalidValues_v2() {
        this.expectLocale();
        this.expectFieldVar("Profile.Ext");
        this.expectFieldVar("Profile.SliceLength");
        this.expectMessage(Messages.Application.InvalidField);

        Profile profile = new Profile();
        profile.setSliceLength(1000000000);
        profile.setExt("!");

        Set<Message> result = this.validator.validate(profile);
        this.assertMessages(Arrays.asList( //
                Messages.Application.InvalidField, //
                Messages.Application.InvalidField), //
                result);
    }

    @Test
    public void test_validate_valid_nullArgs() {
        Profile profile = this.fac().profile();
        profile.setAudioArgs(null);
        profile.setCommonArgs(null);
        profile.setVideoArgs(null);

        Set<Message> result = this.validator.validate(profile);
        this.assertValid(result);
    }

    @Test
    public void test_validate_valid_noArgs() {
        Profile profile = this.fac().profile();
        profile.setAudioArgs(new ArrayList<>());
        profile.setCommonArgs(new ArrayList<>());
        profile.setVideoArgs(new ArrayList<>());

        Set<Message> result = this.validator.validate(profile);
        this.assertValid(result);
    }

    @Test
    public void test_validate_valid_allArgs() {
        Profile profile = this.fac().profile();
        profile.setAudioArgs(Arrays.asList("a"));
        profile.setCommonArgs(Arrays.asList("c"));
        profile.setVideoArgs(Arrays.asList("v"));

        Mockito.when(this.commandValidator.validate(ArgumentMatchers.notNull())).thenReturn(new HashSet<>());

        Set<Message> result = this.validator.validate(profile);
        this.assertValid(result);
    }
}

package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.scheduler.app.model.User;

public class UserValidatorTest extends AbstractValidatorTest {
    @InjectMocks
    private UserValidator validator;

    @Test
    public void test_null() {
        this.expectLocale();
        this.expectMessage(Messages.Application.RequiredFieldMissing);
        this.expectFieldVar("settings.user.header");

        Set<Message> result = this.validator.validate(null);
        this.assertMessage(Messages.Application.RequiredFieldMissing, result);
    }

    @Test
    public void test_missingValues() {
        this.expectLocale();
        this.expectMessage(Messages.Application.RequiredFieldMissing);
        this.expectFieldVar("settings.user.username");
        this.expectFieldVar("settings.user.password");
        this.expectFieldVar("settings.user.confirm");

        User user = new User();
        Set<Message> result = this.validator.validate(user);
        this.assertMessages(Arrays.asList( //
                Messages.Application.RequiredFieldMissing, //
                Messages.Application.RequiredFieldMissing, //
                Messages.Application.RequiredFieldMissing), result);
    }

    @Test
    public void test_mismatch() {
        this.expectLocale();
        this.expectMessage(Messages.Application.PasswordMismatch);

        User user = new User();
        user.setUsername("junit");
        user.setPassword("pass");
        user.setConfirm("word");

        Set<Message> result = this.validator.validate(user);
        this.assertMessage(Messages.Application.PasswordMismatch, result);
    }

    @Test
    public void test_valid() {
        User user = new User();
        user.setUsername("junit");
        user.setPassword("pass");
        user.setConfirm("pass");

        Set<Message> result = this.validator.validate(user);
        this.assertValid(result);
    }
}

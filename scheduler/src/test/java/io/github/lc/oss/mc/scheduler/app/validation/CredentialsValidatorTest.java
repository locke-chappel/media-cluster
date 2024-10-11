package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.scheduler.app.model.Credentials;

public class CredentialsValidatorTest extends AbstractValidatorTest {
    @InjectMocks
    private CredentialsValidator validator;

    @Test
    public void test_validate_null() {
        this.expectLocale();
        this.expectMessage(Messages.Authentication.InvalidCredentials);

        Set<Message> result = this.validator.validate(null);
        this.assertMessage(Messages.Authentication.InvalidCredentials, result);
    }

    @Test
    public void test_validate_nulls() {
        this.expectLocale();
        this.expectMessage(Messages.Authentication.InvalidCredentials);

        Set<Message> result = this.validator.validate(new Credentials(null, null));
        this.assertMessage(Messages.Authentication.InvalidCredentials, result);
    }

    @Test
    public void test_validate_empty() {
        this.expectLocale();
        this.expectMessage(Messages.Authentication.InvalidCredentials);

        Set<Message> result = this.validator.validate(new Credentials("", ""));
        this.assertMessage(Messages.Authentication.InvalidCredentials, result);
    }

    @Test
    public void test_validate_bad_username_v1() {
        this.expectLocale();
        this.expectMessage(Messages.Authentication.InvalidCredentials);

        Set<Message> result = this.validator.validate(new Credentials("%", "%"));
        this.assertMessage(Messages.Authentication.InvalidCredentials, result);
    }

    @Test
    public void test_validate_bad_username_v2() {
        this.expectLocale();
        this.expectMessage(Messages.Authentication.InvalidCredentials);

        String name = "username";
        for (int i = 0; i < 16; i++) {
            name += name;
        }
        name += "1";

        Set<Message> result = this.validator.validate(new Credentials(name, "password"));
        this.assertMessage(Messages.Authentication.InvalidCredentials, result);
    }

    @Test
    public void test_validate_bad_password_v1() {
        this.expectLocale();
        this.expectMessage(Messages.Authentication.InvalidCredentials);

        Set<Message> result = this.validator.validate(new Credentials("user", ""));
        this.assertMessage(Messages.Authentication.InvalidCredentials, result);
    }

    @Test
    public void test_validate_bad_password_v2() {
        this.expectLocale();
        this.expectMessage(Messages.Authentication.InvalidCredentials);

        String password = "abcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < 10; i++) {
            password += password;
        }
        Set<Message> result = this.validator.validate(new Credentials("user", password));
        this.assertMessage(Messages.Authentication.InvalidCredentials, result);
    }

    @Test
    public void test_validate_valid() {
        Set<Message> result = this.validator.validate(new Credentials("user", "password"));
        this.assertValid(result);
    }
}

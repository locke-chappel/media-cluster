package io.github.lc.oss.mc.validation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.l10n.UserLocale;
import io.github.lc.oss.commons.l10n.Variable;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.AbstractMockTest;
import io.github.lc.oss.mc.api.Messages;

public class AbstractValidatorTest extends AbstractMockTest {
    public static class TestValidator extends AbstractValidator<String> {
        @Override
        public Set<Message> validate(String instance) {
            return null;
        }
    }

    @Mock
    private L10N l10n;
    @Mock
    private UserLocale userLocale;

    private AbstractValidator<String> validator;

    @BeforeEach
    public void init() {
        this.validator = new TestValidator();

        this.setField("l10n", this.l10n, this.validator);
        this.setField("userLocale", this.userLocale, this.validator);
    }

    @Test
    public void test_getFieldVar_null() {
        Variable result = this.validator.getFieldVar(null);
        Assertions.assertEquals("Field=", result.toString());
    }

    @Test
    public void test_validateString_blanks_notRequired() {
        Set<Message> result = this.validator.validateString("field", Patterns.Name, false, null);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());

        result = this.validator.validateString("field", Patterns.Name, false, "");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());

        result = this.validator.validateString("field", Patterns.Name, false, " \t \r \n \t ");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_validateString_blanks_required() {
        Mockito.when(this.userLocale.getLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(this.l10n.getText(Locale.ENGLISH, "field")).thenReturn("field-value");
        Mockito.when(this.l10n.getText(ArgumentMatchers.eq(Locale.ENGLISH),
                ArgumentMatchers.eq("messages.Application.Error.2"), ArgumentMatchers.notNull()))
                .thenReturn("Error field-value");

        Set<Message> result = this.validator.validateString("field", Patterns.Name, true, null);
        this.assertMessage(result, Messages.Application.RequiredFieldMissing);

        result = this.validator.validateString("field", Patterns.Name, true, "");
        this.assertMessage(result, Messages.Application.RequiredFieldMissing);

        result = this.validator.validateString("field", Patterns.Name, true, " \t \r \n \t ");
        this.assertMessage(result, Messages.Application.RequiredFieldMissing);
    }

    @Test
    public void test_validateString_invalid() {
        Mockito.when(this.userLocale.getLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(this.l10n.getText(Locale.ENGLISH, "field")).thenReturn("field-value");
        Mockito.when(this.l10n.getText(ArgumentMatchers.eq(Locale.ENGLISH),
                ArgumentMatchers.eq("messages.Application.Error.3"), ArgumentMatchers.notNull()))
                .thenReturn("Error field-value");

        Set<Message> result = this.validator.validateString("field", Patterns.Name, true, "# a b ");
        this.assertMessage(result, Messages.Application.InvalidField);
    }

    @Test
    public void test_validateString_valid() {
        Set<Message> messages = new HashSet<>();

        Assertions.assertTrue(messages.isEmpty());

        this.validator.validateString(messages, "field", Patterns.Name, true, "a");

        Assertions.assertTrue(messages.isEmpty());
    }

    private void assertMessage(Collection<Message> messages, Message message) {
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message actual = messages.iterator().next();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(message.getCategory(), actual.getCategory());
        Assertions.assertEquals(message.getSeverity(), actual.getSeverity());
        Assertions.assertEquals(message.getNumber(), actual.getNumber());
        Assertions.assertEquals("Error field-value", actual.getText());
    }
}

package io.github.lc.oss.mc.validation;

import java.util.Collection;
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
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.AbstractMockTest;
import io.github.lc.oss.mc.api.Messages;

public class CommandValidatorTest extends AbstractMockTest {
    @Mock
    private L10N l10n;
    @Mock
    private UserLocale userLocale;

    private CommandValidator validator;

    @BeforeEach
    public void init() {
        this.validator = new CommandValidator();

        this.setField("l10n", this.l10n, this.validator);
        this.setField("userLocale", this.userLocale, this.validator);
    }

    @Test
    public void test_validate_blanks() {
        Set<Message> result = this.validator.validate(null);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());

        result = this.validator.validate("");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());

        result = this.validator.validate(" \t \r \n \t ");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_validate_invalid() {
        Mockito.when(this.userLocale.getLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(this.l10n.getText(Locale.ENGLISH, "command")).thenReturn("command-value");
        Mockito.when(this.l10n.getText(ArgumentMatchers.eq(Locale.ENGLISH),
                ArgumentMatchers.eq("messages.Application.Error.3"), ArgumentMatchers.notNull()))
                .thenReturn("Error command-value");

        Set<Message> result = this.validator.validate("..");
        this.assertMessage(result, Messages.Application.InvalidField);

        result = this.validator.validate(";");
        this.assertMessage(result, Messages.Application.InvalidField);

        result = this.validator.validate("&");
        this.assertMessage(result, Messages.Application.InvalidField);

        result = this.validator.validate("|");
        this.assertMessage(result, Messages.Application.InvalidField);
    }

    @Test
    public void test_validate_valid() {
        Set<Message> result = this.validator.validate("ffmpeg -i path/to/file");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    private void assertMessage(Collection<Message> messages, Message message) {
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message actual = messages.iterator().next();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(message.getCategory(), actual.getCategory());
        Assertions.assertEquals(message.getSeverity(), actual.getSeverity());
        Assertions.assertEquals(message.getNumber(), actual.getNumber());
        Assertions.assertEquals("Error command-value", actual.getText());
    }
}

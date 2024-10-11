package io.github.lc.oss.mc.service;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.l10n.UserLocale;
import io.github.lc.oss.commons.l10n.Variable;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.AbstractMockTest;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;

public class AbstractServiceTest extends AbstractMockTest {
    private static class TestService extends AbstractService {

    }

    @Mock
    private L10N l10n;
    @Mock
    private UserLocale userLocale;

    private AbstractService service;

    @BeforeEach
    public void init() {
        this.service = new TestService();

        this.setField("l10n", this.l10n, this.service);
        this.setField("userLocale", this.userLocale, this.service);
    }

    @Test
    public void test_coverageFiller() {
        Assertions.assertNotNull(this.service.getLogger());
    }

    @Test
    public void test_getFieldVar_null() {
        Variable result = this.service.getFieldVar(null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("%Field%", result.getKeyId());
        Assertions.assertEquals("Field", result.getKey());
        Assertions.assertEquals("", result.getValue());
    }

    @Test
    public void test_getFieldVar() {
        Mockito.when(this.userLocale.getLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(this.l10n.getText(Locale.ENGLISH, "field")).thenReturn("field-value");

        Variable result = this.service.getFieldVar("field");
        Assertions.assertNotNull(result);
        Assertions.assertEquals("%Field%", result.getKeyId());
        Assertions.assertEquals("Field", result.getKey());
        Assertions.assertEquals("field-value", result.getValue());
    }

    @Test
    public void test_addMessage() {
        ServiceResponse<?> response = new ServiceResponse<>();

        Assertions.assertNull(response.getMessages());
        Assertions.assertFalse(response.hasMessages(Messages.Application.FailedToStartJob));

        this.service.addMessage(response, Messages.Application.FailedToStartJob);

        Assertions.assertNotNull(response.getMessages());
        Assertions.assertEquals(1, response.getMessages().size());
        Assertions.assertTrue(response.hasMessages(Messages.Application.FailedToStartJob));

        this.service.addMessage(response, Messages.Application.FailedToStartJob);

        Assertions.assertNotNull(response.getMessages());
        Assertions.assertEquals(2, response.getMessages().size());
        Assertions.assertTrue(response.hasMessages(Messages.Application.FailedToStartJob));
    }

    @Test
    public void test_toMessage() {
        Set<Message> result = this.service.toMessages(Messages.Authentication.InvalidSignature);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Message m = result.iterator().next();
        Assertions.assertNotEquals(m, Messages.Authentication.InvalidSignature);
        Assertions.assertTrue(Messages.Authentication.InvalidSignature.isSame(m));
        Assertions.assertFalse(result.contains(Messages.Authentication.InvalidSignature));
    }

    @Test
    public void test_toMessage_empty() {
        Set<Message> result = this.service.toMessages(new Message[0]);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_toMessage_multiple() {
        Set<Message> result = this.service.toMessages(Messages.Authentication.InvalidCredentials,
                Messages.Application.ImportingJobs);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        boolean foundInvalidCreds = false;
        boolean foundImporting = false;
        for (Message m : result) {
            if (Messages.Authentication.InvalidCredentials.isSame(m)) {
                Assertions.assertFalse(foundInvalidCreds);
                foundInvalidCreds = true;
            } else if (Messages.Application.ImportingJobs.isSame(m)) {
                Assertions.assertFalse(foundImporting);
                foundImporting = true;
            } else {
                Assertions.fail("Unexpected message");
            }
        }
        Assertions.assertTrue(foundInvalidCreds);
        Assertions.assertTrue(foundImporting);
    }
}

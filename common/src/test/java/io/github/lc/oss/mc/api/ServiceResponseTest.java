package io.github.lc.oss.mc.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.AbstractMockTest;

public class ServiceResponseTest extends AbstractMockTest {
    @Test
    public void test_getset() {
        ServiceResponse<Entity> sr = new ServiceResponse<>();

        Assertions.assertNull(sr.getEntity());
        Assertions.assertNull(sr.getMessages());
        Assertions.assertFalse(sr.hasMessages());

        final Entity e = new Entity();
        sr.setEntity(e);

        final Set<Message> messages = new HashSet<>();
        sr.setMessages(messages);

        Assertions.assertSame(e, sr.getEntity());
        Assertions.assertSame(messages, sr.getMessages());
        Assertions.assertFalse(sr.hasMessages());

        sr.setMessages(null);

        Assertions.assertNull(sr.getMessages());
        Assertions.assertFalse(sr.hasMessages());

        sr.addMessages(Messages.Application.UnhandledError);
        Assertions.assertNotNull(sr.getMessages());
        Assertions.assertNotEquals(messages, sr.getMessages());
        Assertions.assertTrue(sr.hasMessages());
        Assertions.assertTrue(sr.hasMessages(Messages.Application.UnhandledError));
        Assertions.assertFalse(sr.hasMessages(Messages.Application.ChangesSaved));

        sr.addMessages(Arrays.asList(Messages.Application.ChangesSaved, Messages.Authentication.ExpiredRequest));
        Assertions.assertTrue(sr.hasMessages());
        Assertions.assertTrue(sr.hasMessages(Messages.Application.UnhandledError));
        Assertions.assertTrue(sr.hasMessages(Messages.Application.ChangesSaved));
        Assertions.assertTrue(sr.hasMessages(Messages.Authentication.ExpiredRequest));
        Assertions.assertTrue(sr.hasMessages(Messages.Application.UnhandledError, Messages.Application.ChangesSaved));

        // Test hasMessages "any" behavior (method returns true if any match vs only
        // true if all match)
        Assertions.assertTrue(sr.hasMessages(Messages.Application.UnhandledError, Messages.Application.ChangesSaved,
                Messages.Application.DuplicateNodeName));
    }
}

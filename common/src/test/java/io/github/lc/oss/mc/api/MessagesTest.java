package io.github.lc.oss.mc.api;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.serialization.Message.Category;
import io.github.lc.oss.mc.AbstractMockTest;
import io.github.lc.oss.mc.api.Messages.Application;
import io.github.lc.oss.mc.api.Messages.Authentication;
import io.github.lc.oss.mc.api.Messages.Categories;

public class MessagesTest extends AbstractMockTest {
    @Test
    public void test_messages_api() {
        Set<Message> all = new HashSet<>();
        all.addAll(Messages.Application.all());
        all.addAll(Messages.Authentication.all());

        for (Message m : all) {
            Assertions.assertNotNull(m.getCategory());
            Assertions.assertNotNull(m.getSeverity());
            Assertions.assertTrue(m.getNumber() >= 0);
        }
    }

    @Test
    public void test_categories_caching() {
        Set<Category> all = new HashSet<>(Categories.all());
        for (Categories c : Categories.values()) {
            Assertions.assertTrue(all.remove(c));
            Assertions.assertTrue(Categories.hasName(c.name()));
            Assertions.assertSame(c, Categories.byName(c.name()));
            Assertions.assertSame(c, Categories.tryParse(c.name()));
        }
        Assertions.assertTrue(all.isEmpty());
    }

    @Test
    public void test_application_caching() {
        Set<Application> all = new HashSet<>(Application.all());
        for (Application c : Application.values()) {
            Assertions.assertTrue(all.remove(c));
            Assertions.assertTrue(Application.hasName(c.name()));
            Assertions.assertSame(c, Application.byName(c.name()));
            Assertions.assertSame(c, Application.tryParse(c.name()));
        }
        Assertions.assertTrue(all.isEmpty());
    }

    @Test
    public void test_authentication_caching() {
        Set<Authentication> all = new HashSet<>(Authentication.all());
        for (Authentication c : Authentication.values()) {
            Assertions.assertTrue(all.remove(c));
            Assertions.assertTrue(Authentication.hasName(c.name()));
            Assertions.assertSame(c, Authentication.byName(c.name()));
            Assertions.assertSame(c, Authentication.tryParse(c.name()));
        }
        Assertions.assertTrue(all.isEmpty());
    }

    @Test
    public void test_jsonConstructor() {
        Messages m = new Messages( //
                Messages.Categories.Application.name(), //
                Messages.Severities.Error.name(), //
                1, //
                "Text");

        Assertions.assertNotNull(m);
        Assertions.assertEquals(Messages.Categories.Application, m.getCategory());
        Assertions.assertEquals(Messages.Severities.Error, m.getSeverity());
        Assertions.assertEquals(1, m.getNumber());
        Assertions.assertEquals("Text", m.getText());
    }
}

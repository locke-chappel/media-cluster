package io.github.lc.oss.mc.api;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EntityTest extends AbstractApiEntityTest {
    @Test
    public void test_defaultConstructor() {
        Entity e = new Entity();

        Assertions.assertNull(e.getId());
        Assertions.assertNull(e.getModified());

        e.setId("id");
        e.setModified(new Date());

        Assertions.assertEquals("id", e.getId());
        Assertions.assertNotNull(e.getModified());
    }

    @Test
    public void test_idConstructor() {
        Entity e = new Entity("id");

        Assertions.assertEquals("id", e.getId());
        Assertions.assertNull(e.getModified());
    }

    @Test
    public void test_entityConstructor() {
        AbstractEntity base = new TestEntity();

        Entity e = new Entity(base);

        Assertions.assertEquals(base.getId(), e.getId());
        Assertions.assertSame(base.getModified(), e.getModified());
    }
}

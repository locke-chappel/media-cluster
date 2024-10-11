package io.github.lc.oss.mc.scheduler.app.entity;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.scheduler.AbstractMockTest;

public class EntityTest extends AbstractMockTest {
    @Test
    public void test_fields() {
        AbstractEntity e = new AbstractEntity() {
        };

        final Date created = e.getCreated();

        Assertions.assertNull(e.getId());
        Assertions.assertNull(e.getCreatedBy());
        Assertions.assertNotNull(created);
        Assertions.assertNull(e.getModified());
        Assertions.assertNull(e.getModifiedBy());
    }

    @Test
    public void test_isSame() {
        AbstractEntity e1 = new AbstractEntity() {
        };

        AbstractEntity e2 = new AbstractEntity() {
        };

        this.setField("id", "id-1", e1);
        this.setField("id", "id-2", e2);

        Assertions.assertTrue(e1.isSame(e1));
        Assertions.assertTrue(e2.isSame(e2));

        Assertions.assertFalse(e1.isSame(e2));
        Assertions.assertFalse(e2.isSame(e1));
        Assertions.assertFalse(e1.isSame(null));
        Assertions.assertFalse(e2.isSame(null));
    }
}

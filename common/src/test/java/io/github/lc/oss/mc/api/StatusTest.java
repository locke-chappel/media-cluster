package io.github.lc.oss.mc.api;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.AbstractMockEnumTest;

public class StatusTest extends AbstractMockEnumTest {
    @Override
    protected Class<? extends Enum<?>> getEnum() {
        return Status.class;
    }

    @Test
    public void test_caching() {
        Set<Status> all = new HashSet<>(Status.all());
        for (Status s : Status.values()) {
            Assertions.assertTrue(all.remove(s));
            Assertions.assertTrue(Status.hasName(s.name()));
            Assertions.assertSame(s, Status.byName(s.name()));
            Assertions.assertSame(s, Status.tryParse(s.name()));
        }
        Assertions.assertTrue(all.isEmpty());
    }
}

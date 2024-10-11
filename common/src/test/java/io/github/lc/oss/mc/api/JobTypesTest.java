package io.github.lc.oss.mc.api;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.AbstractMockEnumTest;

public class JobTypesTest extends AbstractMockEnumTest {
    @Override
    protected Class<? extends Enum<?>> getEnum() {
        return JobTypes.class;
    }

    @Test
    public void test_caching() {
        Set<JobTypes> all = new HashSet<>(JobTypes.all());

        for (JobTypes s : JobTypes.values()) {
            Assertions.assertTrue(all.remove(s));
            Assertions.assertTrue(JobTypes.hasName(s.name()));
            Assertions.assertSame(s, JobTypes.byName(s.name()));
            Assertions.assertSame(s, JobTypes.tryParse(s.name()));
        }
        Assertions.assertTrue(all.isEmpty());
    }
}

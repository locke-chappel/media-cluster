package io.github.lc.oss.mc.worker.app.validation;

import java.util.Collection;

import org.junit.jupiter.api.Assertions;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.worker.AbstractMockTest;

public abstract class AbstractValidatorTest extends AbstractMockTest {
    protected void assertValid(Collection<Message> actual) {
        Assertions.assertNotNull(actual);
        Assertions.assertTrue(actual.isEmpty());
    }
}

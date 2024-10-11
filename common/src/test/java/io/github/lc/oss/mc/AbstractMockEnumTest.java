package io.github.lc.oss.mc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.entity.Constants;

public abstract class AbstractMockEnumTest extends AbstractMockTest {
    protected abstract Class<? extends Enum<?>> getEnum();

    @Test
    protected void test_lengths() {
        Enum<?>[] values = this.getEnum().getEnumConstants();
        for (Enum<?> value : values) {
            Assertions.assertTrue(value.name().length() < Constants.Lengths.ENUM,
                    String.format("'%s.%s' exceeds maximum length of %d", value.getClass().getCanonicalName(),
                            value.name(), Constants.Lengths.ENUM));
        }
    }
}

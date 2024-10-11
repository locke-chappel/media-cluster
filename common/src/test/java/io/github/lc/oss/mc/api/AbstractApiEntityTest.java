package io.github.lc.oss.mc.api;

import java.util.Date;

import io.github.lc.oss.mc.AbstractMockTest;

public abstract class AbstractApiEntityTest extends AbstractMockTest {
    protected static class TestEntity implements AbstractEntity {
        private Date modified = new Date();

        @Override
        public String getModifiedBy() {
            return "junit";
        }

        @Override
        public Date getModified() {
            return this.modified;
        }

        @Override
        public String getId() {
            return "id";
        }
    }
}

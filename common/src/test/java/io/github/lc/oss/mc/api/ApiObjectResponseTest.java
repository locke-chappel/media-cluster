package io.github.lc.oss.mc.api;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.AbstractMockTest;

public class ApiObjectResponseTest extends AbstractMockTest {
    @Test
    public void test_getset() {
        ApiObject body = new ApiObject() {
        };

        ApiObjectResponse<ApiObject> ar = new ApiObjectResponse<>();

        Assertions.assertNull(ar.getBody());
        Assertions.assertNull(ar.getMessages());

        ar.setBody(body);
        ar.setMessages(Arrays.asList(new Messages(Messages.Application.ChangesSaved)));

        Assertions.assertSame(body, ar.getBody());
        Assertions.assertNotNull(ar.getMessages());
    }
}

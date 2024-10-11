package io.github.lc.oss.mc.worker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.testing.web.JsonObject;
import io.github.lc.oss.mc.api.Status;

@SpringBootTest(classes = { IntegrationConfig.class })
@Rollback
@Tag("restTest")
@ActiveProfiles("integrationtest")
public abstract class AbstractRestTest extends io.github.lc.oss.commons.testing.web.AbstractRestTest {
    static final int TEST_JOB_RUNTIME = 10 * 1000; // See test scripts in resources, must match sleep value

    @Autowired
    private Factory factory;

    @BeforeEach
    public void init() {
        this.waitForJobToComplete();
    }

    protected void assertJsonMessage(JsonObject object, Message message, String text) {
        this.assertJsonMessage(object, message.getCategory().name(), message.getSeverity().name(), message.getNumber(),
                text);
    }

    protected void assertJsonMessage(JsonObject object, Message... messages) {
        for (Message m : messages) {
            this.assertJsonMessage(object, m.getCategory().name(), m.getSeverity().name(), m.getNumber());
        }
    }

    protected void waitForJobToComplete() {
        this.waitForStatus(Status.Available, TEST_JOB_RUNTIME + 2000);
    }

    protected void waitForStatus(Status expected, int maxWait) {
        this.waitUntil(() -> {
            ResponseEntity<JsonObject> result = this.getJson("/");
            Assertions.assertNotNull(result);
            Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
            JsonObject body = this.assertJsonObject(result.getBody(), "body");
            return expected.name().equals(body.get("status"));
        }, maxWait, 250);
    }

    protected Factory getFactory() {
        return this.factory;
    }
}

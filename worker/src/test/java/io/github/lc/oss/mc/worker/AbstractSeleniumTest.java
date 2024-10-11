package io.github.lc.oss.mc.worker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import io.github.lc.oss.commons.serialization.Message.Severities;
import io.github.lc.oss.commons.testing.web.JsonObject;
import io.github.lc.oss.mc.api.Status;

@SpringBootTest(classes = { IntegrationConfig.class })
@Rollback
@Tag("seleniumTest")
@ActiveProfiles("integrationtest")
public abstract class AbstractSeleniumTest extends io.github.lc.oss.commons.testing.web.AbstractSeleniumTest {
    @Autowired
    private Factory factory;

    protected WebElement findByCssSelector(WebElement root, String selector) {
        return this.waitUntil(ExpectedConditions.elementToBeClickable(root.findElement(By.cssSelector(selector))));
    }

    protected void assertMessage(Severities severity, String text) {
        this.assertMessage(severity.name().toLowerCase(), text);
    }

    protected void waitForJobToComplete() {
        this.waitForStatus(Status.Available, AbstractRestTest.TEST_JOB_RUNTIME + 2000);
    }

    protected void waitForStatus(Status expected, int maxWait) {
        this.waitUntil(() -> {
            ResponseEntity<JsonObject> result = this.getRestService().getJson(this.getUrl("/"));
            Assertions.assertNotNull(result);
            Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
            JsonObject body = this.assertJsonObject(result.getBody(), "body");
            return expected.name().equals(body.get("status"));
        }, maxWait, 250);
    }

    private JsonObject assertJsonObject(JsonObject parent, String id) {
        Assertions.assertNotNull(parent, "Parent cannot be null");
        JsonObject child = parent.getChild(id);
        Assertions.assertNotNull(child);
        return child;
    }

    protected Factory getFactory() {
        return this.factory;
    }
}

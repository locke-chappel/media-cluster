package io.github.lc.oss.mc.worker.app.controllers.v1;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.springframework.http.HttpStatus;

import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.worker.AbstractSeleniumTest;

public class PublicControllerUiIT extends AbstractSeleniumTest {
    @Test
    public void test_ui() {
        // --- open app
        this.navigate("/");

        // --- assert node status
        WebElement e = this.findByCssSelector("#nodeStatus > div.header");
        this.assertTextContent(e, "Node Information");
        List<WebElement> es = this.findAllByCssSelector("#nodeStatus div.info");
        Assertions.assertEquals(2, es.size());

        this.assertInfo(es.get(0), "Cluster Name", "junit");
        this.assertInfo(es.get(1), "Node Name", "JUnit Worker");

        // --- assert job status (none)
        es = this.findAllByCssSelector("#jobStatus div.noJob");
        Assertions.assertEquals(1, es.size());

        this.assertTextContent(es.get(0), "This node is ready to accept a job");

        // --- create job
        Job job = this.getFactory().job();
        SignedRequest request = this.getFactory().sign(job);
        this.getRestService().postJson(this.getUrl("/api/v1/jobs"), request, null, HttpStatus.ACCEPTED);

        // --- wait for job to start
        this.waitFor(250);

        // --- reload page
        this.navigate("/");

        // --- assert job status (processing)
        es = this.findAllByCssSelector("#jobStatus div.info");
        Assertions.assertEquals(3, es.size());

        this.assertInfo(es.get(0), "ID", job.getId());
        this.assertInfo(es.get(1), "Type", job.getType().name());
        this.assertInfo(es.get(2), "Source", job.getSource());

        // --- wait for job to finish (~10 seconds)
        this.waitForJobToComplete();

        // --- reload page
        this.navigate("/");

        // --- assert job status (none)
        es = this.findAllByCssSelector("#jobStatus div.noJob");
        Assertions.assertEquals(1, es.size());

        this.assertTextContent(es.get(0), "This node is ready to accept a job");

        // --- cause error
        this.navigate("/error");
        this.waitForNavigate("/error");
        this.assertTextContent("content", "Oops...something unexpected happened.");
    }

    private void assertInfo(WebElement info, String label, String value) {
        WebElement e = this.findByCssSelector(info, "span.label");
        this.assertTextContent(e, label);

        e = this.findByCssSelector(info, "span.value");
        this.assertTextContent(e, value);
    }
}

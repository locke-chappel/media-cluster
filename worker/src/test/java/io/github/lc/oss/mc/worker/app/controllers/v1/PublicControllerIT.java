package io.github.lc.oss.mc.worker.app.controllers.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.github.lc.oss.commons.testing.web.JsonObject;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.worker.AbstractRestTest;

public class PublicControllerIT extends AbstractRestTest {
    @Test
    public void test_securityHeaders() {
        ResponseEntity<String> result = this.call(HttpMethod.GET, "/", null, String.class, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        HttpHeaders headers = result.getHeaders();
        this.assertHeader("Content-Security-Policy", "default-src 'none'; script-src 'self'; " + //
                "connect-src 'self'; img-src 'self'; style-src 'self'; font-src 'self'; " + //
                "frame-ancestors 'none'; form-action 'none';", headers);
        this.assertHeader("X-Content-Type-Options", "nosniff", headers);
        this.assertHeader("X-Frame-Options", "deny", headers);
        this.assertHeader("X-XSS-Protection", "0", headers);
    }

    @Test
    public void test_jsonStatus_available() {
        ResponseEntity<JsonObject> result = this.getJson("/");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        JsonObject body = this.assertJsonObject(result.getBody(), "body");
        this.assertJsonProperty(body, "id", "junit-worker");
        this.assertJsonProperty(body, "clusterName", "junit");
        this.assertJsonProperty(body, "name", "JUnit Worker");
        this.assertJsonProperty(body, "status", Status.Available);
        this.assertJsonNull(body, "currentJob");
    }

    @Test
    public void test_jsonStatus_busy() {
        Job job = this.getFactory().job();
        SignedRequest request = this.getFactory().sign(job);
        this.postJson(this.getUrl("/api/v1/jobs"), request, null, HttpStatus.ACCEPTED);

        ResponseEntity<JsonObject> result = this.getJson("/");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        JsonObject body = this.assertJsonObject(result.getBody(), "body");
        this.assertJsonProperty(body, "id", "junit-worker");
        this.assertJsonProperty(body, "clusterName", "junit");
        this.assertJsonProperty(body, "name", "JUnit Worker");
        this.assertJsonProperty(body, "status", Status.InProgress);

        JsonObject currentJob = this.assertJsonObject(body, "currentJob");
        this.assertJsonNotNull(currentJob, "id");
        this.assertJsonProperty(currentJob, "index", 1);
        this.assertJsonProperty(currentJob, "source", "sleep.avi");
        this.assertJsonProperty(currentJob, "type", JobTypes.Video);

        this.waitForJobToComplete();
    }
}

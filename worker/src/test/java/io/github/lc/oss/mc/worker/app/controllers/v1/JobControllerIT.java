package io.github.lc.oss.mc.worker.app.controllers.v1;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import io.github.lc.oss.commons.testing.web.JsonObject;
import io.github.lc.oss.mc.api.ApiObject;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.worker.AbstractRestTest;

public class JobControllerIT extends AbstractRestTest {
    @Test
    public void test_jobs_invalidSignature() {
        Job job = this.getFactory().job();
        SignedRequest request = this.getFactory().sign(job);
        request.setSignature("junk");

        ResponseEntity<JsonObject> response = this.postJson(this.getUrl("/api/v1/jobs"), request, null,
                HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Authentication.InvalidSignature);
    }

    @Test
    public void test_jobs_invalidNode() {
        Job job = this.getFactory().job();
        SignedRequest request = this.getFactory().sign("junk", job);

        ResponseEntity<JsonObject> response = this.postJson(this.getUrl("/api/v1/jobs"), request, null,
                HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Application.InvalidNode);
    }

    @Test
    public void test_jobs_futureRequest() {
        Job job = this.getFactory().job();
        SignedRequest request = this.getFactory().sign(System.currentTimeMillis() + 1000 * 1000, job);

        ResponseEntity<JsonObject> response = this.postJson(this.getUrl("/api/v1/jobs"), request, null,
                HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Authentication.ExpiredRequest);
    }

    @Test
    public void test_jobs_expiredRequest() {
        Job job = this.getFactory().job();
        SignedRequest request = this.getFactory().sign(System.currentTimeMillis() - 1000 * 1000, job);

        ResponseEntity<JsonObject> response = this.postJson(this.getUrl("/api/v1/jobs"), request, null,
                HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Authentication.ExpiredRequest);
    }

    @Test
    public void test_jobs_invalid_null() {
        SignedRequest request = this.getFactory().sign((ApiObject) null);

        ResponseEntity<JsonObject> response = this.postJson(this.getUrl("/api/v1/jobs"), request, null,
                HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Application.RequiredFieldMissing);
    }

    @Test
    public void test_jobs_invalid_batchIndex() {
        Job job = this.getFactory().job();
        job.setBatchIndex(-1);
        SignedRequest request = this.getFactory().sign(job);

        ResponseEntity<JsonObject> response = this.postJson(this.getUrl("/api/v1/jobs"), request, null,
                HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Application.InvalidField);
    }

    @Test
    public void test_jobs_invalid_source_null() {
        Job job = this.getFactory().job();
        job.setSource(null);
        SignedRequest request = this.getFactory().sign(job);

        ResponseEntity<JsonObject> response = this.postJson(this.getUrl("/api/v1/jobs"), request, null,
                HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Application.RequiredFieldMissing);
    }

    @Test
    public void test_jobs_error() {
        Job job = this.getFactory().jobError();
        SignedRequest request = this.getFactory().sign(job);

        ResponseEntity<JsonObject> response = this.postJson(this.getUrl("/api/v1/jobs"), request, null,
                HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Application.RequiredFieldMissing);
    }

    @Test
    public void test_jobs() {
        // --- first is accepted
        Job job = this.getFactory().job();
        SignedRequest request = this.getFactory().sign(job);
        this.postJson(this.getUrl("/api/v1/jobs"), request, null, HttpStatus.ACCEPTED);

        // --- wait for job to start (but no more than 1 second)
        this.waitForStatus(Status.InProgress, 1000);

        // --- second should be busy
        job = this.getFactory().job();
        request = this.getFactory().sign(job);
        ResponseEntity<JsonObject> response = this.postJson(this.getUrl("/api/v1/jobs"), request, null,
                HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Application.AlreadyProcessingJob);

        // --- check that job is still running
        this.waitForStatus(Status.InProgress, 500);

        // --- wait for job to finish
        this.waitForJobToComplete();

        // --- third should be accepted since worker is idle again
        job = this.getFactory().job();
        job.setType(JobTypes.Audio);
        request = this.getFactory().sign(job);
        this.postJson(this.getUrl("/api/v1/jobs"), request, null, HttpStatus.ACCEPTED);

        // --- wait for job to finish
        this.waitForJobToComplete();
    }

    @Test
    public void test_abortJob_invalidSignature() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        SignedRequest request = this.getFactory().sign("abort");
        request.setSignature("junk");
        ResponseEntity<JsonObject> response = this.callJson(HttpMethod.DELETE, this.getUrl("/api/v1/jobs"), headers,
                request, HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Authentication.InvalidSignature);
    }

    @Test
    public void test_abortJob_invalidNode() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        SignedRequest request = this.getFactory().sign("junk", "abort");
        ResponseEntity<JsonObject> response = this.callJson(HttpMethod.DELETE, this.getUrl("/api/v1/jobs"), headers,
                request, HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Application.InvalidNode);
    }

    @Test
    public void test_abortJob_futureRequest() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        SignedRequest request = this.getFactory().sign(System.currentTimeMillis() + 1000 * 1000, "abort");
        ResponseEntity<JsonObject> response = this.callJson(HttpMethod.DELETE, this.getUrl("/api/v1/jobs"), headers,
                request, HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Authentication.ExpiredRequest);
    }

    @Test
    public void test_abortJob_expiredRequest() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        SignedRequest request = this.getFactory().sign(System.currentTimeMillis() - 1000 * 1000, "abort");
        ResponseEntity<JsonObject> response = this.callJson(HttpMethod.DELETE, this.getUrl("/api/v1/jobs"), headers,
                request, HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Authentication.ExpiredRequest);
    }

    @Test
    public void test_abortJob_invalid_null() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        SignedRequest request = this.getFactory().sign((String) null);
        ResponseEntity<JsonObject> response = this.callJson(HttpMethod.DELETE, this.getUrl("/api/v1/jobs"), headers,
                request, HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Application.InvalidField);
    }

    @Test
    public void test_abortJob() {
        // --- start a job
        Job job = this.getFactory().job();
        SignedRequest request = this.getFactory().sign(job);
        this.postJson(this.getUrl("/api/v1/jobs"), request, null, HttpStatus.ACCEPTED);

        // --- wait for job to start (but no more than 1 second)
        this.waitForStatus(Status.InProgress, 1000);

        // --- abort job (bad message, rejected)
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        request = this.getFactory().sign("junk");
        ResponseEntity<JsonObject> response = this.callJson(HttpMethod.DELETE, this.getUrl("/api/v1/jobs"), headers,
                request, HttpStatus.UNPROCESSABLE_ENTITY);
        this.assertJsonMessage(response.getBody(), Messages.Application.InvalidField);

        // --- check that job is still running
        this.waitForStatus(Status.InProgress, 500);

        // --- abort job (before it finishes)
        headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        request = this.getFactory().sign("abort");
        this.callJson(HttpMethod.DELETE, this.getUrl("/api/v1/jobs"), headers, request, HttpStatus.ACCEPTED);

        // --- wait for the abort command to complete (but no more than 2 seconds)
        this.waitForStatus(Status.Available, 2000);

        // -- abort again - in effect a no-op
        headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        request = this.getFactory().sign("abort");
        this.callJson(HttpMethod.DELETE, this.getUrl("/api/v1/jobs"), headers, request, HttpStatus.ACCEPTED);

        // --- check that worker is still available
        this.waitForStatus(Status.Available, 500);
    }
}

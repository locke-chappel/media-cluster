package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.github.lc.oss.commons.testing.web.JsonObject;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.scheduler.AbstractRestTest;
import io.github.lc.oss.mc.scheduler.app.model.Credentials;

public class PublicControllerIT extends AbstractRestTest {
    @Test
    public void test_securityHeaders() {
        ResponseEntity<String> result = this.call(HttpMethod.GET, "/", null, String.class, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.FOUND, result.getStatusCode());
        HttpHeaders headers = result.getHeaders();
        this.assertHeader("Content-Security-Policy", "default-src 'none'; script-src 'self'; " + //
                "connect-src 'self'; img-src 'self'; style-src 'self'; font-src 'self'; " + //
                "frame-ancestors 'none'; form-action 'none';", headers);
        this.assertHeader("X-Content-Type-Options", "nosniff", headers);
        this.assertHeader("X-Frame-Options", "deny", headers);
        this.assertHeader("X-XSS-Protection", "0", headers);

        result = this.call(HttpMethod.GET, "/login", null, String.class, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        headers = result.getHeaders();
        this.assertHeader("Content-Security-Policy", "default-src 'none'; script-src 'self'; " + //
                "connect-src 'self'; img-src 'self'; style-src 'self'; font-src 'self'; " + //
                "frame-ancestors 'none'; form-action 'none';", headers);
        this.assertHeader("X-Content-Type-Options", "nosniff", headers);
        this.assertHeader("X-Frame-Options", "deny", headers);
        this.assertHeader("X-XSS-Protection", "0", headers);
    }

    @Test
    public void test_login_errors_missingCredentials() {
        // missing record
        ResponseEntity<JsonObject> result = this.postJson("/api/v1/login", null, null, HttpStatus.UNPROCESSABLE_ENTITY);
        Assertions.assertNotNull(result);
        JsonObject body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Authentication.InvalidCredentials);

        // no user or pass
        Credentials creds = new Credentials(null, null);
        result = this.postJson("/api/v1/login", creds, null, HttpStatus.UNPROCESSABLE_ENTITY);
        Assertions.assertNotNull(result);
        body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Authentication.InvalidCredentials);

        // user but no pass
        creds = new Credentials("junk", null);
        result = this.postJson("/api/v1/login", creds, null, HttpStatus.UNPROCESSABLE_ENTITY);
        Assertions.assertNotNull(result);
        body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Authentication.InvalidCredentials);

        // no user but pass
        creds = new Credentials(null, "password");
        result = this.postJson("/api/v1/login", creds, null, HttpStatus.UNPROCESSABLE_ENTITY);
        Assertions.assertNotNull(result);
        body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Authentication.InvalidCredentials);
    }

    @Test
    public void test_login_errors_userNotFound() {
        Credentials creds = new Credentials("junk", "password");
        ResponseEntity<JsonObject> result = this.postJson("/api/v1/login", creds, null,
                HttpStatus.UNPROCESSABLE_ENTITY);
        Assertions.assertNotNull(result);
        JsonObject body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Authentication.InvalidCredentials);
    }

    @Test
    public void test_login_errors_wrongPassword() {
        Credentials creds = new Credentials(this.getDefaultUsername(), this.getDefaultUserPassword() + "junk");
        ResponseEntity<JsonObject> result = this.postJson("/api/v1/login", creds, null,
                HttpStatus.UNPROCESSABLE_ENTITY);
        Assertions.assertNotNull(result);
        JsonObject body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Authentication.InvalidCredentials);
    }
}

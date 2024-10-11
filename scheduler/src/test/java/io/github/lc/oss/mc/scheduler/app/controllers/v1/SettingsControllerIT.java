package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.testing.web.JsonObject;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.scheduler.AbstractRestTest;
import io.github.lc.oss.mc.scheduler.app.model.EncryptedBackup;

public class SettingsControllerIT extends AbstractRestTest {
    @Test
    public void test_downloadBackup_passwordIssues() {
        EncryptedBackup request = new EncryptedBackup();
        request.setPassword(null);

        Map<String, String> headers = this.userToken();

        ResponseEntity<JsonObject> result = this.postJson("/api/v1/backup", request, headers,
                HttpStatus.UNPROCESSABLE_ENTITY);
        JsonObject body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Application.InvalidField);
        this.assertJsonNull(body, "data");

        request = new EncryptedBackup();
        request.setPassword("".toCharArray());

        result = this.postJson("/api/v1/backup", request, headers, HttpStatus.UNPROCESSABLE_ENTITY);
        body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Application.InvalidField);
        this.assertJsonNull(body, "data");

        request = new EncryptedBackup();
        request.setPassword(" \t \r \n \t ".toCharArray());

        result = this.postJson("/api/v1/backup", request, headers, HttpStatus.UNPROCESSABLE_ENTITY);
        body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Application.InvalidField);
        this.assertJsonNull(body, "data");
    }

    @Test
    public void test_restoreBackup_passwordIssues() {
        EncryptedBackup request = new EncryptedBackup();
        request.setPassword(null);

        Map<String, String> headers = this.userToken();

        ResponseEntity<JsonObject> result = this.putJson("/api/v1/backup", request, headers,
                HttpStatus.UNPROCESSABLE_ENTITY);
        JsonObject body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Application.ErrorDecryptingBackup);
        this.assertJsonNull(body, "data");

        request = new EncryptedBackup();
        request.setPassword("".toCharArray());

        result = this.putJson("/api/v1/backup", request, headers, HttpStatus.UNPROCESSABLE_ENTITY);
        body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Application.ErrorDecryptingBackup);
        this.assertJsonNull(body, "data");

        request = new EncryptedBackup();
        request.setPassword(" \t \r \n \t ".toCharArray());

        result = this.putJson("/api/v1/backup", request, headers, HttpStatus.UNPROCESSABLE_ENTITY);
        body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Application.ErrorDecryptingBackup);
        this.assertJsonNull(body, "data");
    }

    @Test
    public void test_backupAndRestore() {
        EncryptedBackup request = new EncryptedBackup();
        request.setPassword("pass".toCharArray());

        Map<String, String> headers = this.userToken();

        // --- Download Backup
        ResponseEntity<JsonObject> result = this.postJson("/api/v1/backup", request, headers, HttpStatus.OK);
        JsonObject body = this.assertJsonObject(result.getBody(), "body");
        this.assertJsonNull(body, "password");
        String data = this.assertJsonNotNull(body, "data");
        String[] parts = data.split("\\$");
        Assertions.assertEquals(3, parts.length);
        String decoded = Encodings.Base64.decodeString(parts[2]);
        // data is supposed to be encrypted
        Assertions.assertFalse(decoded.contains("SCRIPT"));

        // --- restore backup, wrong password
        request = new EncryptedBackup();
        request.setPassword("pass2".toCharArray());
        request.setData(data);

        result = this.putJson("/api/v1/backup", request, headers, HttpStatus.UNPROCESSABLE_ENTITY);
        body = result.getBody();
        Assertions.assertNotNull(body);
        this.assertJsonMessage(body, Messages.Application.ErrorDecryptingBackup);
        this.assertJsonNull(body, "data");

        // --- restore backup, correct password
        request.setPassword("pass".toCharArray());

        this.putJson("/api/v1/backup", request, headers, HttpStatus.NO_CONTENT);
    }
}

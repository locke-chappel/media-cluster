package io.github.lc.oss.mc.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.AbstractMockTest;

public class PatternsTest extends AbstractMockTest {
    @Test
    public void test_DisplayName() {
        Assertions.assertFalse(Patterns.DisplayName.matcher("").matches());
        Assertions.assertFalse(Patterns.DisplayName.matcher("\\").matches());
        Assertions.assertFalse(Patterns.DisplayName.matcher("/").matches());
        Assertions.assertFalse(Patterns.DisplayName.matcher("?").matches());
        Assertions.assertFalse(Patterns.DisplayName.matcher(":").matches());
        Assertions.assertTrue(Patterns.DisplayName
                .matcher("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZZ0123456789+=.,&~|_!@#$<>(){}-[] ")
                .matches());

        String s = this.pad("ABCDEF1234567890", 8);
        Assertions.assertTrue(Patterns.DisplayName.matcher(s).matches());
        Assertions.assertFalse(Patterns.DisplayName.matcher(s + "1").matches());
    }

    @Test
    public void test_ExternalId() {
        Assertions.assertFalse(Patterns.ExternalId.matcher("").matches());
        Assertions.assertFalse(Patterns.ExternalId.matcher("\\").matches());
        Assertions.assertFalse(Patterns.ExternalId.matcher("/").matches());
        Assertions.assertFalse(Patterns.ExternalId.matcher("!").matches());
        Assertions.assertFalse(Patterns.ExternalId.matcher("?").matches());
        Assertions.assertFalse(Patterns.ExternalId.matcher(":").matches());
        Assertions.assertTrue(Patterns.ExternalId.matcher("abcdefghijklmnopqrstuvwxyz").matches());
        Assertions.assertTrue(Patterns.ExternalId.matcher("ABCDEFGHIJKLMNOPQRSTUVWXYZ").matches());
        Assertions.assertTrue(Patterns.ExternalId.matcher("aB0123456789-._").matches());

        String s = this.pad("123456789", 4);
        Assertions.assertTrue(Patterns.ExternalId.matcher(s).matches());
        Assertions.assertFalse(Patterns.ExternalId.matcher(s + "a").matches());
    }

    @Test
    public void test_FilePath() {
        Assertions.assertFalse(Patterns.FilePath.matcher("").matches());
        Assertions.assertFalse(Patterns.FilePath.matcher("..").matches());
        Assertions.assertTrue(Patterns.FilePath.matcher("a/b/.c").matches());

        String s = this.pad("ABCDEF1234567890", 16);
        Assertions.assertTrue(Patterns.FilePath.matcher(s).matches());
        Assertions.assertFalse(Patterns.FilePath.matcher(s + "a").matches());
    }

    @Test
    public void test_FileExt() {
        Assertions.assertFalse(Patterns.FileExt.matcher("").matches());
        Assertions.assertFalse(Patterns.FileExt.matcher(".").matches());
        Assertions.assertFalse(Patterns.FileExt.matcher("..").matches());
        Assertions.assertTrue(Patterns.FileExt.matcher("abcdefghijklmnop").matches());
        Assertions.assertTrue(Patterns.FileExt.matcher("qrztuvwxyz").matches());
        Assertions.assertTrue(Patterns.FileExt.matcher("ABCDEFGHIJKLMNOP").matches());
        Assertions.assertTrue(Patterns.FileExt.matcher("QRSTUVWXYZ").matches());
        Assertions.assertTrue(Patterns.FileExt.matcher("aB0123456789-_").matches());

        String s = this.pad("ABCDEF1234567890", 1);
        Assertions.assertTrue(Patterns.FileExt.matcher(s).matches());
        Assertions.assertFalse(Patterns.FileExt.matcher(s + "a").matches());
    }

    @Test
    public void test_Name() {
        Assertions.assertFalse(Patterns.Name.matcher("").matches());
        Assertions.assertFalse(Patterns.Name.matcher("#").matches());
        Assertions.assertFalse(Patterns.Name.matcher("?").matches());
        Assertions.assertFalse(Patterns.Name.matcher(".").matches());
        Assertions.assertFalse(Patterns.Name.matcher("..").matches());
        Assertions.assertFalse(Patterns.Name.matcher("/").matches());
        Assertions.assertFalse(Patterns.Name.matcher("\\").matches());
        Assertions.assertFalse(Patterns.Name.matcher("/./a/../").matches());
        Assertions.assertFalse(Patterns.Name.matcher("|").matches());
        Assertions.assertFalse(Patterns.Name.matcher("'").matches());
        Assertions.assertFalse(Patterns.Name.matcher("\"").matches());
        Assertions.assertTrue(Patterns.Name
                .matcher("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZZ0123456789-_#@=+!~()[]{}:;&").matches());

        String s = this.pad("ABCDEF1234567890", 8);
        Assertions.assertTrue(Patterns.Name.matcher(s).matches());
        Assertions.assertFalse(Patterns.Name.matcher(s + "1").matches());
    }

    @Test
    public void test_SearchTerm() {
        Assertions.assertTrue(Patterns.SearchTerm.matcher("").matches());

        String s = this.pad("ABCDEF1234567890", 8);
        Assertions.assertTrue(Patterns.Name.matcher(s).matches());
        Assertions.assertFalse(Patterns.Name.matcher(s + "1").matches());
    }

    @Test
    public void test_Username() {
        Assertions.assertFalse(Patterns.Username.matcher("").matches());
        Assertions.assertFalse(Patterns.Username.matcher(" ").matches());
        Assertions.assertFalse(Patterns.Username.matcher("\\").matches());
        Assertions.assertFalse(Patterns.Username.matcher("/").matches());
        Assertions.assertFalse(Patterns.Username.matcher("?").matches());
        Assertions.assertFalse(Patterns.Username.matcher(":").matches());
        Assertions.assertTrue(Patterns.Username
                .matcher("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZZ0123456789+=.,&~|_!@#$<>(){}-[]")
                .matches());

        String s = this.pad("ABCDEF1234567890", 8);
        Assertions.assertTrue(Patterns.Username.matcher(s).matches());
        Assertions.assertFalse(Patterns.Username.matcher(s + "1").matches());
    }

    @Test
    public void test_Url() {
        Assertions.assertFalse(Patterns.Url.matcher("").matches());
        Assertions.assertFalse(Patterns.Url.matcher("ftp://asd").matches());

        // Opportunity for refining the regex...careful not to get too strict though
        Assertions.assertTrue(Patterns.Url.matcher("https://localhosthttp://").matches());
        Assertions.assertTrue(Patterns.Url.matcher("https://localhost?q=http://").matches());

        Assertions.assertTrue(Patterns.Url.matcher("http://localhost").matches());
        Assertions.assertTrue(Patterns.Url.matcher("https://example.com?q=v&v=t%20").matches());

        String s = this.pad("12345678", 63);
        Assertions.assertTrue(Patterns.Url.matcher("https://" + s).matches());
        Assertions.assertFalse(Patterns.Url.matcher("https://" + s + "1").matches());
    }

    private String pad(String sequence, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(sequence);
        }
        return sb.toString();
    }
}

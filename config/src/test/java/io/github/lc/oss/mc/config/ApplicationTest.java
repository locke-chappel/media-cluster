package io.github.lc.oss.mc.config;

import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.github.lc.oss.commons.util.IoTools;

public class ApplicationTest extends AbstractMockTest {
    private static class Counter {
        public int count = 0;
    }

    private static String tempDir = null;

    private InputStream systemIn;

    private String getTempDir() {
        if (ApplicationTest.tempDir == null) {
            String tempDir = System.getProperty("java.io.tmpdir").replace("\\", "/");
            if (!tempDir.endsWith("/")) {
                tempDir += "/";
            }
            ApplicationTest.tempDir = tempDir + "mc.config.test/";
        }
        return ApplicationTest.tempDir;
    }

    private String getKeyPath() {
        return this.getTempDir() + "encrypted-config-test.key";
    }

    private String getConfigPath() {
        return this.getTempDir() + "encrypted-config-test.cfg";
    }

    @BeforeEach
    public void setup() {
        new File(getTempDir()).mkdirs();

        this.systemIn = System.in;
        this.setField("stdin", null, UserPropmpt.class);
        this.setField("abort", false, Application.class);
        this.setField("console", null, Application.class);
    }

    @AfterEach
    public void cleanup() {
        System.setIn(this.systemIn);
        this.setField("stdin", null, UserPropmpt.class);
        this.setField("abort", false, Application.class);
        this.setField("console", null, Application.class);

        new File(this.getKeyPath()).delete();

        new File(this.getConfigPath()).delete();

        new File(this.getTempDir() + "key").delete();
        new File(this.getTempDir() + "config").delete();
        new File(this.getTempDir()).delete();
    }

    protected void writeStdIn(String data) {
        System.setIn(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void test_main_internalAuth() {
        this.writeStdIn("\n\n\npass\nchangeit\n\n" + this.getKeyPath() + "\n" + this.getConfigPath() + "\n");

        Application.main(null);

        File key = new File(this.getKeyPath());
        Assertions.assertTrue(key.exists());
        Assertions.assertEquals(1024, key.length());

        File config = new File(this.getConfigPath());
        /*
         * Config size is variable due to intentional variations in the encryption
         * algorithm
         */
        Assertions.assertTrue(config.exists());
        Assertions.assertTrue(config.length() > 256);
        Assertions.assertTrue(config.length() < 512);
    }

    @Test
    public void test_main_internalAuth_noPaths() {
        System.setProperty("user.dir", new File(this.getTempDir()).getAbsolutePath());
        String cwd = System.getProperty("user.dir");
        Assertions.assertNotNull(cwd);
        Assertions.assertFalse(cwd.endsWith("/"));

        this.writeStdIn("\n\n\npass\nchangeit\n\n\n\n");

        Application.main(null);

        File key = new File(this.getTempDir() + "key");
        Assertions.assertTrue(key.exists());
        Assertions.assertEquals(1024, key.length());

        File config = new File(this.getTempDir() + "config");
        /*
         * Config size is variable due to intentional variations in the encryption
         * algorithm
         */
        Assertions.assertTrue(config.exists());
        Assertions.assertTrue(config.length() > 256);
        Assertions.assertTrue(config.length() < 512);
    }

    @Test
    public void test_main_internalAuth_noPaths_cwdTailingSlash() {
        System.setProperty("user.dir", new File(this.getTempDir()).getAbsolutePath() + "/");
        String cwd = System.getProperty("user.dir");
        Assertions.assertNotNull(cwd);
        Assertions.assertTrue(cwd.endsWith("/"));

        this.writeStdIn("\n\n\npass\nchangeit\n\n\n\n");

        Application.main(null);

        File key = new File(this.getTempDir() + "key");
        Assertions.assertTrue(key.exists());
        Assertions.assertEquals(1024, key.length());

        File config = new File(this.getTempDir() + "config");
        /*
         * Config size is variable due to intentional variations in the encryption
         * algorithm
         */
        Assertions.assertTrue(config.exists());
        Assertions.assertTrue(config.length() > 256);
        Assertions.assertTrue(config.length() < 512);
    }

    @Test
    public void test_main_internalAuth_withConsole() {
        Counter counter = new Counter();

        List<String> answers = Arrays.asList("\n", "\n", "\n", "pass\n", "changeit\n", "\n", this.getKeyPath() + "\n",
                this.getConfigPath() + "\n");

        Console console = Mockito.mock(Console.class);
        this.setField("console", console, Application.class);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String response = answers.get(counter.count);
                counter.count++;
                return response;
            }
        }).when(console).readLine();

        Mockito.doAnswer(new Answer<char[]>() {
            @Override
            public char[] answer(InvocationOnMock invocation) throws Throwable {
                String response = answers.get(counter.count);
                counter.count++;
                return response.toCharArray();
            }
        }).when(console).readPassword();

        Assertions.assertEquals(0, counter.count);
        Application.main(null);
        Assertions.assertEquals(answers.size(), counter.count);

        File key = new File(this.getKeyPath());
        Assertions.assertTrue(key.exists());
        Assertions.assertEquals(1024, key.length());

        File config = new File(this.getConfigPath());
        /*
         * Config size is variable due to intentional variations in the encryption
         * algorithm
         */
        Assertions.assertTrue(config.exists());
        Assertions.assertTrue(config.length() > 256);
        Assertions.assertTrue(config.length() < 512);
    }

    @Test
    public void test_main_externalAuth() {
        this.writeStdIn(
                "y\njunit\npk\n\n\n\npass\nchangeit\n\n" + this.getKeyPath() + "\n" + this.getConfigPath() + "\n");

        Application.main(null);

        File key = new File(this.getKeyPath());
        Assertions.assertTrue(key.exists());
        Assertions.assertEquals(1024, key.length());

        File config = new File(this.getConfigPath());
        /*
         * Config size is variable due to intentional variations in the encryption
         * algorithm
         */
        Assertions.assertTrue(config.exists());
        Assertions.assertTrue(config.length() > 256);
        Assertions.assertTrue(config.length() < 512);
    }

    @Test
    public void test_main_internalAuth_overwrite_accept() {
        this.writeStdIn("\n\n\npass\nchangeit\n\n" + this.getKeyPath() + "\ny\n" + this.getConfigPath() + "\nyes\n");

        IoTools.writeToFile("junk".getBytes(StandardCharsets.UTF_8), this.getKeyPath());
        IoTools.writeToFile("junk".getBytes(StandardCharsets.UTF_8), this.getConfigPath());

        File key = new File(this.getKeyPath());
        Assertions.assertTrue(key.exists());
        Assertions.assertEquals(4, key.length());

        File config = new File(this.getConfigPath());
        Assertions.assertTrue(config.exists());
        Assertions.assertEquals(4, config.length());

        Application.main(null);

        key = new File(this.getKeyPath());
        Assertions.assertTrue(key.exists());
        Assertions.assertEquals(1024, key.length());

        config = new File(this.getConfigPath());
        Assertions.assertTrue(config.exists());
        /*
         * Config size is variable due to intentional variations in the encryption
         * algorithm
         */
        Assertions.assertTrue(config.length() > 256);
        Assertions.assertTrue(config.length() < 512);
    }

    @Test
    public void test_main_internalAuth_overwrite_decline() {
        this.writeStdIn("\n\n\npass\nchangeit\n\n" + this.getKeyPath() + "\nwhat ever\n");

        IoTools.writeToFile("junk".getBytes(StandardCharsets.UTF_8), this.getKeyPath());
        IoTools.writeToFile("junk".getBytes(StandardCharsets.UTF_8), this.getConfigPath());

        File key = new File(this.getKeyPath());
        Assertions.assertTrue(key.exists());
        Assertions.assertEquals(4, key.length());

        File config = new File(this.getConfigPath());
        Assertions.assertTrue(config.exists());
        Assertions.assertEquals(4, config.length());

        Application.main(null);

        key = new File(this.getKeyPath());
        Assertions.assertTrue(key.exists());
        Assertions.assertEquals(4, key.length());

        config = new File(this.getConfigPath());
        Assertions.assertTrue(config.exists());
        Assertions.assertEquals(4, config.length());
    }

    @Test
    public void test_main_internalAuth_overwrite_decline2() {
        this.writeStdIn(
                "\n\n\npass\nchangeit\n\n" + this.getKeyPath() + "\ny\n" + this.getConfigPath() + "\nwhat ever\n");

        IoTools.writeToFile("junk".getBytes(StandardCharsets.UTF_8), this.getKeyPath());
        IoTools.writeToFile("junk".getBytes(StandardCharsets.UTF_8), this.getConfigPath());

        File key = new File(this.getKeyPath());
        Assertions.assertTrue(key.exists());
        Assertions.assertEquals(4, key.length());

        File config = new File(this.getConfigPath());
        Assertions.assertTrue(config.exists());
        Assertions.assertEquals(4, config.length());

        Application.main(null);

        key = new File(this.getKeyPath());
        Assertions.assertTrue(key.exists());
        Assertions.assertEquals(4, key.length());

        config = new File(this.getConfigPath());
        Assertions.assertTrue(config.exists());
        Assertions.assertEquals(4, config.length());
    }

    @Test
    public void test_isWindowsFilePath() {
        boolean result = Application.isWindowsFilePath("");
        Assertions.assertFalse(result);

        result = Application.isWindowsFilePath(" \t \r \n \t ");
        Assertions.assertFalse(result);

        result = Application.isWindowsFilePath("C:/my/path");
        Assertions.assertFalse(result);

        result = Application.isWindowsFilePath("/my/path");
        Assertions.assertFalse(result);

        result = Application.isWindowsFilePath("C:\\my\\path");
        Assertions.assertTrue(result);

        result = Application.isWindowsFilePath("${user.home}\\my\\path");
        Assertions.assertTrue(result);

        result = Application.isWindowsFilePath("%env_var%\\my\\path");
        Assertions.assertTrue(result);
    }

    /*
     * Test contains known cases where the detection result is not correct. Proper
     * detection of these cases in all scenarios is hard :(
     */
    @Test
    public void test_isWindowsFilePath_falseNegatives() {
        // .\my\path is a valid Windows path but it's hard to detect accurately
        boolean result = Application.isWindowsFilePath(".\\my\\path");
        Assertions.assertFalse(result);

        // UNC paths are hard to detect
        result = Application.isWindowsFilePath("\\server\\my\\path");
        Assertions.assertFalse(result);

        // ~/my\:path/sub is a valid *nix path but it's hard to detect accurately.
        // Ideally this should return false
        result = Application.isWindowsFilePath("~/my\\:path/sub");
        Assertions.assertTrue(result);
    }

    @Test
    public void test_normalizeFilePath_windows() {
        this.setField("pathSeparator", "\\", Application.class);

        String result = Application.normalizeFilePath(null, false);
        Assertions.assertNull(result);

        result = Application.normalizeFilePath(null, true);
        Assertions.assertEquals("/", result);

        result = Application.normalizeFilePath("", false);
        Assertions.assertEquals("", result);

        result = Application.normalizeFilePath("", true);
        Assertions.assertEquals("/", result);

        result = Application.normalizeFilePath(" \t \r \n \t ", false);
        Assertions.assertEquals("", result);

        result = Application.normalizeFilePath("  C:\\My Path\\sub  ", false);
        Assertions.assertEquals("C:/My Path/sub", result);

        result = Application.normalizeFilePath("~/my path/sub ", false);
        Assertions.assertEquals("~/my path/sub", result);

        result = Application.normalizeFilePath("~/my\\ path/sub ", false);
        Assertions.assertEquals("~/my\\ path/sub", result);

        result = Application.normalizeFilePath("~/my\\ path/sub ", true);
        Assertions.assertEquals("~/my\\ path/sub/", result);
    }

    /*
     * Known scenarios where the path is altered but shouldn't be or vice versa
     */
    @Test
    public void test_normalizeFilePath_windows_knownErrorCases() {
        this.setField("pathSeparator", "\\", Application.class);

        /*
         * Should be ~/my\:path/sub
         */
        String result = Application.normalizeFilePath("~/my\\:path/sub ", false);
        Assertions.assertEquals("~/my/:path/sub", result);

        /*
         * Unclear - this might be / or \ depending on interpretation. Just \ is
         * technically not a valid Windows path which is why it currently does not get
         * modified
         */
        result = Application.normalizeFilePath("\\", false);
        Assertions.assertEquals("\\", result);

        /*
         * Unclear - this might be / or \/ depending on interpretation. Just \ is
         * technically not a valid Windows path which is why it currently does not get
         * modified to / which then means it's missing the trailing / so that gets
         * appended.
         */
        result = Application.normalizeFilePath("\\", true);
        Assertions.assertEquals("\\/", result);
    }

    @Test
    public void test_normalizeFilePath_nix() {
        this.setField("pathSeparator", "/", Application.class);

        String result = Application.normalizeFilePath(null, false);
        Assertions.assertNull(result);

        result = Application.normalizeFilePath(null, true);
        Assertions.assertEquals("/", result);

        result = Application.normalizeFilePath("", false);
        Assertions.assertEquals("", result);

        result = Application.normalizeFilePath("/", false);
        Assertions.assertEquals("/", result);

        result = Application.normalizeFilePath("", true);
        Assertions.assertEquals("/", result);

        result = Application.normalizeFilePath("/", true);
        Assertions.assertEquals("/", result);

        result = Application.normalizeFilePath(" \t \r \n \t ", false);
        Assertions.assertEquals("", result);

        result = Application.normalizeFilePath("  C:\\My Path\\sub  ", false);
        Assertions.assertEquals("C:\\My Path\\sub", result);

        result = Application.normalizeFilePath("~/my path/sub ", false);
        Assertions.assertEquals("~/my path/sub", result);

        result = Application.normalizeFilePath("~/my\\:path/sub ", false);
        Assertions.assertEquals("~/my\\:path/sub", result);

        result = Application.normalizeFilePath("~/my\\ path/sub ", false);
        Assertions.assertEquals("~/my\\ path/sub", result);

        result = Application.normalizeFilePath("~/my\\ path/sub ", true);
        Assertions.assertEquals("~/my\\ path/sub/", result);
    }
}

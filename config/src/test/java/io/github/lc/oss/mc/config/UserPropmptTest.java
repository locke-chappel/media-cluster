package io.github.lc.oss.mc.config;

import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class UserPropmptTest extends AbstractMockTest {
    private static class CallHelper {
        public boolean wasCalled = false;
    }

    private InputStream systemIn;

    @BeforeEach
    public void setup() {
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
    }

    protected void writeStdIn(String data) {
        System.setIn(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void test_read() {
        UserPropmpt up = new UserPropmpt() {
            @Override
            protected boolean isValid(String line) {
                return "value".equals(line);
            }
        };

        this.writeStdIn("\nother\nvalue");

        String result = up.read("prompt ");
        Assertions.assertEquals("value", result);
    }

    @Test
    public void test_read_console() {
        final Console console = Mockito.mock(Console.class);

        UserPropmpt up = new UserPropmpt() {
            @Override
            protected Console getConsole() {
                return console;
            }
        };

        Mockito.when(console.readLine()).thenReturn("\n\nvalue");

        String result = up.read("prompt ");
        Assertions.assertEquals("value", result);
    }

    @Test
    public void test_readPassword_console() {
        final Console console = Mockito.mock(Console.class);

        UserPropmpt up = new UserPropmpt() {
            @Override
            protected Console getConsole() {
                return console;
            }

            @Override
            protected String readLine() {
                return this.readLine(true);
            }
        };

        Mockito.when(console.readPassword()).thenReturn("\n\npass".toCharArray());

        String result = up.read("prompt ");
        Assertions.assertEquals("pass", result);
    }

    @Test
    public void test_read_default() {
        UserPropmpt up = new UserPropmpt();

        this.writeStdIn(" ");

        String result = up.read("prompt ", "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void test_read_default_null() {
        UserPropmpt up = new UserPropmpt();

        System.setIn(new ByteArrayInputStream(new byte[] { 0x00 }));

        String result = up.read("prompt ", "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void test_read_default_confirm_y() {
        final CallHelper helper1 = new CallHelper();
        final CallHelper helper2 = new CallHelper();

        UserPropmpt up = new UserPropmpt() {
            @Override
            protected boolean mustConfirm(String line) {
                Assertions.assertFalse(helper1.wasCalled);
                helper1.wasCalled = true;
                return true;
            }

            @Override
            protected void onConfrimDecline() {
                Assertions.assertFalse(helper2.wasCalled);
                helper2.wasCalled = true;
                super.onConfrimDecline();
            }
        };

        this.writeStdIn(" \ny");

        Assertions.assertFalse(helper1.wasCalled);
        Assertions.assertFalse(helper2.wasCalled);

        String result = up.read("prompt ", "default");
        Assertions.assertEquals("default", result);
        Assertions.assertTrue(helper1.wasCalled);
        Assertions.assertFalse(helper2.wasCalled);
    }

    @Test
    public void test_read_default_confirm_yes() {
        final CallHelper helper1 = new CallHelper();
        final CallHelper helper2 = new CallHelper();

        UserPropmpt up = new UserPropmpt() {
            @Override
            protected boolean mustConfirm(String line) {
                Assertions.assertFalse(helper1.wasCalled);
                helper1.wasCalled = true;
                return true;
            }

            @Override
            protected void onConfrimDecline() {
                Assertions.assertFalse(helper2.wasCalled);
                helper2.wasCalled = true;
                super.onConfrimDecline();
            }
        };

        this.writeStdIn(" \nyes");

        Assertions.assertFalse(helper1.wasCalled);
        Assertions.assertFalse(helper2.wasCalled);

        String result = up.read("prompt ", "default");
        Assertions.assertEquals("default", result);
        Assertions.assertTrue(helper1.wasCalled);
        Assertions.assertFalse(helper2.wasCalled);
    }

    @Test
    public void test_read_default_confirm_notYOrYes() {
        final CallHelper helper1 = new CallHelper();
        final CallHelper helper2 = new CallHelper();

        UserPropmpt up = new UserPropmpt() {
            @Override
            protected boolean mustConfirm(String line) {
                Assertions.assertFalse(helper1.wasCalled);
                helper1.wasCalled = true;
                return true;
            }

            @Override
            protected void onConfrimDecline() {
                Assertions.assertFalse(helper2.wasCalled);
                helper2.wasCalled = true;
                super.onConfrimDecline();
            }
        };

        this.writeStdIn(" \nnot y or yes");

        Assertions.assertFalse(helper1.wasCalled);
        Assertions.assertFalse(helper2.wasCalled);

        String result = up.read("prompt ", "default");
        /*
         * Note result is still the last value entered, it's expected the
         * onConfirmDecline method will act accordingly and use the result value
         * appropriately
         */
        Assertions.assertEquals("default", result);
        Assertions.assertTrue(helper1.wasCalled);
        Assertions.assertTrue(helper2.wasCalled);
    }

    @Test
    public void test_read_confirm_y() {
        final CallHelper helper1 = new CallHelper();
        final CallHelper helper2 = new CallHelper();

        UserPropmpt up = new UserPropmpt() {
            @Override
            protected boolean mustConfirm(String line) {
                Assertions.assertFalse(helper1.wasCalled);
                helper1.wasCalled = true;
                return true;
            }

            @Override
            protected void onConfrimDecline() {
                Assertions.assertFalse(helper2.wasCalled);
                helper2.wasCalled = true;
                super.onConfrimDecline();
            }
        };

        this.writeStdIn("value\ny");

        Assertions.assertFalse(helper1.wasCalled);
        Assertions.assertFalse(helper2.wasCalled);

        String result = up.read("prompt ");
        Assertions.assertEquals("value", result);
        Assertions.assertTrue(helper1.wasCalled);
        Assertions.assertFalse(helper2.wasCalled);
    }

    @Test
    public void test_read_confirm_notYOrYes() {
        final CallHelper helper1 = new CallHelper();
        final CallHelper helper2 = new CallHelper();

        UserPropmpt up = new UserPropmpt() {
            @Override
            protected boolean mustConfirm(String line) {
                Assertions.assertFalse(helper1.wasCalled);
                helper1.wasCalled = true;
                return true;
            }

            @Override
            protected void onConfrimDecline() {
                Assertions.assertFalse(helper2.wasCalled);
                helper2.wasCalled = true;
                super.onConfrimDecline();
            }
        };

        this.writeStdIn("value\nnot y or yes");

        Assertions.assertFalse(helper1.wasCalled);
        Assertions.assertFalse(helper2.wasCalled);

        String result = up.read("prompt ");
        /*
         * Note result is still the last value entered, it's expected the
         * onConfirmDecline method will act accordingly and use the result value
         * appropriately
         */
        Assertions.assertEquals("value", result);
        Assertions.assertTrue(helper1.wasCalled);
        Assertions.assertTrue(helper2.wasCalled);
    }

    @Test
    public void test_readList_single() {
        UserPropmpt up = new UserPropmpt();

        this.writeStdIn("\n\nvalue");

        List<String> result = up.readList("prompt ");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("value", result.get(0));
    }

    @Test
    public void test_readList_multi() {
        UserPropmpt up = new UserPropmpt();

        this.writeStdIn("\n\nvalue,,value2,,a\n");

        List<String> result = up.readList("prompt ");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("value", result.get(0));
        Assertions.assertEquals("value2", result.get(1));
        Assertions.assertEquals("a", result.get(2));
    }

    @Test
    public void test_readList_default() {
        UserPropmpt up = new UserPropmpt();

        this.writeStdIn(" ");

        List<String> result = up.readList("prompt ", "default");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("default", result.get(0));
    }

    @Test
    public void test_readList_defaultMulti() {
        UserPropmpt up = new UserPropmpt();

        this.writeStdIn(" ");

        List<String> result = up.readList("prompt ", "two,default");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("two", result.get(0));
        Assertions.assertEquals("default", result.get(1));
    }

    @Test
    public void test_isValid() {
        UserPropmpt up = new UserPropmpt();

        boolean result = up.isValid((String) null);
        Assertions.assertFalse(result);

        result = up.isValid((List<String>) null);
        Assertions.assertFalse(result);

        result = up.isValid("");
        Assertions.assertFalse(result);

        result = up.isValid(new ArrayList<>());
        Assertions.assertFalse(result);

        result = up.isValid("a");
        Assertions.assertTrue(result);

        result = up.isValid(" ");
        Assertions.assertTrue(result);

        result = up.isValid(Arrays.asList(""));
        Assertions.assertTrue(result);

        result = up.isValid(Arrays.asList(" "));
        Assertions.assertTrue(result);

        result = up.isValid(Arrays.asList("a"));
        Assertions.assertTrue(result);

        result = up.isValid(Arrays.asList("a", "b"));
        Assertions.assertTrue(result);
    }

    @Test
    public void test_trim() {
        UserPropmpt up = new UserPropmpt();

        String result = up.trim(null);
        Assertions.assertEquals("", result);

        result = up.trim(" ");
        Assertions.assertEquals("", result);

        result = up.trim(" \t \r \n \t ");
        Assertions.assertEquals("", result);

        result = up.trim("a");
        Assertions.assertEquals("a", result);

        result = up.trim(" a");
        Assertions.assertEquals("a", result);

        result = up.trim("a ");
        Assertions.assertEquals("a", result);

        result = up.trim(" a ");
        Assertions.assertEquals("a", result);
    }

    /*
     * Test coverage filler, the actual method has no logic but help detect changes
     * we need to invoke it anyway
     */
    @Test
    public void test_codeFiller() {
        UserPropmpt up = new UserPropmpt();

        up.onConfrimDecline();
    }
}

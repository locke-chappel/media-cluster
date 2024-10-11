package io.github.lc.oss.mc.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.mc.AbstractMockTest;
import io.github.lc.oss.mc.entity.Constants;

public class PathNormalizerTest extends AbstractMockTest {
    private static class NixNormalizer extends PathNormalizer {
        @Override
        String getFileSeparator() {
            return "/";
        }
    }

    private static class WinNormalizer extends PathNormalizer {
        @Override
        String getFileSeparator() {
            return "\\";
        }
    }

    @Test
    public void test_defaultFileSeparator() {
        PathNormalizer pn = new PathNormalizer();

        Assertions.assertSame(Constants.FILE_SEPARATOR, pn.getFileSeparator());
    }

    @Test
    public void test_dirOsAware_blanks() {
        PathNormalizer pn = new PathNormalizer();

        String result = pn.dirOsAware(null);
        Assertions.assertEquals("", result);

        result = pn.dirOsAware("");
        Assertions.assertEquals("", result);

        result = pn.dirOsAware(" \t \r \n \t ");
        Assertions.assertEquals("", result);
    }

    @Test
    public void test_dirOsAware_noTrailing_nix() {
        PathNormalizer pn = new NixNormalizer();

        String result = pn.dirOsAware("a");
        Assertions.assertEquals("a/", result);

        result = pn.dirOsAware("a/b");
        Assertions.assertEquals("a/b/", result);

        result = pn.dirOsAware("/a");
        Assertions.assertEquals("/a/", result);

        result = pn.dirOsAware("/a/b");
        Assertions.assertEquals("/a/b/", result);

        result = pn.dirOsAware("a\\");
        Assertions.assertEquals("a/", result);

        result = pn.dirOsAware("\\a");
        Assertions.assertEquals("/a/", result);

        result = pn.dirOsAware("\\a\\b");
        Assertions.assertEquals("/a/b/", result);
    }

    @Test
    public void test_dirOsAware_noTrailing_win() {
        PathNormalizer pn = new WinNormalizer();

        String result = pn.dirOsAware("a");
        Assertions.assertEquals("a\\", result);

        result = pn.dirOsAware("a/b");
        Assertions.assertEquals("a\\b\\", result);

        result = pn.dirOsAware("/a");
        Assertions.assertEquals("\\a\\", result);

        result = pn.dirOsAware("/a/b");
        Assertions.assertEquals("\\a\\b\\", result);

        result = pn.dirOsAware("a\\");
        Assertions.assertEquals("a\\", result);

        result = pn.dirOsAware("\\a");
        Assertions.assertEquals("\\a\\", result);

        result = pn.dirOsAware("\\a\\b");
        Assertions.assertEquals("\\a\\b\\", result);
    }
}

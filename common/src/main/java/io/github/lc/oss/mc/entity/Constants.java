package io.github.lc.oss.mc.entity;

import io.github.lc.oss.commons.util.Constants.FileSizes;

public final class Constants {
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    public static final class Lengths {
        public static final int CLI = 2048;
        public static final int ENUM = 32;
        public static final int FILE_EXT = 16;
        public static final int FILE_PATH = 256;
        public static final int ID = 36;
        public static final int JSON = 256 * FileSizes.KB;
        public static final int NAME = 128;
        public static final int PUBLIC_KEY = 256;
        public static final int URL = 512;

        private Lengths() {
        }

        public static final class Search {
            public static final int MIN_PAGE_SIZE = 10;
            public static final int MAX_PAGE_SIZE = 100;
            public static final int MAX_TERM_LENGTH = 128;

            private Search() {
            }
        }
    }

    private Constants() {
    }
}

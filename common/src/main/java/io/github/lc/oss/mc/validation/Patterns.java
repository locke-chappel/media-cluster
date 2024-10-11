package io.github.lc.oss.mc.validation;

import java.util.regex.Pattern;

import io.github.lc.oss.mc.entity.Constants;

public class Patterns {
    public static final Pattern DisplayName = Pattern
            .compile("^[a-zA-Z0-9+=.,&~|_!@#$<>(){}\\-\\[\\] ]{1," + Constants.Lengths.NAME + "}$");
    public static final Pattern ExternalId = Pattern.compile("^[a-zA-Z0-9._\\-]{1," + Constants.Lengths.ID + "}$");
    public static final Pattern FilePath = Pattern.compile("^(?!.*\\.\\.).{1," + Constants.Lengths.FILE_PATH + "}$");
    public static final Pattern FileExt = Pattern.compile("^[a-zA-Z0-9\\-_]{1,16}$");
    public static final Pattern Name = Pattern
            .compile("^(?!#)[a-zA-Z0-9\\-_\\.#@=+!~ ()\\[\\]{}:;/\\\\%&|\"']{1," + Constants.Lengths.NAME + "}$");
    public static final Pattern SearchTerm = Pattern.compile("^.{0," + Constants.Lengths.Search.MAX_TERM_LENGTH + "}$");
    public static final Pattern Username = Pattern
            .compile("^[a-zA-Z0-9+=.,&~|_!@#$<>(){}\\-\\[\\]]{1," + Constants.Lengths.NAME + "}$");
    public static final Pattern Url = Pattern
            .compile("^https?:\\/\\/[a-zA-Z0-9\\-\\._~:/?#\\[\\]@!$&'()*+;%=]{1," + (Constants.Lengths.URL - 8) + "}$");

    private Patterns() {
    }
}

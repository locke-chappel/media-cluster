package io.github.lc.oss.mc.security;

public final class Authorities extends io.github.lc.oss.commons.web.config.Authorities {
    public static final String USER_PERMISSION = "USER";

    public static final String ANY_APP_USER = "hasAnyAuthority('" + //
            Authorities.USER_PERMISSION + "')";

    public static final String USER = "hasAuthority('" + Authorities.USER_PERMISSION + "')";

    private Authorities() {
    }
}

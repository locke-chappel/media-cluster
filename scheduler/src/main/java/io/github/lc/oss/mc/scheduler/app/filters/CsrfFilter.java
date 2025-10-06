package io.github.lc.oss.mc.scheduler.app.filters;

import org.apache.commons.lang3.Strings;

import jakarta.servlet.http.HttpServletRequest;

public class CsrfFilter extends io.github.lc.oss.commons.web.filters.CsrfFilter {
    @Override
    protected boolean requiresToken(HttpServletRequest request) {
        if (this.isAppApiUrl(request.getRequestURI())) {
            return false;
        }
        return super.requiresToken(request);
    }

    protected boolean isAppApiUrl(String url) {
        return Strings.CI.equalsAny(url, "/api/v1/jobs/complete");
    }
}

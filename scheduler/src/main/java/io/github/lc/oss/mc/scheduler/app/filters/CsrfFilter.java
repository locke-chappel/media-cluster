package io.github.lc.oss.mc.scheduler.app.filters;

import org.apache.commons.lang3.StringUtils;

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
        return StringUtils.equalsAnyIgnoreCase(url, "/api/v1/jobs/complete");
    }
}

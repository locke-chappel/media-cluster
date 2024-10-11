package io.github.lc.oss.mc.scheduler.app.service;

import java.io.IOException;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;

import io.github.lc.oss.commons.web.services.CustomResponseErrorHandler;

/**
 * Custom HTTP response handler. 4xx errors should be handled by the
 * application. Only 5xx are exceptions.
 */
@Component
public class RestResponseErrorHandler extends DefaultResponseErrorHandler implements CustomResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        if (response.getStatusCode().is5xxServerError()) {
            return true;
        }
        return false;
    }
}

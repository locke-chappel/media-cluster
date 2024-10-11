package io.github.lc.oss.mc.worker.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import io.github.lc.oss.commons.web.services.AbstractETagService;

@Service
public class ETagService extends AbstractETagService {
    @Autowired
    private BuildProperties buildProperties;

    @Override
    protected String getAppVersion() {
        return this.buildProperties.getVersion();
    }
}

package io.github.lc.oss.mc.scheduler.app.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lc.oss.commons.web.tokens.StatelessCsrfTokenManager;

@DisallowConcurrentExecution
public class CsrfTokenSaltJob implements Job {
    @Autowired
    private StatelessCsrfTokenManager csrfTokenManager;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        this.csrfTokenManager.newSalt();
    }
}

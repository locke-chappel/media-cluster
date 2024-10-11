package io.github.lc.oss.mc.scheduler.app.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lc.oss.mc.scheduler.security.JwtManager;

@DisallowConcurrentExecution
public class TokenCleanupJob implements Job {
    @Autowired
    private JwtManager jwtManager;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        this.jwtManager.cleanCache();
    }
}

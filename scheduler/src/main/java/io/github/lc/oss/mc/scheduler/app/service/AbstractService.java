package io.github.lc.oss.mc.scheduler.app.service;

import org.springframework.transaction.interceptor.TransactionAspectSupport;

public abstract class AbstractService extends io.github.lc.oss.mc.service.AbstractService {
    protected void rollback() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
}

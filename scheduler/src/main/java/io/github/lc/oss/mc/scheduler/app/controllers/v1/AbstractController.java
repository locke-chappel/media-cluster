package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import org.springframework.transaction.interceptor.TransactionAspectSupport;

public abstract class AbstractController extends io.github.lc.oss.commons.web.controllers.AbstractController {
    protected void rollback() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
}

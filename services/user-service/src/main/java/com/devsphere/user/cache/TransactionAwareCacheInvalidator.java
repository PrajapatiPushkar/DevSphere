package com.devsphere.user.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TransactionAwareCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(TransactionAwareCacheInvalidator.class);

    private TransactionAwareCacheInvalidator() {
    }

    public static void executeAfterCommit(Runnable action) {
        if (action == null) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (Exception e) {
                        log.warn("Error executing post-commit cache invalidation action: {}", e.getMessage(), e);
                    }
                }
            });
        } else {
            try {
                action.run();
            } catch (Exception e) {
                log.warn("Error executing non-transactional cache invalidation action: {}", e.getMessage(), e);
            }
        }
    }
}

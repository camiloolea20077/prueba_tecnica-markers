package com.markers.data_credits.infrastructure.adapter.out.cache;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ejecuta una acción cuando la transacción actual confirma (o de inmediato si no hay transacción).
 * <p>
 * Se usa para invalidar la caché: si se invalidara antes del commit, una lectura concurrente
 * podría volver a cachear el valor viejo; y si la transacción hace rollback no hay nada que invalidar.
 * </p>
 */
final class AfterCommit {

    private AfterCommit() {
    }

    static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}

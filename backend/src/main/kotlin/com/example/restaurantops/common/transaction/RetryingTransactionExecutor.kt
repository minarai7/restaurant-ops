package com.example.restaurantops.common.transaction

import com.example.restaurantops.common.error.RetryExhaustedException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.sql.SQLException
import java.util.concurrent.ThreadLocalRandom

/**
 * Executes a unit of work inside a fresh transaction and retries the whole
 * transaction when PostgreSQL reports a transient serialization failure
 * (SQLSTATE 40001) or a detected deadlock (SQLSTATE 40P01).
 *
 * Retrying happens at the transaction boundary — not by catching inside an
 * already-aborted transaction — because once PostgreSQL aborts a transaction
 * every further statement on that connection fails until it is rolled back.
 * [PROPAGATION_REQUIRES_NEW][TransactionDefinition.PROPAGATION_REQUIRES_NEW]
 * guarantees each attempt is an entirely new transaction.
 *
 * The [action] must be free of external side effects (HTTP calls, message
 * publishes, etc.): it may run up to [MAX_ATTEMPTS] times.
 */
@Component
class RetryingTransactionExecutor(
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun <T> execute(action: () -> T): T {
        var attempt = 0
        while (true) {
            attempt++
            try {
                @Suppress("UNCHECKED_CAST")
                return transactionTemplate.execute { action() } as T
            } catch (exception: RuntimeException) {
                val sqlState = retryableSqlState(exception) ?: throw exception

                if (attempt >= MAX_ATTEMPTS) {
                    logger.warn(
                        "Transaction failed after {} attempts (SQLSTATE {})",
                        attempt,
                        sqlState,
                    )
                    throw RetryExhaustedException()
                }

                logger.warn(
                    "Retryable DB error SQLSTATE {} on attempt {}/{}; retrying",
                    sqlState,
                    attempt,
                    MAX_ATTEMPTS,
                )
                backoff(attempt)
            }
        }
    }

    /**
     * Returns the retryable SQLSTATE that caused [throwable] (walking the cause
     * chain), or null when the failure is not one we should retry.
     *
     * TODO(human): implement the detection. Spring wraps the driver error, so
     * the raw java.sql.SQLException carrying the SQLSTATE is somewhere down the
     * cause chain. Decide how to locate it and which states count as retryable
     * (see RETRYABLE_SQL_STATES).
     */
    private fun retryableSqlState(throwable: Throwable): String? {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is SQLException) {
                val sqlState = current.sqlState
                if (sqlState != null && sqlState in RETRYABLE_SQL_STATES) {
                    return sqlState
                }
            }
            current = current.cause
        }
        return null
    }

    private fun backoff(attempt: Int) {
        val jitter = ThreadLocalRandom.current().nextLong(BACKOFF_JITTER_MILLIS)
        Thread.sleep(BASE_BACKOFF_MILLIS * attempt + jitter)
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val BASE_BACKOFF_MILLIS = 20L
        private const val BACKOFF_JITTER_MILLIS = 20L
        private val RETRYABLE_SQL_STATES = setOf("40001", "40P01")
    }
}

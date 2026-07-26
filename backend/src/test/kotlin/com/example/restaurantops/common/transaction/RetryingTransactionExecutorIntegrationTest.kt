package com.example.restaurantops.common.transaction

import com.example.restaurantops.common.error.RetryExhaustedException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.SQLException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RetryingTransactionExecutorIntegrationTest @Autowired constructor(
    private val retryingTransactionExecutor: RetryingTransactionExecutor,
    private val jdbcClient: JdbcClient,
) {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17")
    }

    @Test
    fun `reproduces a deadlock and recovers by retrying in a new transaction`() {
        val id1 = createRow()
        val id2 = createRow()

        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val firstLocks = CountDownLatch(2)
        val attempts = AtomicInteger(0)

        try {
            // Two workers lock the same rows in opposite order. Each grabs its
            // first row, signals via firstLocks, then waits until both hold a
            // lock before reaching for the second row — which forces the cycle.
            val worker = { firstRow: UUID, secondRow: UUID ->
                executor.submit<Boolean> {
                    ready.countDown()
                    start.await()
                    retryingTransactionExecutor.execute {
                        attempts.incrementAndGet()
                        lockRow(firstRow)
                        firstLocks.countDown()
                        check(firstLocks.await(10, TimeUnit.SECONDS)) {
                            "Timed out waiting for both first-row locks"
                        }
                        lockRow(secondRow)
                    }
                    true
                }
            }

            val futureA = worker(id1, id2)
            val futureB = worker(id2, id1)

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
            start.countDown()

            // Both units of work complete: PostgreSQL aborts one side of the
            // deadlock, and the executor re-runs it in a fresh transaction.
            assertThat(futureA.get(30, TimeUnit.SECONDS)).isTrue()
            assertThat(futureB.get(30, TimeUnit.SECONDS)).isTrue()

            // 2 initial runs + at least 1 retry proves the wrapper re-ran the
            // aborted transaction rather than propagating the deadlock.
            assertThat(attempts.get()).isGreaterThanOrEqualTo(3)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `exhausts retries and raises a controlled conflict error`() {
        val attempts = AtomicInteger(0)

        assertThatThrownBy {
            retryingTransactionExecutor.execute<Unit> {
                attempts.incrementAndGet()
                // A retryable SQLSTATE nested in the cause chain, exactly as a
                // driver deadlock error surfaces — but it never clears, so all
                // attempts are used up.
                throw RuntimeException(
                    "simulated failure",
                    SQLException("deadlock detected", "40P01"),
                )
            }
        }.isInstanceOf(RetryExhaustedException::class.java)

        assertThat(attempts.get()).isEqualTo(3)
    }

    @Test
    fun `does not retry a non-retryable failure`() {
        val attempts = AtomicInteger(0)

        assertThatThrownBy {
            retryingTransactionExecutor.execute<Unit> {
                attempts.incrementAndGet()
                throw IllegalStateException("business rule violation")
            }
        }.isInstanceOf(IllegalStateException::class.java)

        assertThat(attempts.get()).isEqualTo(1)
    }

    private fun createRow(): UUID {
        val id = UUID.randomUUID()
        jdbcClient.sql(
            """
            INSERT INTO stores (id, name, created_at)
            VALUES (:id, :name, CURRENT_TIMESTAMP)
            """.trimIndent(),
        )
            .param("id", id)
            .param("name", "Row-$id")
            .update()
        return id
    }

    private fun lockRow(id: UUID) {
        jdbcClient.sql("SELECT id FROM stores WHERE id = :id FOR UPDATE")
            .param("id", id)
            .query(UUID::class.java)
            .single()
    }
}

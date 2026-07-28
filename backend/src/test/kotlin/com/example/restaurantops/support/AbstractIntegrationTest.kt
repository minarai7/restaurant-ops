package com.example.restaurantops.support

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.simple.JdbcClient
import java.util.UUID

/**
 * Base class for all integration tests. Boots the full application context once,
 * backed by the shared [TestcontainersConfiguration] PostgreSQL container, and
 * resets the database before every test method so each test starts from a known,
 * empty state.
 *
 * Note there is deliberately no `@DirtiesContext`: keeping the context intact is
 * what lets Spring's context cache reuse the same context (and container) across
 * every subclass instead of rebuilding it per class.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var jdbcClient: JdbcClient

    @BeforeEach
    fun resetDatabase() {
        val tables = jdbcClient
            .sql(
                """
                    SELECT format('%I.%I', schemaname, tablename)
                    FROM pg_tables
                    WHERE schemaname = :schema
                    AND tablename <> :excludedTable
                """.trimIndent()
            )
            .param("schema", "public")
            .param("excludedTable", "flyway_schema_history")
            .query(String::class.java)
            .list()
        
        if (tables.isEmpty()) {
            return
        }
        
        val truncateSql = """
            TRUNCATE TABLE ${tables.joinToString(", ")}
            RESTART IDENTITY CASCADE
        """.trimIndent()
        
        jdbcClient.sql(truncateSql).update()
    }

    /**
     * Publishing is implemented in Task 6.3. Until then, tests that need an
     * orderable menu item promote its draft directly via SQL.
     */
    protected fun publishDraftRevision(menuItemId: UUID) {
        jdbcClient.sql(
            """
            UPDATE menu_item_revisions
            SET
                status = 'PUBLISHED',
                published_at = CURRENT_TIMESTAMP
            WHERE menu_item_id = :menuItemId
              AND status = 'DRAFT'
            """.trimIndent(),
        )
            .param("menuItemId", menuItemId)
            .update()
    }
}

package com.example.restaurantops.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Declares the PostgreSQL Testcontainer as a Spring [Bean] rather than a JUnit
 * `@Container` field. Because the application context is cached and shared across
 * every test that imports this configuration, the container is created exactly
 * once for the whole suite (and Flyway migrates once). [ServiceConnection] wires
 * the datasource URL/credentials from the running container automatically.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer =
        PostgreSQLContainer("postgres:17")
}

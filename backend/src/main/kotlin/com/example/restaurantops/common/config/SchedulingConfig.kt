package com.example.restaurantops.common.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Gated behind a property (default on) so integration tests can disable
 * background ticks and drive the worker deterministically instead.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = ["restaurant-ops.scheduling.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class SchedulingConfig
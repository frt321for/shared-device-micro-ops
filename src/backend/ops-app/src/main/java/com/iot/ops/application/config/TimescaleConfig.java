package com.iot.ops.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class TimescaleConfig {
    // TimescaleDB specific configuration for production
    // Currently uses default PostgreSQL auto-configuration
    // which works with TimescaleDB since it's a PostgreSQL extension
}
